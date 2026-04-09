package com.Team_Berry.Artefacts.Components.Systems;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Enums.TargetType;
import com.Team_Berry.Artefacts.Codecs.Enums.TriggerType;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
import com.Team_Berry.Utils.TooltipInjector.StringFormatter;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StatEffectSystem {
    private static final KeyedScheduler scheduler = new KeyedScheduler();

    public static void register() {
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectTickingSystem(EntityStatsModule.get().getEntityStatMapComponentType()));
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectDamageSystem());
    }

    public static class StatEffectDamageSystem extends DamageEventSystem {
        public StatEffectDamageSystem() {}

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

    private static void applyTempStats(StatEffectComponent originEntityComponent, EntityStatMap originEntityStatMap, EntityStatMap targetEntityStatMap, TriggerType type) {
        if (originEntityComponent == null || targetEntityStatMap == null)
            return;
        originEntityComponent.artefactList.forEach(((artefact, count) -> {
            artefact.getStatArray().forEach(stat -> {
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
                targetMap.putModifier(getEntityIndex(stat.getType()), key, new StaticModifier(Modifier.ModifierTarget.MAX, stat.calc,stat.value*count));
                scheduler.schedule(key, () -> targetMap.removeModifier(getEntityIndex(stat.getType()), key), (long) (stat.duration*1000), TimeUnit.MILLISECONDS);
            });
        }));
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

        @Override
        public void tick(float dt, int index, @NonNull ArchetypeChunk archetypeChunk, @NonNull Store store, @NonNull CommandBuffer commandBuffer) {
            StatEffectComponent comp = (StatEffectComponent) archetypeChunk.getComponent(index, ArtefactPlugin.get().getStatEffectComponentType());
            if (comp == null || comp.artefactUpdated.isEmpty())
                return;
            EntityStatMap statMap = (EntityStatMap)archetypeChunk.getComponent(index, EntityStatsModule.get().getEntityStatMapComponentType());
            if (statMap == null)
                return;
            comp.artefactUpdated.forEach(artefact -> {
                ArrayList<StatCodec> list = artefact.getStatArray();
                for (int i = 0; i < list.size(); i++) {
                    StatCodec stat = list.get(i);
                    String key = ArtefactPlugin.get().getName() + "-" + artefact.getId() + "-" + i;
                    if (stat == null || stat.getType() == null)
                        return;
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
        public @Nullable Query getQuery() {
            return this.query;
        }
    }

    private static int getEntityIndex(EntityStatType statType) {
        return ArtefactPlugin.getEntityStatTypeAssetStore().getIndex(statType.getId());
    }
}
