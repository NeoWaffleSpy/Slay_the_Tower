package com.Team_Berry.Artefacts.Components.Systems;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Enums.TriggerType;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.SlayTheTower;
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
        EntityStatType bonusArmorType;
        public StatEffectDamageSystem() {}

        @Nonnull
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }

        @Override
        public void handle(int i, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull Damage damage) {
            if (bonusDamageType == null)
                bonusDamageType = StatCodec.getStatFromString("Bonus_Damage");
            if (bonusArmorType == null)
                bonusArmorType = StatCodec.getStatFromString("Bonus_Armor");
            if (damage.getAmount() <= 0.0F)
                return;
            Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(i);
            Damage.Source source = damage.getSource();
            if (source instanceof Damage.EntitySource entitySource) {
                float damageValue = damage.getAmount();
                Ref<EntityStore> attackerRef = entitySource.getRef();
                EntityStatMap statMapAttack = commandBuffer.getComponent(attackerRef, EntityStatMap.getComponentType());
                if (bonusDamageType != null && statMapAttack != null) {
                    float mult = 1;
                    Map<String, Modifier> modMap = statMapAttack.get(getEntityIndex(bonusDamageType)).getModifiers();
                    if (modMap != null) {
                        for (Modifier mod : modMap.values()) {
                            StaticModifier modifier = (StaticModifier) mod;
                            if (modifier.getCalculationType() == StaticModifier.CalculationType.ADDITIVE)
                                damageValue += modifier.getAmount();
                            else if (modifier.getCalculationType() == StaticModifier.CalculationType.MULTIPLICATIVE)
                                mult += modifier.getAmount();
                        }
                        damageValue *= mult;
                    }
                }
                EntityStatMap statMapTarget = commandBuffer.getComponent(targetRef, EntityStatMap.getComponentType());
                if (bonusArmorType != null && statMapTarget != null) {
                    float div = 1;
                    Map<String, Modifier> modMap = statMapTarget.get(getEntityIndex(bonusArmorType)).getModifiers();
                    if (modMap != null) {
                        for (Modifier mod : modMap.values()) {
                            StaticModifier modifier = (StaticModifier) mod;
                            if (modifier.getCalculationType() == StaticModifier.CalculationType.ADDITIVE)
                                damageValue -= modifier.getAmount();
                            else if (modifier.getCalculationType() == StaticModifier.CalculationType.MULTIPLICATIVE)
                                div *= modifier.getAmount();
                        }
                        damageValue *= div;
                    }
                }
                damage.setAmount(damageValue);
            }
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
                    for (int i = 0; i < artefact.statList.size(); i++) {
                        StatCodec stat = artefact.statList.get(i);
                        String key = ArtefactPlugin.get().getName() + "-" + artefact.getId() + "-" + i;
                        if (stat == null || stat.type == null)
                            return;
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
