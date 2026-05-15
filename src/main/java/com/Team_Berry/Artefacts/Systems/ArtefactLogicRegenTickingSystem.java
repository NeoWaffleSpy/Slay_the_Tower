package com.Team_Berry.Artefacts.Systems;

import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnTickRegen;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ArtefactLogicRegenTickingSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return StatEffectComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> cb) {
        Ref<EntityStore> playerRef = chunk.getReferenceTo(index);
        StatEffectComponent statComp = chunk.getComponent(index, StatEffectComponent.getComponentType());

        if (statComp != null) {
            statComp.triggerLogic(IOnTickRegen.class, (artefact, logic) -> {
                int stacks = statComp.getAmount(artefact);
                if (stacks > 0) {
                    logic.onTickRegen(artefact, stacks, playerRef, store, dt);
                }
            });
        }
    }
}