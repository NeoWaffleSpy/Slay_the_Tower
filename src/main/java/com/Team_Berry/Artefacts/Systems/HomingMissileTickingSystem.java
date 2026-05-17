package com.Team_Berry.Artefacts.Systems;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Components.HomingMissileComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider.STATE;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.util.List;

public class HomingMissileTickingSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                ArtefactPlugin.getHomingMissileComponentType(),
                TransformComponent.getComponentType()
        );
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> cb) {
        Ref<EntityStore> missileRef = chunk.getReferenceTo(index);
        HomingMissileComponent missile = (HomingMissileComponent) chunk.getComponent(index, ArtefactPlugin.getHomingMissileComponentType());
        TransformComponent tc = (TransformComponent) chunk.getComponent(index, TransformComponent.getComponentType());

        if (missile == null || tc == null) return;

        if (missile.isDying) {
            missile.deathTimer -= dt;

            missile.vy -= missile.gravity * dt;
            missile.px += missile.vx * dt;
            missile.py += missile.vy * dt;
            missile.pz += missile.vz * dt;
            tc.getPosition().assign(missile.px, missile.py, missile.pz);


            if (missile.deathTimer <= 0.0f) {
                triggerExplosion(missile, missileRef, store, cb);
            }
            return;
        }

        StandardPhysicsProvider spp = (StandardPhysicsProvider) store.getComponent(missileRef, StandardPhysicsProvider.getComponentType());
        if (spp != null && spp.getState() != STATE.INACTIVE) {
            spp.setState(STATE.INACTIVE);
        }

        if (!missile.reachedApex) {
            missile.vy -= missile.gravity * dt;

            if (missile.vy <= 0.0) {
                missile.reachedApex = true;
                missile.target = findNearestEnemy(missile.px, missile.py, missile.pz, store, 50.0);
            }
        } else {
            if (missile.target != null && missile.target.isValid() && store.getComponent(missile.target, DeathComponent.getComponentType()) == null) {
                TransformComponent ttc = (TransformComponent) store.getComponent(missile.target, TransformComponent.getComponentType());
                if (ttc != null) {
                    double tx = ttc.getPosition().x;
                    double tz = ttc.getPosition().z;
                    double ty = ttc.getPosition().y + 1.2;

                    BoundingBox bb = (BoundingBox) store.getComponent(missile.target, BoundingBox.getComponentType());
                    if (bb != null) {
                        Box box = bb.getBoundingBox();
                        ty = ttc.getPosition().y + (box.getMin().y + box.getMax().y) * 0.5;
                    }

                    double dx = tx - missile.px;
                    double dy = ty - missile.py;
                    double dz = tz - missile.pz;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    if (dist < 1.5) {
                        DamageSystems.executeDamage(missile.target, cb, new Damage(new Damage.EntitySource(missile.ownerRef), DamageCause.PHYSICAL, missile.damage));
                        triggerExplosion(missile, missileRef, store, cb);
                        return;
                    }

                    if (dist > 0.1) {
                        double ndx = dx / dist;
                        double ndy = dy / dist;
                        double ndz = dz / dist;
                        double speed = Math.sqrt(missile.vx * missile.vx + missile.vy * missile.vy + missile.vz * missile.vz);
                        if (speed < 1.0) speed = missile.seekSpeed;

                        double cvx = missile.vx / speed;
                        double cvy = missile.vy / speed;
                        double cvz = missile.vz / speed;

                        double turn = missile.turnRate;
                        if (dist < 15.0) {
                            turn *= 1.0 + 3.0 * (1.0 - dist / 15.0);
                        }

                        double t = Math.min(1.0, turn * dt);
                        double nvx = cvx * (1.0 - t) + ndx * t;
                        double nvy = cvy * (1.0 - t) + ndy * t;
                        double nvz = cvz * (1.0 - t) + ndz * t;

                        double nlen = Math.sqrt(nvx * nvx + nvy * nvy + nvz * nvz);
                        if (nlen > 0.001) {
                            missile.vx = (nvx / nlen) * missile.seekSpeed;
                            missile.vy = (nvy / nlen) * missile.seekSpeed;
                            missile.vz = (nvz / nlen) * missile.seekSpeed;
                        }
                    }
                }
            } else {
                missile.isDying = true;
                missile.deathTimer = (float) (Math.random() * 0.5);
                return;
            }
        }

        missile.px += missile.vx * dt;
        missile.py += missile.vy * dt;
        missile.pz += missile.vz * dt;

        tc.getPosition().assign(missile.px, missile.py, missile.pz);

        double hSpeedSq = missile.vx * missile.vx + missile.vz * missile.vz;
        if (hSpeedSq > 1.0E-10) {
            float yaw = (float) Math.atan2(-missile.vx, -missile.vz);
            float pitch = (float) Math.atan2(missile.vy, Math.sqrt(hSpeedSq));
            tc.getRotation().setYaw(yaw);
            tc.getRotation().setPitch(pitch);
        }
    }

    private void triggerExplosion(HomingMissileComponent missile, Ref<EntityStore> missileRef, Store<EntityStore> store, CommandBuffer<EntityStore> cb) {
        if (missile.hitParticle != null && !missile.hitParticle.isEmpty()) {
            SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = (SpatialResource) store.getResource(EntityModule.get().getPlayerSpatialResourceType());
            List<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
            playerSpatialResource.getSpatialStructure().collect(new Vector3d(missile.px, missile.py, missile.pz), 75.0, playerRefs);

            ParticleUtil.spawnParticleEffect(
                    missile.hitParticle,
                    missile.px, missile.py, missile.pz,
                    0.0F, 0.0F, 0.0F,
                    missile.particleScale,
                    null,
                    null,
                    playerRefs,
                    store
            );
        }

        if (missile.hitSound != null && !missile.hitSound.isEmpty()) {
            int hitSoundIndex = SoundEvent.getAssetMap().getIndex(missile.hitSound);
            if (hitSoundIndex != 0) {
                SoundUtil.playSoundEvent3d(hitSoundIndex, SoundCategory.SFX, missile.px, missile.py, missile.pz, store);
            }
        }

        cb.removeEntity(missileRef, RemoveReason.REMOVE);
    }

    private Ref<EntityStore> findNearestEnemy(double x, double y, double z, Store<EntityStore> store, double maxRange) {
        Ref<EntityStore>[] bestTarget = new Ref[]{null};
        double[] bestSq = new double[]{maxRange * maxRange};

        store.forEachChunk(Query.and(NPCEntity.getComponentType(), TransformComponent.getComponentType()), (chunk, unused) -> {
            int size = chunk.size();
            for (int i = 0; i < size; ++i) {
                if (chunk.getComponent(i, DeathComponent.getComponentType()) == null) {
                    TransformComponent tc = (TransformComponent) chunk.getComponent(i, TransformComponent.getComponentType());
                    if (tc != null) {
                        double dx = tc.getPosition().x - x;
                        double dy = tc.getPosition().y - y;
                        double dz = tc.getPosition().z - z;
                        double sq = dx * dx + dy * dy + dz * dz;

                        if (sq < bestSq[0]) {
                            bestSq[0] = sq;
                            bestTarget[0] = chunk.getReferenceTo(i);
                        }
                    }
                }
            }
        });

        return bestTarget[0];
    }
}