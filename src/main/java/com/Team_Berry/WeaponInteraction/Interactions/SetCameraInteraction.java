package com.Team_Berry.WeaponInteraction.Interactions;

import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SetCameraInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<SetCameraInteraction> CODEC;

    static {
        CODEC = BuilderCodec.builder(SetCameraInteraction.class, SetCameraInteraction::new, SimpleInstantInteraction.CODEC)
                .documentation("Sets the camera POV for the owning player")
                .append(new KeyedCodec<>("CameraName", Codec.STRING),
                        (interaction, o) -> interaction.cameraName = o,
                        (interaction) -> interaction.cameraName)
                .documentation("The camera name to apply").add()
                .build();
    }

    protected String cameraName;

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = interactionContext.getOwningEntity();

        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef == null || cameraName == null || cameraName.isEmpty()) return;

        CameraInitializer cam = CameraInitializer.get(cameraName);
        if (cam != null && cam.isActive) ref.getStore().getExternalData().getWorld().execute(() -> CameraInitializer.setPlayerPov(cameraName, playerRef));
    }
}