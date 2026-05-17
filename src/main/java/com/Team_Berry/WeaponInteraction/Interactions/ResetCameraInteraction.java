package com.Team_Berry.WeaponInteraction.Interactions;

import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ResetCameraInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<ResetCameraInteraction> CODEC = BuilderCodec.builder(ResetCameraInteraction.class, ResetCameraInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Resets the camera POV for the owning player")
            .build();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = interactionContext.getOwningEntity();

        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef == null) return;

        Store<EntityStore> store = ref.getStore();
        PlayerPOVComponent pPOV = store.getComponent(ref, PlayerPOVComponent.getComponentType());
        if (pPOV != null) {
            store.getExternalData().getWorld().execute(() -> CameraInitializer.deletePOV(playerRef));
        }
    }
}