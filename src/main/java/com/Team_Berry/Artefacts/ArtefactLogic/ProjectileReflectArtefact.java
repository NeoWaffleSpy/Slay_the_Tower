package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnTakePreDamage;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;

public class ProjectileReflectArtefact implements IOnTakePreDamage {

    @Override
    public void onTakePreDamage(ArtefactCodec codec, Ref<EntityStore> targetRef, Damage damage, StatEffectComponent statComp, CommandBuffer<EntityStore> cmds) {
        Damage.Source source = damage.getSource();

        if (source instanceof Damage.ProjectileSource projectileSource) {
            int projectileCauseIndex = -1;
            DamageCause projectileDamageCause = DamageCause.getAssetMap().getAsset("PROJECTILE");
            if (projectileDamageCause != null) {
                projectileCauseIndex = DamageCause.getAssetMap().getIndex(projectileDamageCause.getId());
            }

            if (projectileCauseIndex != -1 && damage.getDamageCauseIndex() == projectileCauseIndex) {
                handleProjectileReflection(targetRef, cmds, projectileSource, damage);
            }
        }
    }

    private void handleProjectileReflection(Ref<EntityStore> targetRef, CommandBuffer<EntityStore> commandBuffer, Damage.ProjectileSource projectileSource, Damage damage) {
        DamageDataComponent damageData = commandBuffer.getComponent(targetRef, DamageDataComponent.getComponentType());
        if (damageData == null || damageData.getCurrentWielding() == null) return;

        Ref<EntityStore> oldProjectileRef = projectileSource.getProjectile();
        if (!oldProjectileRef.isValid()) return;

        TransformComponent oldTransform = commandBuffer.getComponent(oldProjectileRef, TransformComponent.getComponentType());
        ProjectileComponent oldProj = commandBuffer.getComponent(oldProjectileRef, ProjectileComponent.getComponentType());

        if (oldTransform != null && oldProj != null) {
            createReflectedProjectile(targetRef, oldProjectileRef, oldTransform, oldProj, commandBuffer);
            commandBuffer.removeEntity(oldProjectileRef, RemoveReason.REMOVE);
        }
    }

    private void createReflectedProjectile(Ref<EntityStore> targetRef, Ref<EntityStore> oldRef, TransformComponent oldTransform, ProjectileComponent oldProj, CommandBuffer<EntityStore> commandBuffer) {
        String assetName = oldProj.getProjectileAssetName();
        if (assetName == null || assetName.isEmpty()) return;

        float bonusReflectDamage = 0;
        float reflectMult = 1;

        EntityStatMap playerStats = commandBuffer.getComponent(targetRef, EntityStatsModule.get().getEntityStatMapComponentType());
        EntityStatType reflectStatType = StatCodec.getStatFromString("Bonus_Projectile_Reflect_Damage");

        if (playerStats != null && reflectStatType != null) {
            int statIndex = ArtefactPlugin.getEntityStatTypeAssetStore().getIndex(reflectStatType.getId());
            Map<String, Modifier> modMap = playerStats.get(statIndex).getModifiers();
            if (modMap != null) {
                for (Modifier mod : modMap.values()) {
                    StaticModifier modifier = (StaticModifier) mod;
                    if (modifier.getCalculationType() == StaticModifier.CalculationType.ADDITIVE)
                        bonusReflectDamage += modifier.getAmount();
                    else if (modifier.getCalculationType() == StaticModifier.CalculationType.MULTIPLICATIVE)
                        reflectMult += modifier.getAmount();
                }
            }
        }

        Vector3f oldRot = oldTransform.getRotation();
        float newYaw = oldRot.getYaw() + (float) Math.PI;
        float newPitch = -oldRot.getPitch();
        Vector3f newRotation = new Vector3f(oldRot.getX(), newYaw, newPitch);

        Vector3d direction = new Vector3d();
        PhysicsMath.vectorFromAngles(newYaw, newPitch, direction);

        Vector3d spawnPos = oldTransform.getPosition().clone().addScaled(direction, 0.5);

        TimeResource time = commandBuffer.getResource(TimeResource.getResourceType());
        Holder<EntityStore> holder = ProjectileComponent.assembleDefaultProjectile(time, assetName, spawnPos, newRotation);
        ProjectileComponent newProjComp = holder.getComponent(ProjectileComponent.getComponentType());

        if (newProjComp != null) {
            if (newProjComp.getProjectile() == null) newProjComp.initialize();

            float baseDamage = (float) newProjComp.getProjectile().getDamage();
            float finalReflectDamage = (baseDamage + bonusReflectDamage) * reflectMult;

            if (baseDamage > 0) {
                float multiplier = finalReflectDamage / baseDamage;
                newProjComp.applyBrokenPenalty(1.0f - multiplier);
            }

            holder.ensureComponent(com.hypixel.hytale.server.core.modules.entity.component.Intangible.getComponentType());

            UUIDComponent playerUuidComp = commandBuffer.getComponent(targetRef, UUIDComponent.getComponentType());
            if (playerUuidComp != null) {
                newProjComp.shoot(holder, playerUuidComp.getUuid(), spawnPos.x, spawnPos.y, spawnPos.z, newYaw, newPitch);
            }
        }

        commandBuffer.addEntity(holder, AddReason.SPAWN);
    }
}