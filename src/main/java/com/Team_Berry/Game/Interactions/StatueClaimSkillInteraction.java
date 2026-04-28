package com.Team_Berry.Game.Interactions;

import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class StatueClaimSkillInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<StatueClaimSkillInteraction> CODEC = BuilderCodec.builder(StatueClaimSkillInteraction.class, StatueClaimSkillInteraction::new, SimpleInstantInteraction.CODEC).build();

    @Override
    protected void firstRun(@NotNull InteractionType interactionType, @NotNull InteractionContext interactionContext, @NotNull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        PlayerRef playerRef = commandBuffer.getComponent(interactionContext.getOwningEntity(), PlayerRef.getComponentType());
        Ref<EntityStore> ref = interactionContext.getOwningEntity();
        if (playerRef == null || !playerRef.getReference().isValid()) return;
        World world = playerRef.getReference().getStore().getExternalData().getWorld();

        GameManager manager = GamePlugin.get().getGameManagers().get(world);
        if (manager != null) {
            manager.grantSkillRewards(playerRef);
        }
    }
}