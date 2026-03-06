package com.Team_Berry.Slay.Component.System;

import com.Team_Berry.Slay.Camera.CameraInitializer;
import com.Team_Berry.Slay.Component.Data.PlayerPOVComponent;
import com.Team_Berry.Slay.SlayTheTower;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PlayerPOVSystem extends RefChangeSystem<EntityStore, PlayerPOVComponent> {
    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return PlayerPOVComponent.getComponentType();
    }

    @Override
    public @NonNull ComponentType<EntityStore, PlayerPOVComponent> componentType() {
        return SlayTheTower.get().getPlayerPOVComponentType();
    }

    @Override
    public void onComponentAdded(@NonNull Ref<EntityStore> ref, @NonNull PlayerPOVComponent playerPOVComponent, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        CameraInitializer cam = CameraInitializer.get(playerPOVComponent.getPOVName());
        if (cam == null) {
            throw new UnsupportedOperationException("Invalid Camera POV");
        }
        cam.setPOV(store.getComponent(ref, PlayerRef.getComponentType()));
    }

    @Override
    public void onComponentSet(@NonNull Ref<EntityStore> ref, @Nullable PlayerPOVComponent playerPOVComponent, @NonNull PlayerPOVComponent t1, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        assert playerPOVComponent != null;
        CameraInitializer cam = CameraInitializer.get(playerPOVComponent.getPOVName());
        if (cam == null) {
            throw new UnsupportedOperationException("Invalid Camera POV");
        }
        cam.setPOV(store.getComponent(ref, PlayerRef.getComponentType()));
    }

    @Override
    public void onComponentRemoved(@NonNull Ref<EntityStore> ref, @NonNull PlayerPOVComponent playerPOVComponent, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        assert pr != null;
        CameraInitializer.resetCamera(pr);
    }
}
