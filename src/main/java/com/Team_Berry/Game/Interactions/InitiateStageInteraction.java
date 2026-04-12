package com.Team_Berry.Game.Interactions;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class InitiateStageInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<InitiateStageInteraction> CODEC = BuilderCodec.builder(InitiateStageInteraction.class, InitiateStageInteraction::new, SimpleInstantInteraction.CODEC).build();

    @Override
    protected void firstRun(@NotNull InteractionType interactionType, @NotNull InteractionContext interactionContext, @NotNull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        PlayerRef playerRef = commandBuffer.getComponent(interactionContext.getOwningEntity(), PlayerRef.getComponentType());
        playerRef.sendMessage(Message.raw("interaction started"));
        World world = InstancesPlugin.get().spawnInstance("Haven", playerRef.getReference().getStore().getExternalData().getWorld(), commandBuffer.getComponent(playerRef.getReference(), TransformComponent.getComponentType()).getTransform()).join();
        playerRef.sendMessage(Message.raw(world.getName()));

    }
}
