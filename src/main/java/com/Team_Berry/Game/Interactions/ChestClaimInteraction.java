package com.Team_Berry.Game.Interactions;

import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class ChestClaimInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<ChestClaimInteraction> CODEC = BuilderCodec.builder(ChestClaimInteraction.class, ChestClaimInteraction::new, SimpleInstantInteraction.CODEC).build();

    @Override
    protected void firstRun(@NotNull InteractionType interactionType, @NotNull InteractionContext interactionContext, @NotNull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        Ref<EntityStore> ref = interactionContext.getOwningEntity();
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef == null || !playerRef.getReference().isValid()) return;

        // Extract the target block position using the method you found
        BlockPosition targetBlock = interactionContext.getTargetBlock();
        if (targetBlock == null) return;

        World world = playerRef.getReference().getStore().getExternalData().getWorld();
        GameManager manager = GamePlugin.get().getGameManagers().get(world);

        if (manager != null) {
            // Pass the BlockPosition and PlayerRef directly to the GameManager
            manager.playerChestClaim(targetBlock, playerRef);
        }
    }
}