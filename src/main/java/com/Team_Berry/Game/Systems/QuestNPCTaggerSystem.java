package com.Team_Berry.Game.Systems;

import com.Team_Berry.Game.Components.QuestNPCComponent;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuestNPCTaggerSystem extends EntityTickingSystem<EntityStore> {

    ComponentType<EntityStore, QuestNPCComponent> questNPCComponentType;
    private int tickDelay = 0;

    public QuestNPCTaggerSystem(ComponentType<EntityStore, QuestNPCComponent> questNPCComponentType) {
        this.questNPCComponentType = questNPCComponentType;
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return questNPCComponentType;
    }

    @Override
    public void tick(float v, int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {

        tickDelay++;

        if (tickDelay >= 20) {
            int mobCount = archetypeChunk.size();

            GamePlugin.LOGGER.atInfo().log(
                    "SUCCESS! Tagger System found %d quest mobs in this chunk!", mobCount
            );

            tickDelay = 0;
        }
    }
}