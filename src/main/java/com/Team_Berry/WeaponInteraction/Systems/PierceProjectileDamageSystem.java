package com.Team_Berry.WeaponInteraction.Systems;

import com.Team_Berry.WeaponInteraction.Component.PiercedCooldownComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.physics.SimplePhysicsProvider;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;
import org.jetbrains.annotations.NotNull;

public class PierceProjectileDamageSystem extends DamageEventSystem {
    private static final ComponentType<EntityStore, Velocity> VELOCITY_TYPE = Velocity.getComponentType();
    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_TYPE = TransformComponent.getComponentType();
    private static final ComponentType<EntityStore, UUIDComponent> UUID_TYPE = UUIDComponent.getComponentType();
    private static final ComponentType<EntityStore, NetworkId> NETWORK_ID_TYPE = NetworkId.getComponentType();
    private static final ComponentType<EntityStore, ProjectileComponent> PROJECTILE_TYPE = ProjectileComponent.getComponentType();
    private static final ComponentType<EntityStore, BoundingBox> BOX_TYPE = BoundingBox.getComponentType();

    private final ComponentType<EntityStore, PiercedCooldownComponent> piercedCooldownComponentType;


    static String PIERCE_PROJECTILE_ASSET_NAME = "Piercing_Arrow";
    static long PIERCE_COOLDOWN_MS = 100;

    public PierceProjectileDamageSystem(ComponentType<EntityStore, PiercedCooldownComponent> piercedCooldownComponentType) {
        this.piercedCooldownComponentType = piercedCooldownComponentType;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }
    @Override
    public void handle(int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull Damage damage) {
        Damage.Source source = damage.getSource();
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        if (!(source instanceof Damage.ProjectileSource projectileSource)) return;

        Ref<EntityStore> oldProjectileRef = projectileSource.getProjectile();
        if (oldProjectileRef == null || !oldProjectileRef.isValid()) return;

        ProjectileComponent projComp = commandBuffer.getComponent(oldProjectileRef, PROJECTILE_TYPE);
        if (projComp != null && PIERCE_PROJECTILE_ASSET_NAME.equals(projComp.getProjectileAssetName())) {

            long now = commandBuffer.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();
            PiercedCooldownComponent cooldown = commandBuffer.getComponent(ref, piercedCooldownComponentType);
            if (cooldown != null && (now - cooldown.getLastPierceTime()) < PIERCE_COOLDOWN_MS) {
                damage.setCancelled(true);
            }
            TransformComponent transform = commandBuffer.getComponent(oldProjectileRef, TRANSFORM_TYPE);
            if (transform != null) {
                if (cooldown == null ) {
                    cooldown = new PiercedCooldownComponent();
                    cooldown.setLastPierceTime(commandBuffer.getResource(TimeResource.getResourceType()).getNow().toEpochMilli());

                    commandBuffer.addComponent(ref, piercedCooldownComponentType, cooldown);
                } else {
                    cooldown.setLastPierceTime(commandBuffer.getResource(TimeResource.getResourceType()).getNow().toEpochMilli());

                }

                createPiercingProjectile(oldProjectileRef, transform, projComp, commandBuffer);
                commandBuffer.removeEntity(oldProjectileRef, RemoveReason.REMOVE);


            }
        }
    }

    private void createPiercingProjectile(Ref<EntityStore> oldRef, TransformComponent oldTransform, ProjectileComponent oldProj, CommandBuffer<EntityStore> commandBuffer) {
        Archetype<EntityStore> oldArchetype = commandBuffer.getArchetype(oldRef);
        Component<EntityStore>[] components = new Component[oldArchetype.length()];

        Vector3d direction = new Vector3d();
        Velocity oldVelComp = commandBuffer.getComponent(oldRef, VELOCITY_TYPE);

        if (oldVelComp != null && oldVelComp.getVelocity().squaredLength() > 0.001) {
            direction.assign(oldVelComp.getVelocity()).normalize();
        } else {
            PhysicsMath.vectorFromAngles(oldTransform.getRotation().getYaw(), oldTransform.getRotation().getPitch(), direction);
        }

        double currentSpeed = (oldProj.getProjectile() != null) ? oldProj.getProjectile().getMuzzleVelocity() : 20.0;
        Vector3d nudgedPos = oldTransform.getPosition().clone().addScaled(direction, 2);

        for (int i = oldArchetype.getMinIndex(); i < oldArchetype.length(); i++) {
            ComponentType<EntityStore, ?> type = oldArchetype.get(i);
            if (type == null) continue;

            if (type.equals(UUID_TYPE)) {
                components[i] = UUIDComponent.randomUUID();
            } else if (type.equals(NETWORK_ID_TYPE)) {
                components[i] = new NetworkId(commandBuffer.getStore().getExternalData().takeNextNetworkId());
            } else if (type.equals(VELOCITY_TYPE)) {
                components[i] = new Velocity(direction.clone().scale(currentSpeed));
            } else if (type.equals(TRANSFORM_TYPE)) {
                components[i] = new TransformComponent(nudgedPos, oldTransform.getRotation());
            } else if (type.equals(PROJECTILE_TYPE)) {
                ProjectileComponent newProjectile = new ProjectileComponent(oldProj);
                resetProjectilePhysics(oldRef, commandBuffer, newProjectile, direction, currentSpeed);
                components[i] = newProjectile;
            } else {
                components[i] = commandBuffer.getComponent(oldRef, type);
            }
        }

        Holder<EntityStore> holder = commandBuffer.getStore().getRegistry().newHolder(oldArchetype, components);
        commandBuffer.addEntity(holder, AddReason.SPAWN);
    }

    private void resetProjectilePhysics(Ref<EntityStore> oldRef, CommandBuffer<EntityStore> commandBuffer, ProjectileComponent newProjectile, Vector3d direction, double currentSpeed) {
        SimplePhysicsProvider spp = newProjectile.getSimplePhysicsProvider();
        if (spp != null) {
            spp.setImpacted(false);
            spp.setResting(false);
            spp.setVelocity(direction.clone().scale(currentSpeed));
            BoundingBox box = commandBuffer.getComponent(oldRef, BOX_TYPE);
            if (box != null && newProjectile.getProjectile() != null) {
                spp.initialize(newProjectile.getProjectile(), box);
            }
        }
    }
}