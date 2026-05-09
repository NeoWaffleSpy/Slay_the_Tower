package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnDealPostDamage;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OverheatArtefact implements IOnDealPostDamage {

    private static final KeyedScheduler overheatScheduler = new KeyedScheduler();

    @Override
    public void onDealPostDamage(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {
        World world = targetRef.getStore().getExternalData().getWorld();

        int maxStages = 0;
        while (codec.logicStrings != null && codec.logicStrings.containsKey("overheatEffect" + (maxStages + 1))) {
            maxStages++;
        }

        if (maxStages == 0) return;

        final int finalMaxStages = maxStages;
        String taskKey = "Overheat_" + targetRef.toString();

        overheatScheduler.schedule(taskKey, () -> {
            world.execute(() -> {

                EffectControllerComponent effectController = targetRef.getStore().getComponent(targetRef, EffectControllerComponent.getComponentType());
                if (effectController == null) return;

                String postEffectName = codec.getLogicString("postOverheatEffect", "");
                if (!postEffectName.isEmpty()) {
                    EntityEffect postEffect = EntityEffect.getAssetMap().getAsset(postEffectName);
                    if (postEffect != null && effectController.hasEffect(postEffect)) {
                        return;
                    }
                }

                EntityEffect redFlashEffect = EntityEffect.getAssetMap().getAsset("Red_Flash");
                if (redFlashEffect != null && effectController.hasEffect(redFlashEffect)) {
                    int redFlashIndex = EntityEffect.getAssetMap().getIndex(redFlashEffect.getId());
                    effectController.removeEffect(targetRef, redFlashIndex, targetRef.getStore());
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

                if (currentStage > 0) {
                    String oldEffectName = codec.getLogicString("overheatEffect" + currentStage, "");
                    EntityEffect oldEffect = EntityEffect.getAssetMap().getAsset(oldEffectName);
                    if (oldEffect != null) {
                        int oldIndex = EntityEffect.getAssetMap().getIndex(oldEffect.getId());
                        effectController.removeEffect(targetRef, oldIndex, targetRef.getStore());
                    }
                }

                if (currentStage < finalMaxStages) {
                    int nextStage = currentStage + 1;
                    String newEffectName = codec.getLogicString("overheatEffect" + nextStage, "");
                    EntityEffect newEffect = EntityEffect.getAssetMap().getAsset(newEffectName);
                    if (newEffect != null) {
                        effectController.addEffect(targetRef, newEffect, targetRef.getStore());
                    }
                } else {
                    String maxEffectName = codec.getLogicString("overheatEffect" + finalMaxStages, "");
                    EntityEffect maxEffect = EntityEffect.getAssetMap().getAsset(maxEffectName);
                    if (maxEffect != null) {
                        int maxIndex = EntityEffect.getAssetMap().getIndex(maxEffect.getId());
                        effectController.removeEffect(targetRef, maxIndex, targetRef.getStore());
                    }

                    if (!postEffectName.isEmpty()) {
                        EntityEffect postEffect = EntityEffect.getAssetMap().getAsset(postEffectName);
                        if (postEffect != null) {
                            effectController.addEffect(targetRef, postEffect, targetRef.getStore());
                        }
                    }

                    triggerOverheatExplosion(codec, attackerRef, targetRef, cmds);
                }
            });

        }, 10, TimeUnit.MILLISECONDS);
    }

    private void triggerOverheatExplosion(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> targetRef, CommandBuffer<EntityStore> cmds) {
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

        float finalExplosionDamage = (BASE_DAMAGE + bonusExplosionDamage) * explosionMult;

        World world = targetRef.getStore().getExternalData().getWorld();
        ComponentAccessor<ChunkStore> chunkStore = world.getChunkStore().getStore();

        ExplosionConfig config = new OverheatExplosionConfig(explosionRadius, finalExplosionDamage);


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