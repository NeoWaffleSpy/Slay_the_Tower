package com.Team_Berry.Artefacts.Systems;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IArtefactLogic;
import com.Team_Berry.Artefacts.Interfaces.ICooldownArtefact;
import com.Team_Berry.Artefacts.Registry.ArtefactLogicRegistry;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArtefactCooldownSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(ArtefactPlugin.get().getStatEffectComponentType());
    }

    @Override
    public void tick(float v, int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        StatEffectComponent comp = (StatEffectComponent) archetypeChunk.getComponent(i, ArtefactPlugin.get().getStatEffectComponentType());
        if (comp == null || comp.artefactList.isEmpty()) return;

        long now = store.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();
        Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(i);

        for (ArtefactCodec artefact : comp.artefactList.keySet()) {
            if (comp.getAmount(artefact) > 0) {
                IArtefactLogic logic = ArtefactLogicRegistry.getLogic(artefact);
                if (logic instanceof ICooldownArtefact) {
                    ICooldownArtefact cdLogic = (ICooldownArtefact) logic;

                    long lastUsed = comp.artefactCooldowns.getOrDefault(artefact, 0L);
                    long lastNotified = comp.lastNotifiedReady.getOrDefault(artefact, 0L);

                    if (lastUsed != 0 && lastNotified < lastUsed) {
                        long finalCooldown = cdLogic.getCooldownDuration(artefact, comp.getAmount(artefact));

                        if (now - lastUsed >= finalCooldown) {
                            cdLogic.onCooldownReady(artefact, targetRef, comp, commandBuffer);
                            comp.lastNotifiedReady.put(artefact, now);
                        }
                    }
                }
            }
        }
    }
}