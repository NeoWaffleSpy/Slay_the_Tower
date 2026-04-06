package com.Team_Berry.Game.Systems;

import com.Team_Berry.Game.Components.QuestNPCComponent;
import com.Team_Berry.Game.GameManager;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuestNPCDeathSystem extends RefSystem<EntityStore> {
    private final ComponentType<EntityStore, QuestNPCComponent> questNPCComponentType;
    private final GameManager gameManager;

    public QuestNPCDeathSystem(ComponentType<EntityStore, QuestNPCComponent> questNPCComponentType, GameManager gameManager) {
        this.questNPCComponentType = questNPCComponentType;
        this.gameManager = gameManager;
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType(), questNPCComponentType);
    }

    @Override
    public void onEntityAdded(@NotNull Ref<EntityStore> ref, @NotNull AddReason addReason, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {

    }

    @Override
    public void onEntityRemove(@NotNull Ref<EntityStore> ref, @NotNull RemoveReason removeReason, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        System.out.println("npc just died oh");
    }


}
