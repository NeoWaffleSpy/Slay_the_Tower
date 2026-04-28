package com.Team_Berry.Game.Interactions;

import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class StartRoomFromLobbyInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<StartRoomFromLobbyInteraction> CODEC = BuilderCodec.builder(StartRoomFromLobbyInteraction.class, StartRoomFromLobbyInteraction::new, SimpleInstantInteraction.CODEC).build();

    @Override
    protected void firstRun(@NotNull InteractionType interactionType, @NotNull InteractionContext interactionContext, @NotNull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        World world = commandBuffer.getStore().getExternalData().getWorld();
        PlayerRef playerRef = commandBuffer.getComponent(interactionContext.getOwningEntity(), PlayerRef.getComponentType());
        if (playerRef == null) return;
        if (world != null) {
            GameManager manager = GamePlugin.get().getGameManagers().get(world);
            if (manager != null) {
                manager.startRoomFromLobby();
            }
        }
    }
}