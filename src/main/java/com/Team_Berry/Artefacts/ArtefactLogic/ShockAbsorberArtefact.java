package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnDealPreDamage;
import com.Team_Berry.Artefacts.Interfaces.IOnTakePreDamage;
import com.Team_Berry.Artefacts.Interfaces.IOnTick;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector4d;

public class ShockAbsorberArtefact implements IOnTakePreDamage, IOnDealPreDamage, IOnTick {

    @Override
    public void onTakePreDamage(ArtefactCodec codec, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {

        DamageDataComponent damageData = cmds.getComponent(targetRef, DamageDataComponent.getComponentType());
        if (damageData == null || damageData.getCurrentWielding() == null) {
            return;
        }

        int stacks = statComp.getAmount(codec);
        if (stacks <= 0) {
            return;
        }
        Boolean isBlocked = damage.getMetaObject(Damage.BLOCKED);
        if (isBlocked == null || !isBlocked) {
            return;
        }

        float baseAbsorb = codec.getLogicNumber("baseAbsorb", 0.50f);
        float extraPerStack = codec.getLogicNumber("extraAbsorbPerStack", 0.10f);
        float absorbPercent = Math.min(1.0f, baseAbsorb + (extraPerStack * (stacks - 1)));

        float damageToStore = damage.getInitialAmount() * absorbPercent;

        float currentStored = (float) statComp.customArtefactData.getOrDefault("shock_absorber_stored", 0.0f);
        float newStored = currentStored + damageToStore;

        if (newStored > currentStored) {
            String absorbSound = codec.getLogicString("absorbSound", "");
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

            Vector4d hitLoc = damage.getMetaObject(Damage.HIT_LOCATION);
            if (hitLoc != null) {
                Vector3d impactPos = new Vector3d(hitLoc.x, hitLoc.y, hitLoc.z);

                float yaw = 0.0f;
                float pitch = 0.0f;
                if (damage.getSource() instanceof Damage.EntitySource) {
                    Ref<EntityStore> attackerRefSrc = ((Damage.EntitySource) damage.getSource()).getRef();
                    TransformComponent attackerTransform = cmds.getComponent(attackerRefSrc, TransformComponent.getComponentType());
                    if (attackerTransform != null) {
                        Vector3d aPos = attackerTransform.getPosition();
                        Vector3d dir = new Vector3d(aPos.x - impactPos.x, aPos.y - impactPos.y, aPos.z - impactPos.z);
                        yaw = (float) Math.atan2(dir.x, dir.z);
                        pitch = (float) Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z));
                    }
                }

                String absorbParticle = codec.getLogicString("absorbParticle", "");
                String absorbColor = codec.getLogicString("absorbColor", "");

                spawnColoredParticle(absorbParticle, absorbColor, impactPos, yaw, pitch, 0.0f, cmds);
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

            String hitParticle = codec.getLogicString("hitParticle", "");
            String hitColor = codec.getLogicString("hitColor", "");

            if (!hitParticle.isEmpty() && victimTransform != null && attackerTransform != null) {
                Vector4d hitLoc = damage.getMetaObject(Damage.HIT_LOCATION);
                Vector3d particlePos;

                if (hitLoc != null) {
                    particlePos = new Vector3d(hitLoc.x, hitLoc.y, hitLoc.z);
                } else {
                    Vector3d vPos = victimTransform.getPosition();
                    particlePos = new Vector3d(vPos.x, vPos.y + 1.0, vPos.z);
                }

                Vector3d aPos = attackerTransform.getPosition();
                Vector3d dir = new Vector3d(particlePos.x - aPos.x, particlePos.y - aPos.y, particlePos.z - aPos.z);

                float yaw = (float) Math.atan2(dir.x, dir.z);
                float pitch = (float) Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z));

                spawnColoredParticle(hitParticle, hitColor, particlePos, yaw, pitch, 0.0f, cmds);
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
            if (newStored <= 0.0f) {
                statComp.customArtefactData.put("shock_absorber_idle_time", 0.0f);
            }
        }
    }

    private void spawnColoredParticle(String particleName, String hexColor, Vector3d pos, float yaw, float pitch, float roll, CommandBuffer<EntityStore> cmds) {
        if (particleName == null || particleName.isEmpty()) return;

        com.hypixel.hytale.protocol.Color pColor = null;
        if (hexColor != null && hexColor.length() >= 6) {
            String cleanHex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
            try {
                byte r = (byte) Integer.parseInt(cleanHex.substring(0, 2), 16);
                byte g = (byte) Integer.parseInt(cleanHex.substring(2, 4), 16);
                byte b = (byte) Integer.parseInt(cleanHex.substring(4, 6), 16);
                pColor = new Color(r, g, b);
            } catch (Exception ignored) {
            }
        }

        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource =
                (SpatialResource) cmds.getResource(EntityModule.get().getPlayerSpatialResourceType());

        java.util.List<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
        playerSpatialResource.getSpatialStructure().collect(pos, 75.0, playerRefs);

        ParticleUtil.spawnParticleEffect(
                particleName,
                pos.x, pos.y, pos.z,
                yaw, pitch, roll,
                1.0f,
                pColor,
                null,
                playerRefs,
                cmds
        );
    }
}