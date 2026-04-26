package com.Team_Berry.Game.Systems;

import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceBlockProtectionSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public PlaceBlockProtectionSystem(@NotNull Class<PlaceBlockEvent> eventType) {
        super(eventType);
    }

    @Override
    public void handle(int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull PlaceBlockEvent placeBlockEvent) {
        World world = store.getExternalData().getWorld();

        if (GamePlugin.get().getGameManager(world) != null) {
            placeBlockEvent.setCancelled(true);
        }
    }


    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.any();
    }
}