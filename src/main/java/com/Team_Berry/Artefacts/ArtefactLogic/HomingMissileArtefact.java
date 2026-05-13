package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.HomingMissileComponent;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnKill;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HomingMissileArtefact implements IOnKill {

    @Override
    public void onKill(ArtefactCodec codec, Ref<EntityStore> attackerRef, Ref<EntityStore> deadEntityRef, StatEffectComponent statComp, Store<EntityStore> store, CommandBuffer<EntityStore> cmds) {

        int missileCount = statComp.getAmount(codec);
        if (missileCount <= 0) return;

        TransformComponent deadTransform = store.getComponent(deadEntityRef, TransformComponent.getComponentType());
        if (deadTransform == null) return;

        Vector3d spawnPos = new Vector3d(
                deadTransform.getPosition().x,
                deadTransform.getPosition().y + 1.0,
                deadTransform.getPosition().z
        );

        String projectileAssetName = codec.getLogicString("projectileAsset", "Projectile_Config_Default_Missile");
        ProjectileConfig config = (ProjectileConfig) ProjectileConfig.getAssetMap().getAsset(projectileAssetName);
        if (config == null) return;

        float missileDamage = codec.getLogicNumber("missileDamage", 10.0f);
        String hitParticle = codec.getLogicString("hitParticle", "Explosion_Small");
        String hitSound = codec.getLogicString("hitSound", "");
        String spawnSound = codec.getLogicString("spawnSound", "");
        float particleScale = codec.getLogicNumber("particleScale", 1.0f);

        if (!spawnSound.isEmpty()) {
            int spawnSoundIndex = SoundEvent.getAssetMap().getIndex(spawnSound);
            if (spawnSoundIndex != 0) {
                SoundUtil.playSoundEvent3d(spawnSoundIndex, SoundCategory.SFX, spawnPos.x, spawnPos.y, spawnPos.z, store);
            }
        }

        for (int i = 0; i < missileCount; i++) {

            double vx = (Math.random() - 0.5) * 12.0;
            double vz = (Math.random() - 0.5) * 12.0;
            double vy = 15.0 + (Math.random() * 10.0);

            Vector3d dir = new Vector3d(vx, vy, vz);
            double len = Math.sqrt(dir.x * dir.x + dir.y * dir.y + dir.z * dir.z);
            if (len > 0.01) {
                dir.x /= len;
                dir.y /= len;
                dir.z /= len;
            }

            Ref<EntityStore> projectileRef = ProjectileModule.get().spawnProjectile(attackerRef, cmds, config, spawnPos, dir);

            HomingMissileComponent missileData = new HomingMissileComponent(
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    vx, vy, vz, attackerRef, missileDamage, hitParticle, hitSound, particleScale
            );
            cmds.addComponent(projectileRef, ArtefactPlugin.getHomingMissileComponentType(), missileData);
        }
    }
}