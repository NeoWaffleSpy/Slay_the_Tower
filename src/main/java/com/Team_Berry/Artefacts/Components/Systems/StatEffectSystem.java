package com.Team_Berry.Artefacts.Components.Systems;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Enums.TargetType;
import com.Team_Berry.Artefacts.Codecs.Enums.TriggerType;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.Artefacts.UI.ArtefactHud;
import com.Team_Berry.Artefacts.UI.ShieldHud;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class StatEffectSystem {
    private static final KeyedScheduler scheduler = new KeyedScheduler();

    public static void register() {
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectTickingSystem(EntityStatsModule.get().getEntityStatMapComponentType()));
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectDamageSystem());
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectRefChangeSystem());
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectPreDamageSystem());
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectDeathSystem());


    }

    public static void stop() {
        scheduler.stop();
    }

    private static void clearTempStats(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;
        Store<EntityStore> store = ref.getStore();
        EntityStatMap targetEntityStatMap = store.getComponent(ref, EntityStatsModule.get().getEntityStatMapComponentType());
        if (targetEntityStatMap == null) return;
        ArtefactCodec.getAssetMap().getAssetMap().forEach((assetName, artefact) -> {
            ArrayList<StatCodec> stats = artefact.getStatArray();
            if (stats != null) {
                stats.forEach(stat -> {
                    if (stat.trigger != TriggerType.PASSIVE) {
                        String key = ArtefactPlugin.get().getName() + "-" + artefact.getId() + "-" + stat.hashCode();
                        targetEntityStatMap.removeModifier(getEntityIndex(stat.getType()), key);
                    }
                });
            }
        });
    }

    private static void applyTempStats(StatEffectComponent originEntityComponent, EntityStatMap originEntityStatMap, EntityStatMap targetEntityStatMap, TriggerType type) {
        if (originEntityComponent == null || targetEntityStatMap == null)
            return;
        originEntityComponent.artefactList.forEach(((artefact, count) -> {
            artefact.getStatArray().forEach(stat -> {
                if (stat == null || stat.getType() == null) return;
                if (stat.trigger != type)
                    return;
                if (Math.random() >= stat.probability)
                    return;
                EntityStatMap targetMap;
                if (stat.target == TargetType.ENEMY)
                    targetMap = targetEntityStatMap;
                else if (stat.target == TargetType.SELF)
                    targetMap = originEntityStatMap;
                else
                    return;
                String key = ArtefactPlugin.get().getName() + "-" + artefact.getId() + "-" + stat.hashCode();
                targetMap.putModifier(getEntityIndex(stat.getType()), key, new StaticModifier(Modifier.ModifierTarget.MAX, stat.calc, stat.value * count));
                scheduler.schedule(key, () -> targetMap.removeModifier(getEntityIndex(stat.getType()), key), (long) (stat.duration * 1000), TimeUnit.MILLISECONDS);
            });
        }));
    }

    private static int getEntityIndex(EntityStatType statType) {
        return ArtefactPlugin.getEntityStatTypeAssetStore().getIndex(statType.getId());
    }

    public static class StatEffectDamageSystem extends DamageEventSystem {
        public StatEffectDamageSystem() {
        }
        @Override
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getFilterDamageGroup();
        }

        @Nonnull
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }

        private EntityStatType getStat(String name) {
            return StatCodec.getStatFromString(name);
        }

        @Override
        public void handle(int i, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull Damage damage) {
            if (damage.getAmount() <= 0.0F)
                return;
            Damage.Source source = damage.getSource();
            if (source instanceof Damage.EntitySource entitySource) {
                float damageValue = damage.getAmount();
                Ref<EntityStore> attackerRef = entitySource.getRef();
                Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(i);
                if (attackerRef == targetRef)
                    return;
                EntityStatMap statMapAttack = commandBuffer.getComponent(attackerRef, EntityStatMap.getComponentType());
                EntityStatMap statMapTarget = commandBuffer.getComponent(targetRef, EntityStatMap.getComponentType());

                damageValue = applyBonusAttack(damageValue, statMapAttack);
                damageValue = applyCritical(damageValue, statMapAttack);
                damageValue = applyArmor(damageValue, statMapTarget);
                damage.setAmount(damageValue);

                handleBleedArtefact(attackerRef, targetRef, commandBuffer);
                StatEffectSystem.applyTempStats(commandBuffer.getComponent(attackerRef, StatEffectComponent.getComponentType()), statMapAttack, statMapTarget, TriggerType.ON_HIT);
                int damageIndex = DamageCause.getAssetMap().getIndex("Skill");
                if (damageIndex == damage.getDamageCauseIndex())
                    StatEffectSystem.applyTempStats(commandBuffer.getComponent(attackerRef, StatEffectComponent.getComponentType()), statMapAttack, statMapTarget, TriggerType.ON_SKILL_USE);
            }
        }

        private void handleBleedArtefact(Ref<EntityStore> attackerRef, Ref<EntityStore> targetRef, CommandBuffer<EntityStore> commandBuffer) {
            StatEffectComponent statComp = commandBuffer.getComponent(attackerRef, StatEffectComponent.getComponentType());
            if (statComp == null) return;

            ArtefactCodec bleedArtefact = ArtefactCodec.getAssetMap().getAsset("Bleed_Artefact");
            if (bleedArtefact == null) return;

            int stacks = statComp.getAmount(bleedArtefact);
            if (stacks <= 0) return;

            double chance = stacks * 0.10;

            if (Math.random() < chance) {
                EffectControllerComponent effectController = commandBuffer.getComponent(targetRef, EffectControllerComponent.getComponentType());
                EntityEffect bleedEffect = EntityEffect.getAssetMap().getAsset("Artefact_Bleed_Damage_Flat");

                if (effectController != null && bleedEffect != null) {
                    effectController.addEffect(targetRef, bleedEffect, commandBuffer);
                }
            }
        }

        public float applyBonusAttack(float damageValue, EntityStatMap entityStatMap) {
            EntityStatType bonusDamageType = getStat("Bonus_Damage");
            if (bonusDamageType != null && entityStatMap != null) {
                float mult = 1;
                Map<String, Modifier> modMap = entityStatMap.get(getEntityIndex(bonusDamageType)).getModifiers();
                if (modMap != null) {
                    for (Modifier mod : modMap.values()) {
                        StaticModifier modifier = (StaticModifier) mod;
                        if (modifier.getCalculationType() == StaticModifier.CalculationType.ADDITIVE)
                            damageValue += modifier.getAmount();
                        else if (modifier.getCalculationType() == StaticModifier.CalculationType.MULTIPLICATIVE)
                            mult += modifier.getAmount();
                    }
                    return damageValue * mult;
                }
            }
            return damageValue;
        }

        public float applyCritical(float damageValue, EntityStatMap entityStatMap) {
            EntityStatType bonusCritChanceType = getStat("Crit_Rate");
            if (bonusCritChanceType != null && entityStatMap != null) {
                float odd = 0;
                Map<String, Modifier> modMap = entityStatMap.get(getEntityIndex(bonusCritChanceType)).getModifiers();
                if (modMap != null) {
                    for (Modifier mod : modMap.values()) {
                        StaticModifier modifier = (StaticModifier) mod;
                        odd += modifier.getAmount();
                    }
                }
                if (Math.random() < (odd)) {
                    float mult = 2;
                    EntityStatType bonusCritDamageType = getStat("Crit_Damage");
                    if (bonusCritDamageType != null) {
                        Map<String, Modifier> modCritMap = entityStatMap.get(getEntityIndex(bonusCritDamageType)).getModifiers();
                        if (modCritMap != null) {
                            for (Modifier mod : modCritMap.values()) {
                                StaticModifier modifier = (StaticModifier) mod;
                                mult += modifier.getAmount();
                            }
                        }
                    }
                    return damageValue * mult;
                }
            }
            return damageValue;
        }

        public float applyArmor(float damageValue, EntityStatMap entityStatMap) {
            EntityStatType bonusArmorType = getStat("Bonus_Armor");
            if (bonusArmorType != null && entityStatMap != null) {
                float div = 1;
                Map<String, Modifier> modMap = entityStatMap.get(getEntityIndex(bonusArmorType)).getModifiers();
                if (modMap != null) {
                    for (Modifier mod : modMap.values()) {
                        StaticModifier modifier = (StaticModifier) mod;
                        if (modifier.getCalculationType() == StaticModifier.CalculationType.ADDITIVE)
                            damageValue -= modifier.getAmount();
                        else if (modifier.getCalculationType() == StaticModifier.CalculationType.MULTIPLICATIVE)
                            div *= 1 - modifier.getAmount();
                    }
                    return damageValue * div;
                }
            }
            return damageValue;
        }
    }

    public static class StatEffectTickingSystem extends EntityTickingSystem<EntityStore> {
        @Nonnull
        private final ComponentType<EntityStore, EntityStatMap> entityStatMapComponentType;
        @Nonnull
        private final Query<EntityStore> query;

        public StatEffectTickingSystem(@Nonnull ComponentType<EntityStore, EntityStatMap> entityStatMapComponentType) {
            this.entityStatMapComponentType = entityStatMapComponentType;
            this.query = Query.and(ArtefactPlugin.get().getStatEffectComponentType());
        }

        public void tick(float v, int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
            StatEffectComponent comp = (StatEffectComponent) archetypeChunk.getComponent(i, ArtefactPlugin.get().getStatEffectComponentType());

            if (comp == null) return;


            long now = store.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();
            checkShieldCooldown(i, archetypeChunk, comp, now, commandBuffer);


            if (comp.artefactUpdated.isEmpty()) return;

            EntityStatMap statMap = (EntityStatMap) archetypeChunk.getComponent(i, EntityStatsModule.get().getEntityStatMapComponentType());
            if (statMap == null) return;

            comp.artefactUpdated.forEach(artefact -> {
                ArrayList<StatCodec> list = artefact.getStatArray();
                for (int j = 0; j < list.size(); j++) {
                    StatCodec stat = list.get(j);
                    String key = ArtefactPlugin.get().getName() + "-" + artefact.getId() + "-" + i;
                    if (stat == null || stat.getType() == null) return;
                    if (stat.trigger == TriggerType.PASSIVE) {
                        if (!comp.artefactList.containsKey(artefact) || comp.getAmount(artefact) == 0)
                            statMap.removeModifier(getEntityIndex(stat.getType()), key);
                        else
                            statMap.putModifier(getEntityIndex(stat.getType()), key, new StaticModifier(Modifier.ModifierTarget.MAX, stat.calc, stat.value * comp.getAmount(artefact)));
                    }
                }

                if (Objects.equals(artefact.getId(), "Shield_Artefact")) {
                    comp.shieldHud.displayShield(comp.getAmount(artefact) > 0);
                }
            });

            comp.artefactUpdated.clear();
        }

        private void checkShieldCooldown(int index, ArchetypeChunk<EntityStore> archetypeChunk, StatEffectComponent comp, long now, CommandBuffer<EntityStore> commandBuffer) {
            ArtefactCodec shield = ArtefactCodec.getAssetMap().getAsset("Shield_Artefact");
            if (shield == null) return;

            int stacks = comp.getAmount(shield);
            if (stacks <= 0) return;

            long lastUsed = comp.artefactCooldowns.getOrDefault(shield, 0L);
            long lastNotified = comp.lastNotifiedReady.getOrDefault(shield, 0L);

            if (lastUsed == 0 || lastNotified >= lastUsed) return;

            double baseCooldownMs = 60000.0;
            double calculatedCooldown = baseCooldownMs * Math.pow(0.9, stacks);
            long finalCooldown = (long) Math.max(10000.0, calculatedCooldown);

            if (now - lastUsed >= finalCooldown) {
                Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                EffectControllerComponent effectController = (EffectControllerComponent) commandBuffer.getComponent(ref, EffectControllerComponent.getComponentType());
                EntityEffect readyEffect = EntityEffect.getAssetMap().getAsset("Artefact_Shield_Ready");

                if (effectController != null && readyEffect != null) {
                    effectController.addEffect(ref, readyEffect, commandBuffer);

                    comp.lastNotifiedReady.put(shield, now);
                }

                comp.shieldHud.displayShield(true);

            }
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return this.query;
        }
    }

    public static class StatEffectRefChangeSystem extends RefChangeSystem<EntityStore, StatEffectComponent> {

        @Override
        public @NonNull ComponentType<EntityStore, StatEffectComponent> componentType() {
            return StatEffectComponent.getComponentType();
        }

        @Override
        public void onComponentAdded(@NonNull Ref<EntityStore> ref, @NonNull StatEffectComponent component, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            component.artefactHud = new ArtefactHud(component, playerRef);
            component.shieldHud = new ShieldHud(component, playerRef);
            if (playerRef != null)
                StatEffectSystem.clearTempStats(playerRef);
        }

        @Override
        public void onComponentSet(@NonNull Ref<EntityStore> ref, @Nullable StatEffectComponent component, @NonNull StatEffectComponent t1, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        }

        @Override
        public void onComponentRemoved(@NonNull Ref<EntityStore> ref, @NonNull StatEffectComponent component, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return StatEffectComponent.getComponentType();
        }
    }

    public static class StatEffectPreDamageSystem extends DamageEventSystem {

        private static final ComponentType<EntityStore, DamageDataComponent> DAMAGE_DATA_TYPE = DamageDataComponent.getComponentType();
        private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_TYPE = TransformComponent.getComponentType();
        private static final ComponentType<EntityStore, ProjectileComponent> PROJECTILE_TYPE = ProjectileComponent.getComponentType();


        @Override
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getFilterDamageGroup();
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return ArtefactPlugin.get().getStatEffectComponentType();
        }

        @Override
        public void handle(int i, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull Damage damage) {
            if (damage.isCancelled()) return;

            Damage.Source source = damage.getSource();
            Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(i);
            if (source instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> attackerRef = entitySource.getRef();
                if (attackerRef == targetRef)
                    return;
            }
            StatEffectComponent statComp = commandBuffer.getComponent(targetRef, StatEffectComponent.getComponentType());
            handleShieldArtefact(targetRef, statComp, commandBuffer, damage);
            if (source instanceof Damage.ProjectileSource projectileSource) {
                handleProjectileReflection(targetRef, commandBuffer, projectileSource, damage);
            }
        }

        private void handleProjectileReflection(Ref<EntityStore> targetRef, CommandBuffer<EntityStore> commandBuffer, Damage.ProjectileSource projectileSource, Damage damage) {
            StatEffectComponent statComp = commandBuffer.getComponent(targetRef, StatEffectComponent.getComponentType());
            if (statComp == null) return;

            ArtefactCodec reflectArtefact = ArtefactCodec.getAssetMap().getAsset("Projectile_Reflect_Artefact");
            if (reflectArtefact == null || statComp.getAmount(reflectArtefact) <= 0) return;

            DamageDataComponent damageData = commandBuffer.getComponent(targetRef, DAMAGE_DATA_TYPE);
            if (damageData == null || damageData.getCurrentWielding() == null) return;

            Ref<EntityStore> oldProjectileRef = projectileSource.getProjectile();
            if (!oldProjectileRef.isValid()) return;

            TransformComponent oldTransform = commandBuffer.getComponent(oldProjectileRef, TRANSFORM_TYPE);
            ProjectileComponent oldProj = commandBuffer.getComponent(oldProjectileRef, PROJECTILE_TYPE);

            if (oldTransform != null && oldProj != null) {

                createReflectedProjectile(targetRef, oldProjectileRef, oldTransform, oldProj, commandBuffer);
                commandBuffer.removeEntity(oldProjectileRef, RemoveReason.REMOVE);
            }
        }

        private void createReflectedProjectile(Ref<EntityStore> targetRef, Ref<EntityStore> oldRef, TransformComponent oldTransform, ProjectileComponent oldProj, CommandBuffer<EntityStore> commandBuffer) {
            String assetName = oldProj.getProjectileAssetName();
            if (assetName == null || assetName.isEmpty()) return;

            float bonusReflectDamage = 0;
            float reflectMult = 1;

            EntityStatMap playerStats = commandBuffer.getComponent(targetRef, EntityStatsModule.get().getEntityStatMapComponentType());
            EntityStatType reflectStatType = StatCodec.getStatFromString("Bonus_Projectile_Reflect_Damage");

            if (playerStats != null && reflectStatType != null) {
                int statIndex = ArtefactPlugin.getEntityStatTypeAssetStore().getIndex(reflectStatType.getId());
                Map<String, Modifier> modMap = playerStats.get(statIndex).getModifiers();
                if (modMap != null) {
                    for (Modifier mod : modMap.values()) {
                        StaticModifier modifier = (StaticModifier) mod;
                        if (modifier.getCalculationType() == StaticModifier.CalculationType.ADDITIVE)
                            bonusReflectDamage += modifier.getAmount();
                        else if (modifier.getCalculationType() == StaticModifier.CalculationType.MULTIPLICATIVE)
                            reflectMult += modifier.getAmount();
                    }
                }
            }


            Vector3f oldRot = oldTransform.getRotation();
            float newYaw = oldRot.getYaw() + (float) Math.PI;
            float newPitch = -oldRot.getPitch();
            Vector3f newRotation = new Vector3f(oldRot.getX(), newYaw, newPitch);

            Vector3d direction = new Vector3d();
            PhysicsMath.vectorFromAngles(newYaw, newPitch, direction);

            Vector3d spawnPos = oldTransform.getPosition().clone().addScaled(direction, 0.5);

            TimeResource time = commandBuffer.getResource(TimeResource.getResourceType());
            Holder<EntityStore> holder = ProjectileComponent.assembleDefaultProjectile(time, assetName, spawnPos, newRotation);
            ProjectileComponent newProjComp = holder.getComponent(ProjectileComponent.getComponentType());

            if (newProjComp != null) {
                if (newProjComp.getProjectile() == null) newProjComp.initialize();

                float baseDamage = (float) newProjComp.getProjectile().getDamage();
                float finalReflectDamage = (baseDamage + bonusReflectDamage) * reflectMult;

                if (baseDamage > 0) {
                    float multiplier = finalReflectDamage / baseDamage;
                    newProjComp.applyBrokenPenalty(1.0f - multiplier);
                }

                holder.ensureComponent(com.hypixel.hytale.server.core.modules.entity.component.Intangible.getComponentType());

                UUIDComponent playerUuidComp = commandBuffer.getComponent(targetRef, UUIDComponent.getComponentType());
                if (playerUuidComp != null) {
                    newProjComp.shoot(holder, playerUuidComp.getUuid(), spawnPos.x, spawnPos.y, spawnPos.z, newYaw, newPitch);
                }
            }

            commandBuffer.addEntity(holder, AddReason.SPAWN);
        }

        private void handleShieldArtefact(Ref<EntityStore> targetRef, StatEffectComponent statComp, CommandBuffer<EntityStore> commandBuffer, Damage damage) {
            ArtefactCodec shieldArtefact = ArtefactCodec.getAssetMap().getAsset("Shield_Artefact");
            if (shieldArtefact == null) return;

            int stacks = statComp.getAmount(shieldArtefact);
            if (stacks <= 0) return;


            double baseCooldownMs = 60000.0;
            double calculatedCooldown = baseCooldownMs * Math.pow(0.9, stacks);
            long finalCooldown = (long) Math.max(10000.0, calculatedCooldown);

            long now = commandBuffer.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();
            long lastUsed = statComp.artefactCooldowns.getOrDefault(shieldArtefact, 0L);

            if (now - lastUsed >= finalCooldown) {
                damage.setCancelled(true);

                statComp.artefactCooldowns.put(shieldArtefact, now);
                EffectControllerComponent effectController = commandBuffer.getComponent(targetRef, EffectControllerComponent.getComponentType());

                EntityEffect effect = EntityEffect.getAssetMap().getAsset("Artefact_Shield_Break");

                if (effectController != null && effect != null) {
                    effectController.addEffect(targetRef, effect, commandBuffer);
                }

                statComp.shieldHud.displayShield(false);
            }
        }

    }

    public static class StatEffectDeathSystem extends RefChangeSystem<EntityStore, DeathComponent> {

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public @NotNull ComponentType<EntityStore, DeathComponent> componentType() {
            return DeathComponent.getComponentType();
        }

        @Override
        public void onComponentAdded(@NotNull Ref<EntityStore> deadEntityRef, @NotNull DeathComponent death, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
            if (death.getDeathInfo() == null) return;
            Damage.Source source = death.getDeathInfo().getSource();
            if (source instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> attackerRef = entitySource.getRef();
                StatEffectComponent statComp = store.getComponent(attackerRef, StatEffectComponent.getComponentType());
                if (statComp != null) {
                    handleExplosionArtefact(deadEntityRef, attackerRef, statComp, store, commandBuffer);
                    handleSpeedOnKillArtefact(attackerRef, statComp, store, commandBuffer);
                }
            }
        }

        private void handleExplosionArtefact(Ref<EntityStore> deadEntityRef, Ref<EntityStore> attackerRef, StatEffectComponent statComp, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
            ArtefactCodec explodeArtefact = ArtefactCodec.getAssetMap().getAsset("Explosion_On_Kill_Artefact");
            if (explodeArtefact == null || statComp.getAmount(explodeArtefact) <= 0) return;

            float EXPLOSION_RADIUS = 5.0f;
            float BASE_DAMAGE = 5.0f;
            float bonusExplosionDamage = 0;
            float explosionMult = 1;

            EntityStatMap playerStats = store.getComponent(attackerRef, EntityStatsModule.get().getEntityStatMapComponentType());
            EntityStatType explosionStatType = StatCodec.getStatFromString("Bonus_Explosion_Damage");

            if (playerStats != null && explosionStatType != null) {
                int statIndex = ArtefactPlugin.getEntityStatTypeAssetStore().getIndex(explosionStatType.getId());
                Map<String, Modifier> modMap = playerStats.get(statIndex).getModifiers();
                if (modMap != null) {
                    for (Modifier mod : modMap.values()) {
                        StaticModifier modifier = (StaticModifier) mod;
                        if (modifier.getCalculationType() == StaticModifier.CalculationType.ADDITIVE)
                            bonusExplosionDamage += modifier.getAmount();
                        else if (modifier.getCalculationType() == StaticModifier.CalculationType.MULTIPLICATIVE)
                            explosionMult += modifier.getAmount();
                    }
                }
            }

            float finalExplosionDamage = (BASE_DAMAGE + bonusExplosionDamage) * explosionMult;
            TransformComponent deadTransform = store.getComponent(deadEntityRef, TransformComponent.getComponentType());
            if (deadTransform == null) return;
            Vector3d explosionPos = deadTransform.getPosition();

            World world = store.getExternalData().getWorld();
            ComponentAccessor<ChunkStore> chunkStore = world.getChunkStore().getStore();
            ExplosionConfig config = new ArtefactExplosionConfig(EXPLOSION_RADIUS, finalExplosionDamage);

            ParticleUtil.spawnParticleEffect("Explosion_Big", explosionPos, store);

            ExplosionUtils.performExplosion(
                    new Damage.EntitySource(attackerRef),
                    explosionPos,
                    config,
                    attackerRef,
                    commandBuffer,
                    chunkStore
            );
        }

        @Override
        public void onComponentSet(@NotNull Ref<EntityStore> ref, @Nullable DeathComponent oldComponent, @NotNull DeathComponent newComponent, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        }

        @Override
        public void onComponentRemoved(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent death, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        }

        private void handleSpeedOnKillArtefact(Ref<EntityStore> attackerRef, StatEffectComponent statComp, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
            ArtefactCodec speedArtefact = ArtefactCodec.getAssetMap().getAsset("Speed_On_Kill_Artefact");
            if (speedArtefact == null) return;

            int stacks = statComp.getAmount(speedArtefact);
            if (stacks <= 0) return;

            int cappedStacks = Math.min(stacks, 10);

            String effectName = "Speed_On_Kill_Effect" + cappedStacks;

            EffectControllerComponent effectController = store.getComponent(attackerRef, EffectControllerComponent.getComponentType());
            EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectName);

            if (effectController != null && effect != null) {
                effectController.addEffect(attackerRef, effect, commandBuffer);
            }
        }

        public static class ArtefactExplosionConfig extends ExplosionConfig {
            public ArtefactExplosionConfig(float radius, float damage) {
                this.damageEntities = true;
                this.entityDamageRadius = radius;
                this.entityDamage = damage;
                this.entityDamageFalloff = 0.0f;
                this.damageBlocks = false;
                this.knockback = null;
            }
        }
    }
}
