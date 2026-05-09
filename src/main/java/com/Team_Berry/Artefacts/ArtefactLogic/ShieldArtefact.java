package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.ICooldownArtefact;
import com.Team_Berry.Artefacts.Interfaces.IOnArtefactUpdate;
import com.Team_Berry.Artefacts.Interfaces.IOnTakePreDamage;
import com.Team_Berry.Artefacts.UI.ShieldHud;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ShieldArtefact implements IOnTakePreDamage, ICooldownArtefact, IOnArtefactUpdate {

    @Override
    public long getCooldownDuration(ArtefactCodec codec, int stacks) {
        float baseCooldown = codec.getLogicNumber("baseCooldown", 60000.0f);
        float minCooldown = codec.getLogicNumber("minCooldown", 10000.0f);
        float stackMultiplier = codec.getLogicNumber("stackMultiplier", 0.9f);

        return (long) Math.max(minCooldown, baseCooldown * Math.pow(stackMultiplier, stacks));
    }

    @Override
    public void onCooldownReady(ArtefactCodec codec, Ref<EntityStore> targetRef, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {
        EffectControllerComponent effectController = cmds.getComponent(targetRef, EffectControllerComponent.getComponentType());

        String readyEffectName = codec.getLogicString("readyEffect", "Artefact_Shield_Ready");
        EntityEffect readyEffect = EntityEffect.getAssetMap().getAsset(readyEffectName);

        if (effectController != null && readyEffect != null) {
            effectController.addEffect(targetRef, readyEffect, cmds);
        }
        ShieldHud hud = getOrInitShieldHud(statComp, targetRef, cmds);
        if (hud != null) {
            hud.displayShield(true);
        }
    }

    private ShieldHud getOrInitShieldHud(StatEffectComponent statComp, Ref<EntityStore> targetRef, CommandBuffer<EntityStore> cmds) {
        if (!statComp.customArtefactData.containsKey("ShieldHud")) {
            PlayerRef playerRef = cmds.getComponent(targetRef, PlayerRef.getComponentType());
            if (playerRef != null) {
                statComp.customArtefactData.put("ShieldHud", new ShieldHud(statComp, playerRef));
            }
        }
        return (ShieldHud) statComp.customArtefactData.get("ShieldHud");
    }

    @Override
    public void onStackChange(ArtefactCodec codec, Ref<EntityStore> targetRef, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds, int currentStacks) {
        ShieldHud hud = getOrInitShieldHud(statComp, targetRef, cmds);
        if (hud == null) return;

        if (currentStacks > 0) {
            long now = cmds.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();
            long lastUsed = statComp.artefactCooldowns.getOrDefault(codec, 0L);
            long cooldown = getCooldownDuration(codec, currentStacks);

            boolean isReady = (lastUsed == 0 || now - lastUsed >= cooldown);
            hud.displayShield(isReady);
        } else {
            hud.displayShield(false);
        }
    }

    @Override
    public void onTakePreDamage(ArtefactCodec codec, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {
        long now = cmds.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();
        long lastUsed = statComp.artefactCooldowns.getOrDefault(codec, 0L);
        long finalCooldown = getCooldownDuration(codec, statComp.getAmount(codec));

        if (now - lastUsed >= finalCooldown) {
            damage.setCancelled(true);
            statComp.artefactCooldowns.put(codec, now);

            EffectControllerComponent effectController = cmds.getComponent(targetRef, EffectControllerComponent.getComponentType());

            String breakEffectName = codec.getLogicString("breakEffect", "Artefact_Shield_Break");
            EntityEffect breakEffect = EntityEffect.getAssetMap().getAsset(breakEffectName);

            if (effectController != null && breakEffect != null) {
                effectController.addEffect(targetRef, breakEffect, cmds);
            }
            ShieldHud hud = getOrInitShieldHud(statComp, targetRef, cmds);
            if (hud != null) {
                hud.displayShield(false);
            }
        }
    }
}