package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnDealPostDamage;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BleedArtefact implements IOnDealPostDamage {

    @Override
    public void onDealPostDamage(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {
        if (damage.getAmount() <= 0) return;

        int stacks = statComp.getAmount(codec);
        if (stacks <= 0) return;

        double chance = stacks * 0.10;

        if (Math.random() < chance) {
            EffectControllerComponent effectController = cmds.getComponent(targetRef, EffectControllerComponent.getComponentType());
            EntityEffect bleedEffect = EntityEffect.getAssetMap().getAsset("Artefact_Bleed_Damage_Flat");

            if (effectController != null && bleedEffect != null) {
                effectController.addEffect(targetRef, bleedEffect, cmds);
            }
        }
    }
}