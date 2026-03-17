package com.Team_Berry.WeaponInteraction.Interactions;

import com.Team_Berry.WeaponInteraction.Component.BleedComponent;
import com.Team_Berry.WeaponInteraction.Utils.BleedStage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class BleedInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<BleedInteraction> CODEC = BuilderCodec.builder(BleedInteraction.class, BleedInteraction::new, SimpleInstantInteraction.CODEC).build();
    public static ComponentType<EntityStore, BleedComponent> BLEED_COMPONENT_TYPE;
    public static final long BLEED_DURATION = 5000L;

    @Override
    protected void firstRun(@NotNull InteractionType interactionType, @NotNull InteractionContext interactionContext, @NotNull CooldownHandler cooldownHandler) {

        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        String itemId = interactionContext.getHeldItem().getItemId();


        Ref<EntityStore> bleedTarget = interactionContext.getTargetEntity();
        if (bleedTarget == null) return;
        BleedComponent bleedComponent = commandBuffer.getComponent(bleedTarget, BLEED_COMPONENT_TYPE);
        long now = commandBuffer.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();

        if(bleedComponent == null) {
            bleedComponent = new BleedComponent();
            commandBuffer.addComponent(bleedTarget, BLEED_COMPONENT_TYPE, bleedComponent);
        }
        bleedComponent.applyStacks(1, now, BLEED_DURATION, itemId);
        EffectControllerComponent effectController = commandBuffer.getComponent(bleedTarget, EffectControllerComponent.getComponentType());
        EntityEffect effect = EntityEffect.getAssetMap().getAsset("Bleed_Effect");
        OverlapBehavior overlap = effect.getOverlapBehavior();
        effectController.addEffect(bleedTarget, effect, BLEED_DURATION, overlap, commandBuffer);

        Ref<EntityStore> ref = interactionContext.getOwningEntity();
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());


        String currentStage = switch (bleedComponent.getEffectStage()) {
            case BleedStage.Stage1 -> "Stage1";
            case BleedStage.Stage2 -> "Stage2";
            case BleedStage.Stage3 -> "Stage3";
            case BleedStage.Bleeding -> "Bleeding"; // This is your MAX_BLEED_STACKS
            default -> "None";
        };
        playerRef.sendMessage(Message.raw("BleedStacks : " + bleedComponent.getCurrentBleedStacks() + " current stage : " + currentStage));
    }


}
