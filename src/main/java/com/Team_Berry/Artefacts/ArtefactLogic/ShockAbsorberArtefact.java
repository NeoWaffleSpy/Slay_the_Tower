package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnDealPreDamage;
import com.Team_Berry.Artefacts.Interfaces.IOnTakePreDamage;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ShockAbsorberArtefact implements IOnTakePreDamage, IOnDealPreDamage {

    @Override
    public void onTakePreDamage(ArtefactCodec codec, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {

        DamageDataComponent damageData = cmds.getComponent(targetRef, DamageDataComponent.getComponentType());
        if (damageData == null || damageData.getCurrentWielding() == null) return;

        int stacks = statComp.getAmount(codec);
        if (stacks <= 0) return;

        float absorbPercent = codec.getLogicNumber("absorbPercent", 0.50f) * stacks;
        float damageToStore = damage.getAmount() * absorbPercent;

        float currentStored = (float) statComp.customArtefactData.getOrDefault("shock_absorber_stored", 0.0f);
        float maxCap = codec.getLogicNumber("maxStoreCap", 50.0f) * stacks;

        float newStored = Math.min(currentStored + damageToStore, maxCap);

        if (newStored > currentStored) {
            String absorbSound = codec.getLogicString("absorbSound", "");
            if (!absorbSound.isEmpty()) {
                int soundIndex = SoundEvent.getAssetMap().getIndex(absorbSound);
                if (soundIndex != Integer.MIN_VALUE && soundIndex != 0) {
                    TransformComponent transform = cmds.getComponent(targetRef, TransformComponent.getComponentType());
                    if (transform != null) {
                        Vector3d pos = transform.getPosition();
                        //TODO PICK SOUND  SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, pos.x, pos.y, pos.z, targetRef.getStore());
                    }
                }
            }
        }

        statComp.customArtefactData.put("shock_absorber_stored", newStored);
    }

    @Override
    public void onDealPreDamage(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {

        float storedDamage = (float) statComp.customArtefactData.getOrDefault("shock_absorber_stored", 0.0f);

        if (storedDamage > 0.0f) {
            damage.setAmount(damage.getAmount() + storedDamage);
            statComp.customArtefactData.put("shock_absorber_stored", 0.0f);

            String hitSound = codec.getLogicString("hitSound", "SFX_Mace_T2_Signature_Impact");
            if (!hitSound.isEmpty()) {
                int soundIndex = SoundEvent.getAssetMap().getIndex(hitSound);
                if (soundIndex != Integer.MIN_VALUE && soundIndex != 0) {
                    TransformComponent transform = cmds.getComponent(targetRef, TransformComponent.getComponentType());
                    if (transform != null) {
                        Vector3d pos = transform.getPosition();
                        SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, pos.x, pos.y, pos.z, targetRef.getStore());
                    }
                }
            }
        }
    }
}