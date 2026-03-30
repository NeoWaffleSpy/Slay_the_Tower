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

public class StatEffectSystem {
    public static class StatEffectDamageSystem extends DamageEventSystem {
        public StatEffectDamageSystem() {}

        @Override
        public void handle(int i, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull Damage damage) {

        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return null;
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
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
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
                        String key = SlayTheTower.get().getName() + "-" + artefact.getId() + "-" + i;
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

        private int getEntityIndex(EntityStatType statType) {
            return ArtefactPlugin.getEntityStatTypeAssetStore().getIndex(statType.getId());
        }

        @Override
        public @Nullable Query getQuery() {
            return this.query;
        }
    }
}
