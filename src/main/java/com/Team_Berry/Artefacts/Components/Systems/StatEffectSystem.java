package com.Team_Berry.Artefacts.Components.Systems;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.Enums.TriggerType;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
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
import java.util.Map;

public class StatEffectSystem {
    public static void register() {
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectTickingSystem(EntityStatsModule.get().getEntityStatMapComponentType()));
        ArtefactPlugin.get().getEntityStoreRegistry().registerSystem(new StatEffectDamageSystem());
    }

    public static class StatEffectDamageSystem extends DamageEventSystem {
        EntityStatType bonusDamageType;
        EntityStatType bonusCritChanceType;
        EntityStatType bonusCritDamageType;
        EntityStatType bonusArmorType;
        public StatEffectDamageSystem() {}

        @Nonnull
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }

        private void refreshStatType() {
            if (bonusDamageType == null)
                bonusDamageType = StatCodec.getStatFromString("Bonus_Damage");
            if (bonusCritChanceType == null)
                bonusCritChanceType = StatCodec.getStatFromString("Crit_Rate");
            if (bonusCritDamageType == null)
                bonusCritDamageType = StatCodec.getStatFromString("Crit_Damage");
            if (bonusArmorType == null)
                bonusArmorType = StatCodec.getStatFromString("Bonus_Armor");
        }

        @Override
        public void handle(int i, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull Damage damage) {
            refreshStatType();
            if (damage.getAmount() <= 0.0F)
                return;
            Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(i);
            Damage.Source source = damage.getSource();
            if (source instanceof Damage.EntitySource entitySource) {
                float damageValue = damage.getAmount();
                Ref<EntityStore> attackerRef = entitySource.getRef();
                EntityStatMap statMapAttack = commandBuffer.getComponent(attackerRef, EntityStatMap.getComponentType());
                EntityStatMap statMapTarget = commandBuffer.getComponent(targetRef, EntityStatMap.getComponentType());
                damageValue = applyBonusAttack(damageValue, statMapAttack);
                damageValue = applyCritical(damageValue, statMapAttack);
                damageValue = applyArmor(damageValue, statMapTarget);
                damage.setAmount(damageValue);
            }
        }

        public float applyBonusAttack(float damageValue, EntityStatMap entityStatMap) {
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
            ArtefactPlugin.LOGGER.atInfo().log("Applying Critical Damage");
            if (bonusCritChanceType != null && entityStatMap != null) {
                ArtefactPlugin.LOGGER.atInfo().log("Successfully entered Critical Rate");
                float odd = 0;
                Map<String, Modifier> modMap = entityStatMap.get(getEntityIndex(bonusCritChanceType)).getModifiers();
                if (modMap != null) {
                    for (Modifier mod : modMap.values()) {
                        StaticModifier modifier = (StaticModifier) mod;
                        odd += modifier.getAmount();
                    }
                }
                ArtefactPlugin.LOGGER.atInfo().log("Critical Rate at " + odd*100 + "%");
                if (Math.random() < (odd)) {
                    float mult = 2;
                    if (bonusCritDamageType != null) {
                        ArtefactPlugin.LOGGER.atInfo().log("Successfully entered Critical Damage");
                        Map<String, Modifier> modCritMap = entityStatMap.get(getEntityIndex(bonusCritDamageType)).getModifiers();
                        if (modCritMap != null) {
                            for (Modifier mod : modCritMap.values()) {
                                StaticModifier modifier = (StaticModifier) mod;
                                mult += modifier.getAmount();
                            }
                        }
                    }
                    ArtefactPlugin.LOGGER.atInfo().log("Critical Damage at " + mult * 100 + "%");
                    return damageValue * mult;
                }
            }
            return damageValue;
        }
        public float applyArmor(float damageValue, EntityStatMap entityStatMap) {
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

        @Override
        public void tick(float dt, int index, @NonNull ArchetypeChunk archetypeChunk, @NonNull Store store, @NonNull CommandBuffer commandBuffer) {
            StatEffectComponent comp = (StatEffectComponent) archetypeChunk.getComponent(index, ArtefactPlugin.get().getStatEffectComponentType());
            if (comp == null || comp.artefactUpdated.isEmpty())
                return;
            else {
                EntityStatMap statMap = (EntityStatMap)archetypeChunk.getComponent(index, EntityStatsModule.get().getEntityStatMapComponentType());
                if (statMap == null)
                    return;
                comp.artefactUpdated.forEach(artefact -> {
                    ArtefactPlugin.LOGGER.atInfo().log("Changed artefact " + artefact.getId());
                    ArtefactPlugin.LOGGER.atInfo().log("StatListSize " + artefact.statList.size());
                    for (int i = 0; i < artefact.statList.size(); i++) {
                        StatCodec stat = artefact.statList.get(i);
                        String key = ArtefactPlugin.get().getName() + "-" + artefact.getId() + "-" + i;
                        if (stat == null || stat.type == null)
                            return;
                        ArtefactPlugin.LOGGER.atInfo().log("Stat & StatType Not Null");
                        if (stat.trigger == TriggerType.PASSIVE) {
                            if (!comp.artefactList.containsKey(artefact) || comp.getAmount(artefact) == 0)
                                statMap.removeModifier(getEntityIndex(stat.type), key);
                            else
                                statMap.putModifier(getEntityIndex(stat.type), key, new StaticModifier(Modifier.ModifierTarget.MAX, stat.calc, stat.value * comp.getAmount(artefact)));
                        }
                    }
                });
                comp.artefactUpdated.clear();
                /*for (int i = 0; i < statMap.size(); i++) {
                    String s = statMap.get(i).getId() + " - ";
                    try {
                        s += statMap.get(i).getModifiers().toString();
                    } catch (NullPointerException e) {
                        s += "None";
                    }
                    ArtefactPlugin.LOGGER.atWarning().log(s);
                }*/
            }
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
