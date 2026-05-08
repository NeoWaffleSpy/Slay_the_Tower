package com.Team_Berry.Artefacts.Artefact;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ShieldArtefact implements IOnTakeDamage, ICooldownArtefact, IOnArtefactUpdate {

    @Override
    public long getCooldownDuration(int stacks) {
        double baseCooldownMs = 60000.0;
        return (long) Math.max(10000.0, baseCooldownMs * Math.pow(0.9, stacks));
    }

    @Override
    public void onCooldownReady(ArtefactCodec codec, Ref<EntityStore> targetRef, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {
        EffectControllerComponent effectController = cmds.getComponent(targetRef, EffectControllerComponent.getComponentType());
        EntityEffect readyEffect = EntityEffect.getAssetMap().getAsset("Artefact_Shield_Ready");

        if (effectController != null && readyEffect != null) {
            effectController.addEffect(targetRef, readyEffect, cmds);
        }
        if (statComp.shieldHud != null) {
            statComp.shieldHud.displayShield(true);
        }
    }

    @Override
    public void onStackChange(ArtefactCodec codec, Ref<EntityStore> targetRef, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds, int currentStacks) {
        if (statComp.shieldHud == null) return;

        if (currentStacks > 0) {
            long now = cmds.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();
            long lastUsed = statComp.artefactCooldowns.getOrDefault(codec, 0L);
            long cooldown = getCooldownDuration(currentStacks);

            boolean isReady = (lastUsed == 0 || now - lastUsed >= cooldown);
            statComp.shieldHud.displayShield(isReady);
        } else {
            statComp.shieldHud.displayShield(false);
        }
    }

    @Override
    public void onTakeDamage(ArtefactCodec codec, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {
        long now = cmds.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();
        long lastUsed = statComp.artefactCooldowns.getOrDefault(codec, 0L);
        long finalCooldown = getCooldownDuration(statComp.getAmount(codec));

        if (now - lastUsed >= finalCooldown) {
            damage.setCancelled(true);
            statComp.artefactCooldowns.put(codec, now);

            EffectControllerComponent effectController = cmds.getComponent(targetRef, EffectControllerComponent.getComponentType());
            EntityEffect breakEffect = EntityEffect.getAssetMap().getAsset("Artefact_Shield_Break");
            if (effectController != null && breakEffect != null) {
                effectController.addEffect(targetRef, breakEffect, cmds);
            }
            if (statComp.shieldHud != null) {
                statComp.shieldHud.displayShield(false);
            }
        }
    }
}