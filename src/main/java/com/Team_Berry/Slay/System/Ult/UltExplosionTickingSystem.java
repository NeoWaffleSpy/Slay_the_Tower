package com.Team_Berry.Slay.System.Ult;

import com.Team_Berry.Slay.Component.Ult.UltExplosionComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class UltExplosionTickingSystem extends EntityTickingSystem<EntityStore> {
    private final ComponentType<EntityStore, UltExplosionComponent> ultExplosionComponentComponentType;

    public UltExplosionTickingSystem(ComponentType<EntityStore, UltExplosionComponent> ultExplosionComponentComponentType) {
        this.ultExplosionComponentComponentType = ultExplosionComponentComponentType;
    }


    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return this.ultExplosionComponentComponentType;
    }
    private static class MyUltExplosionConfig extends ExplosionConfig {
        public MyUltExplosionConfig() {
            this.damageEntities = true;
            this.damageBlocks = true;
            this.blockDamageRadius = 5;
            this.blockDamageFalloff = 1.0F;
            this.entityDamageRadius = 7.0F;
            this.entityDamage = 50.0F;
            this.entityDamageFalloff = 0F;
            this.blockDropChance = 1.0F;
            this.knockback = null;
            this.itemTool = null;
        }
    }

    @Override
    public void tick(float v, int i, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        UltExplosionComponent ultExplosionComponent = archetypeChunk.getComponent(i, ultExplosionComponentComponentType);
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        World world = store.getExternalData().getWorld();
        ComponentAccessor<ChunkStore> chunkStore = world.getChunkStore().getStore();
        long now = store.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();
        if (ultExplosionComponent.delayPassed(now)) {
            Vector3d explosionPosition = ultExplosionComponent.getExplosionPosition();
            commandBuffer.removeComponent(ref, ultExplosionComponentComponentType);
            ParticleUtil.spawnParticleEffect("Explosion_Medium", explosionPosition, store);
            ExplosionConfig config = new MyUltExplosionConfig();
            ExplosionUtils.performExplosion(new Damage.EntitySource(ref), explosionPosition, config, ref, commandBuffer, chunkStore);

        }
    }

}
