package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnDealPreDamage;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OverheatArtefact implements IOnDealPreDamage {

    private static final KeyedScheduler overheatScheduler = new KeyedScheduler();

    private static void removeRedFlash(Ref<EntityStore> targetRef, CommandBuffer<EntityStore> cmds, EffectControllerComponent effectController) {
        EntityEffect redFlashEffect = EntityEffect.getAssetMap().getAsset("Red_Flash");
        if (redFlashEffect != null && effectController.hasEffect(redFlashEffect)) {
            int redFlashIndex = EntityEffect.getAssetMap().getIndex(redFlashEffect.getId());
            effectController.removeEffect(targetRef, redFlashIndex, cmds);
        }
    }

    @Override
    public void onDealPreDamage(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {

        if (!targetRef.isValid()) return;
        DeathComponent deathComp = cmds.getComponent(targetRef, DeathComponent.getComponentType());
        if (deathComp != null) return;

        String processedKey = "OverheatProcessed_" + targetRef;
        if (statComp.customArtefactData.containsKey(processedKey)) {
            return;
        }

        int maxStages = 0;
        while (codec.logicStrings != null && codec.logicStrings.containsKey("overheatEffect" + (maxStages + 1))) {
            maxStages++;
        }

        if (maxStages == 0) return;
        final int finalMaxStages = maxStages;

        EffectControllerComponent effectController = cmds.getComponent(targetRef, EffectControllerComponent.getComponentType());
        if (effectController == null) return;

        String postEffectName = codec.getLogicString("postOverheatEffect", "");
        if (!postEffectName.isEmpty()) {
            EntityEffect postEffect = EntityEffect.getAssetMap().getAsset(postEffectName);
            if (postEffect != null && effectController.hasEffect(postEffect)) {
                return;
            }
        }

        int currentStage = 0;
        for (int i = finalMaxStages; i >= 1; i--) {
            String effectName = codec.getLogicString("overheatEffect" + i, "");
            EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectName);
            if (effect != null && effectController.hasEffect(effect)) {
                currentStage = i;
                break;
            }
        }

        float currentHealth = 100.0f;
        EntityStatMap stats = cmds.getComponent(targetRef, EntityStatsModule.get().getEntityStatMapComponentType());

        if (stats != null) {
            EntityStatValue healthStat = stats.get(DefaultEntityStatTypes.getHealth());
            if (healthStat != null) {
                currentHealth = healthStat.get();
            }
        }

        float incomingDamage = damage.getAmount();
        boolean willDie = (currentHealth - incomingDamage) <= 0;
        boolean willExplode = willDie || (currentStage >= finalMaxStages);

        if (willExplode) {
            statComp.customArtefactData.put(processedKey, true);

            overheatScheduler.schedule("Cleanup_" + targetRef.toString() + "_" + System.currentTimeMillis(), () -> {
                statComp.customArtefactData.remove(processedKey);
            }, 100, TimeUnit.MILLISECONDS);
        }

        World world = targetRef.getStore().getExternalData().getWorld();

        if (willDie) {
            int assumedStage = Math.min(currentStage + 1, finalMaxStages);
            float damageMultiplier = (float) assumedStage / finalMaxStages;

            for (int i = 1; i <= finalMaxStages; i++) {
                String effectName = codec.getLogicString("overheatEffect" + i, "");
                EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectName);
                if (effect != null && effectController.hasEffect(effect)) {
                    int effectIndex = EntityEffect.getAssetMap().getIndex(effect.getId());
                    effectController.removeEffect(targetRef, effectIndex, cmds);
                }
            }

            triggerOverheatExplosion(codec, attackerRef, targetRef, cmds, damageMultiplier);
        } else {

            if (currentStage > 0) {
                String oldEffectName = codec.getLogicString("overheatEffect" + currentStage, "");
                EntityEffect oldEffect = EntityEffect.getAssetMap().getAsset(oldEffectName);
                if (oldEffect != null) {
                    int oldIndex = EntityEffect.getAssetMap().getIndex(oldEffect.getId());
                    effectController.removeEffect(targetRef, oldIndex, cmds);
                }
            }

            if (currentStage < finalMaxStages) {
                int nextStage = currentStage + 1;
                String newEffectName = codec.getLogicString("overheatEffect" + nextStage, "");
                EntityEffect newEffect = EntityEffect.getAssetMap().getAsset(newEffectName);

                if (newEffect != null) {
                    overheatScheduler.schedule("Visuals_" + targetRef.toString() + "_" + System.currentTimeMillis(), () -> {
                        if (targetRef.isValid()) {
                            removeRedFlash(targetRef, cmds, effectController);
                            EffectControllerComponent asyncEffectController = cmds.getComponent(targetRef, EffectControllerComponent.getComponentType());
                            if (asyncEffectController != null) {
                                asyncEffectController.addEffect(targetRef, newEffect, cmds);
                            }
                        }
                    }, 10, TimeUnit.MILLISECONDS);
                }
            } else {
                if (!postEffectName.isEmpty()) {
                    EntityEffect postEffect = EntityEffect.getAssetMap().getAsset(postEffectName);

                    if (postEffect != null) {
                        overheatScheduler.schedule("Charred_" + targetRef.toString() + "_" + System.currentTimeMillis(), () -> {
                            if (targetRef.isValid()) {
                                removeRedFlash(targetRef, cmds, effectController);
                                EffectControllerComponent asyncEffectController = cmds.getComponent(targetRef, EffectControllerComponent.getComponentType());
                                if (asyncEffectController != null) {
                                    asyncEffectController.addEffect(targetRef, postEffect, cmds);
                                }
                            }
                        }, 10, TimeUnit.MILLISECONDS);
                    }
                }
                triggerOverheatExplosion(codec, attackerRef, targetRef, cmds, 1.0f);
            }
        }
    }

    private void triggerOverheatExplosion(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> targetRef, CommandBuffer<EntityStore> cmds, float damageMultiplier) {
        TransformComponent targetTransform = cmds.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) return;

        Vector3d explosionPos = targetTransform.getPosition();

        float BASE_DAMAGE = codec.getLogicNumber("explosionDamage", 15.0f);
        float explosionRadius = codec.getLogicNumber("explosionRadius", 4.0f);

        float bonusExplosionDamage = 0;
        float explosionMult = 1;

        EntityStatMap playerStats = cmds.getComponent(attackerRef, EntityStatsModule.get().getEntityStatMapComponentType());
        EntityStatType explosionStatType = StatCodec.getStatFromString("Bonus_Overheat_Damage");

        if (playerStats != null && explosionStatType != null) {
            int statIndex = ArtefactPlugin.getEntityStatTypeAssetStore().getIndex(explosionStatType.getId());
            Map<String, Modifier> modMap = playerStats.get(statIndex).getModifiers();
            if (modMap != null) {
                for (Modifier mod : modMap.values()) {
                    StaticModifier modifier = (StaticModifier) mod;
                    if (modifier.getCalculationType() == StaticModifier.CalculationType.ADDITIVE)
                        bonusExplosionDamage += modifier.getAmount();
                    else if (modifier.getCalculationType() == StaticModifier.CalculationType.MULTIPLICATIVE)
                        explosionMult += modifier.getAmount();
                }
            }
        }

        float finalExplosionDamage = ((BASE_DAMAGE + bonusExplosionDamage) * explosionMult) * damageMultiplier;

        World world = targetRef.getStore().getExternalData().getWorld();
        ComponentAccessor<ChunkStore> chunkStore = world.getChunkStore().getStore();

        ExplosionConfig config = new OverheatExplosionConfig(explosionRadius, finalExplosionDamage);

        String particleName = codec.getLogicString("explosionParticle", "Explosion_Big");
        if (!particleName.isEmpty()) {
            Vector3d particlePos = new Vector3d(explosionPos.x, explosionPos.y + 1.0, explosionPos.z);
            float particleScale = codec.getLogicNumber("particleScale", 2.0f);

            SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = (SpatialResource) cmds.getResource(EntityModule.get().getPlayerSpatialResourceType());
            List<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
            playerSpatialResource.getSpatialStructure().collect(particlePos, 75.0, playerRefs);

            ParticleUtil.spawnParticleEffect(
                    particleName,
                    particlePos.x, particlePos.y, particlePos.z,
                    0.0F, 0.0F, 0.0F,
                    particleScale,
                    null,
                    null,
                    playerRefs,
                    cmds
            );
        }

        ExplosionUtils.performExplosion(
                new Damage.EntitySource(attackerRef),
                explosionPos,
                config,
                attackerRef,
                cmds,
                chunkStore
        );
    }

    public static class OverheatExplosionConfig extends ExplosionConfig {
        public OverheatExplosionConfig(float radius, float damage) {
            this.damageEntities = true;
            this.entityDamageRadius = radius;
            this.entityDamage = damage;
            this.entityDamageFalloff = 0.0f;
            this.damageBlocks = false;
            this.knockback = null;
        }
    }
}