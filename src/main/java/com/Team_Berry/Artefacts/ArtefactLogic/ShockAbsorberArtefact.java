package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnDealPreDamage;
import com.Team_Berry.Artefacts.Interfaces.IOnTakePreDamage;
import com.Team_Berry.Artefacts.Interfaces.IOnTick;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ShockAbsorberArtefact implements IOnTakePreDamage, IOnDealPreDamage, IOnTick {

    @Override
    public void onTakePreDamage(ArtefactCodec codec, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {

        DamageDataComponent damageData = cmds.getComponent(targetRef, DamageDataComponent.getComponentType());
        if (damageData == null || damageData.getCurrentWielding() == null) return;

        int stacks = statComp.getAmount(codec);
        if (stacks <= 0) return;

        float baseAbsorb = codec.getLogicNumber("baseAbsorb", 0.50f);
        float extraPerStack = codec.getLogicNumber("extraAbsorbPerStack", 0.10f);

        float absorbPercent = Math.min(1.0f, baseAbsorb + (extraPerStack * (stacks - 1)));
        float damageToStore = damage.getAmount() * absorbPercent;
        float currentStored = (float) statComp.customArtefactData.getOrDefault("shock_absorber_stored", 0.0f);

        float newStored = currentStored + damageToStore;

        if (newStored > currentStored) {
            String absorbSound = codec.getLogicString("absorbSound", "SFX_Shield_Block_Default");
            if (!absorbSound.isEmpty()) {
                int soundIndex = SoundEvent.getAssetMap().getIndex(absorbSound);
                if (soundIndex != Integer.MIN_VALUE && soundIndex != 0) {
                    TransformComponent transform = cmds.getComponent(targetRef, TransformComponent.getComponentType());
                    if (transform != null) {
                        Vector3d pos = transform.getPosition();
                        SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, pos.x, pos.y, pos.z, targetRef.getStore());
                    }
                }
            }

            String absorbEffectName = codec.getLogicString("absorbEffect", "");
            if (!absorbEffectName.isEmpty()) {
                EffectControllerComponent effectController = cmds.getComponent(targetRef, EffectControllerComponent.getComponentType());
                EntityEffect effect = EntityEffect.getAssetMap().getAsset(absorbEffectName);
                if (effectController != null && effect != null) {
                    effectController.addEffect(targetRef, effect, cmds);
                }
            }
        }

        statComp.customArtefactData.put("shock_absorber_stored", newStored);
        statComp.customArtefactData.put("shock_absorber_idle_time", 0.0f);
    }

    @Override
    public void onDealPreDamage(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {

        float storedDamage = (float) statComp.customArtefactData.getOrDefault("shock_absorber_stored", 0.0f);

        if (storedDamage > 0.0f) {
            damage.setAmount(damage.getAmount() + storedDamage);

            TransformComponent attackerTransform = cmds.getComponent(attackerRef, TransformComponent.getComponentType());
            TransformComponent victimTransform = cmds.getComponent(targetRef, TransformComponent.getComponentType());

            if (attackerTransform != null && victimTransform != null) {
                Vector3d aPos = attackerTransform.getPosition();
                Vector3d vPos = victimTransform.getPosition();

                Vector3d kbDir = new Vector3d(vPos.x - aPos.x, vPos.y - aPos.y, vPos.z - aPos.z);
                double len = Math.sqrt(kbDir.x * kbDir.x + kbDir.y * kbDir.y + kbDir.z * kbDir.z);
                if (len > 0.01) {
                    kbDir.x /= len;
                    kbDir.y /= len;
                    kbDir.z /= len;
                }


                float minForce = codec.getLogicNumber("minKnockbackForce", 18.0f);
                float maxForce = codec.getLogicNumber("maxKnockbackForce", 80.0f);
                float kbMultiplier = codec.getLogicNumber("knockbackMultiplier", 0.20f);

                double rawForce = minForce + (storedDamage * kbMultiplier);
                double force = Math.min(rawForce, maxForce);

                kbDir.x *= force;
                kbDir.z *= force;

                kbDir.y = 1.0 + (force * 0.12);

                Store<EntityStore> store = targetRef.getStore();
                World world = store.getExternalData().getWorld();
                if (world != null) {
                    world.execute(() -> {
                        KnockbackComponent existingKb = store.getComponent(targetRef, KnockbackComponent.getComponentType());
                        if (existingKb != null) {
                            Vector3d currentVel = existingKb.getVelocity();
                            currentVel.x += kbDir.x;
                            currentVel.y += kbDir.y;
                            currentVel.z += kbDir.z;
                            existingKb.setVelocity(currentVel);
                        } else {
                            KnockbackComponent newKb = new KnockbackComponent();
                            newKb.setVelocity(kbDir);
                            store.addComponent(targetRef, KnockbackComponent.getComponentType(), newKb);
                        }
                    });
                }
            }

            statComp.customArtefactData.put("shock_absorber_stored", 0.0f);
            statComp.customArtefactData.put("shock_absorber_idle_time", 0.0f);

            String hitSound = codec.getLogicString("hitSound", "SFX_Mace_T2_Signature_Impact");
            if (!hitSound.isEmpty() && victimTransform != null) {
                int soundIndex = SoundEvent.getAssetMap().getIndex(hitSound);
                if (soundIndex != Integer.MIN_VALUE && soundIndex != 0) {
                    Vector3d pos = victimTransform.getPosition();
                    SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, pos.x, pos.y, pos.z, targetRef.getStore());
                }
            }

            String hitParticle = codec.getLogicString("hitParticle", "Explosion_Small");
            if (!hitParticle.isEmpty() && victimTransform != null) {
                Vector3d pos = victimTransform.getPosition();
                ParticleUtil.spawnParticleEffect(hitParticle, pos, cmds);
            }
        }
    }

    @Override
    public void onTick(ArtefactCodec codec, int stacks, Ref<EntityStore> playerRef, StatEffectComponent statComp, Store<EntityStore> store, float dt) {

        float storedDamage = (float) statComp.customArtefactData.getOrDefault("shock_absorber_stored", 0.0f);
        if (storedDamage <= 0.0f) return;

        float deltaSeconds = dt > 1.0f ? dt / 1000.0f : dt;

        float idleTime = (float) statComp.customArtefactData.getOrDefault("shock_absorber_idle_time", 0.0f);
        idleTime += deltaSeconds;
        statComp.customArtefactData.put("shock_absorber_idle_time", idleTime);

        float displayValue = storedDamage;

        float waitDuration = codec.getLogicNumber("shockHoldDuration", 5.0f);

        if (idleTime >= waitDuration) {
            float decayPerSecond = codec.getLogicNumber("shockDrainPerSecond", 5.0f);
            float newStored = Math.max(0.0f, storedDamage - (decayPerSecond * deltaSeconds));
            statComp.customArtefactData.put("shock_absorber_stored", newStored);
            displayValue = newStored;

            if (newStored <= 0.0f) {
                statComp.customArtefactData.put("shock_absorber_idle_time", 0.0f);
            }
        }

        PlayerRef pRef = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (pRef != null) {
            pRef.sendMessage(Message.raw(String.format("Shock Sponge Stored: %.1f", displayValue)));
        }
    }
}