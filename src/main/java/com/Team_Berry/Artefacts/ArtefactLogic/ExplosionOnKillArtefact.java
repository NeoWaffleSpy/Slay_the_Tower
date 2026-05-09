package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnKill;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;

public class ExplosionOnKillArtefact implements IOnKill {

    @Override
    public void onKill(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> deadEntityRef, StatEffectComponent statComp, Store<EntityStore> store, CommandBuffer<EntityStore> cmds) {
        float EXPLOSION_RADIUS = 5.0f;
        float BASE_DAMAGE = 5.0f;
        float bonusExplosionDamage = 0;
        float explosionMult = 1;

        EntityStatMap playerStats = store.getComponent(attackerRef, EntityStatsModule.get().getEntityStatMapComponentType());
        EntityStatType explosionStatType = StatCodec.getStatFromString("Bonus_Explosion_Damage");

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
        TransformComponent deadTransform = store.getComponent(deadEntityRef, TransformComponent.getComponentType());
        if (deadTransform == null) return;
        Vector3d explosionPos = deadTransform.getPosition();

        World world = store.getExternalData().getWorld();
        ComponentAccessor<ChunkStore> chunkStore = world.getChunkStore().getStore();
        ExplosionConfig config = new ArtefactExplosionConfig(EXPLOSION_RADIUS, finalExplosionDamage);

        ParticleUtil.spawnParticleEffect("Explosion_Big", explosionPos, store);

        ExplosionUtils.performExplosion(
                new Damage.EntitySource(attackerRef),
                explosionPos,
                config,
                attackerRef,
                cmds,
                chunkStore
        );
    }

    public static class ArtefactExplosionConfig extends ExplosionConfig {
        public ArtefactExplosionConfig(float radius, float damage) {
            this.damageEntities = true;
            this.entityDamageRadius = radius;
            this.entityDamage = damage;
            this.entityDamageFalloff = 0.0f;
            this.damageBlocks = false;
            this.knockback = null;
        }
    }
}