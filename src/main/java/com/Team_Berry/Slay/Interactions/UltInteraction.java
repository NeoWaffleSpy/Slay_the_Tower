package com.Team_Berry.Slay.Interactions;

import com.Team_Berry.Slay.Camera.CameraInitializer;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UltInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<UltInteraction> CODEC = BuilderCodec.builder(UltInteraction.class, UltInteraction::new, SimpleInstantInteraction.CODEC).build();

    @Override
    protected void firstRun(@NotNull InteractionType interactionType, @NotNull InteractionContext interactionContext, @NotNull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        Ref<EntityStore> ref = interactionContext.getOwningEntity();
        Store<EntityStore> store = ref.getStore();
        World world = commandBuffer.getExternalData().getWorld();
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        playerRef.sendMessage(Message.raw("Weapon in hand is : " + interactionContext.getHeldItem().getItemId()));

        TransformComponent transformComponent = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        Vector3d playerPosition = transformComponent.getPosition();
        ParticleUtil.spawnParticleEffect("Explosion_Medium", playerPosition, commandBuffer);
        //world.execute(() -> CameraInitializer.setPlayerPov("topDown", playerRef));
        List<Ref<EntityStore>> nearbyEntities = TargetUtil.getAllEntitiesInSphere(playerPosition, 5d, commandBuffer);

        for (Ref<EntityStore> entityRef : nearbyEntities) {
            if (entityRef.isValid()) {
                playerRef.sendMessage(Message.raw("found entity!"));

            }
        }
    }
}
