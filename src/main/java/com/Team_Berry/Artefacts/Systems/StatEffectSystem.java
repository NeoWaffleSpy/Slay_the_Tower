package com.Team_Berry.Artefacts.Systems;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Enums.TargetType;
import com.Team_Berry.Artefacts.Codecs.Enums.TriggerType;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IArtefactLogic;
import com.Team_Berry.Artefacts.Interfaces.IOnArtefactUpdate;
import com.Team_Berry.Artefacts.Registry.ArtefactLogicRegistry;
import com.Team_Berry.Artefacts.UI.ArtefactHud;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StatEffectSystem {
    private static final KeyedScheduler scheduler = new KeyedScheduler();

    public static void register() {
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectTickingSystem(EntityStatsModule.get().getEntityStatMapComponentType()));
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectDamageSystem());
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectRefChangeSystem());
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

    public static void applyTempStats(StatEffectComponent originEntityComponent, EntityStatMap originEntityStatMap, EntityStatMap targetEntityStatMap, TriggerType type) {
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

                StatEffectSystem.applyTempStats(commandBuffer.getComponent(attackerRef, StatEffectComponent.getComponentType()), statMapAttack, statMapTarget, TriggerType.ON_HIT);
                int damageIndex = DamageCause.getAssetMap().getIndex("Skill");
                if (damageIndex == damage.getDamageCauseIndex())
                    StatEffectSystem.applyTempStats(commandBuffer.getComponent(attackerRef, StatEffectComponent.getComponentType()), statMapAttack, statMapTarget, TriggerType.ON_SKILL_USE);
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


            if (comp.artefactUpdated.isEmpty()) return;

            EntityStatMap statMap = (EntityStatMap) archetypeChunk.getComponent(i, EntityStatsModule.get().getEntityStatMapComponentType());
            if (statMap == null) return;

            Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(i);

            comp.artefactUpdated.forEach(artefact -> {

                IArtefactLogic logic = ArtefactLogicRegistry.getLogic(artefact);
                if (logic instanceof IOnArtefactUpdate) {
                    int currentStacks = comp.getAmount(artefact);
                    ((IOnArtefactUpdate) logic).onStackChange(artefact, targetRef, comp, commandBuffer, currentStacks);
                }

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

            });

            comp.artefactUpdated.clear();
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
}