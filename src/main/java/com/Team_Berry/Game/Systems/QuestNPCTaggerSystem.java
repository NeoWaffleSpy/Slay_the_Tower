package com.Team_Berry.Game.Systems;

import com.Team_Berry.Game.Components.QuestNPCComponent;
import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuestNPCTaggerSystem extends RefChangeSystem<EntityStore, NPCEntity> {

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public @NotNull ComponentType<EntityStore, NPCEntity> componentType() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void onComponentAdded(@NotNull Ref<EntityStore> ref, @NotNull NPCEntity npc, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        World world = store.getExternalData().getWorld();

        GameManager manager = GamePlugin.get().getGameManagers().get(world);

        if (manager != null) {
            commandBuffer.addComponent(ref, GamePlugin.getQuestNPCComponentType(), new QuestNPCComponent(null));
        }
    }

    @Override
    public void onComponentSet(@NotNull Ref<EntityStore> ref, @Nullable NPCEntity old, @NotNull NPCEntity newComp, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentRemoved(@NotNull Ref<EntityStore> ref, @NotNull NPCEntity npc, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
    }
}