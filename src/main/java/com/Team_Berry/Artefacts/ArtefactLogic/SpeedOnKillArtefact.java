package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnKill;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SpeedOnKillArtefact implements IOnKill {

    @Override
    public void onKill(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> deadEntityRef, StatEffectComponent statComp, Store<EntityStore> store, CommandBuffer<EntityStore> cmds) {
        int stacks = statComp.getAmount(codec);
        if (stacks <= 0) return;

        int cappedStacks = Math.min(stacks, 10);
        String effectName = "Speed_On_Kill_Effect" + cappedStacks;

        EffectControllerComponent effectController = store.getComponent(attackerRef, EffectControllerComponent.getComponentType());
        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectName);

        if (effectController != null && effect != null) {
            effectController.addEffect(attackerRef, effect, cmds);
        }
    }
}