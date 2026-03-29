package com.Team_Berry.WeaponInteraction.Systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SlowBombDamageSystem extends DamageEventSystem {
    static String SLOW_BOMB_ASSET = "Slow_Bomb_Projectile";
    static String SLOW_BOMB_EFFECT = "Slow_Bomb_Effect";

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull Damage damage) {
        Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(i);
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EntitySource entitySource) {

            Ref<EntityStore> attackerRef = entitySource.getRef();
            PlayerRef attackerPlayer = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
            if (attackerPlayer != null && source instanceof Damage.ProjectileSource projectileSource) {

                Ref<EntityStore> projectileRef = projectileSource.getProjectile();
                ProjectileComponent projectileComponent = commandBuffer.getComponent(projectileRef, ProjectileComponent.getComponentType());

                addEffect(store, commandBuffer, projectileComponent, targetRef, attackerRef);
            }
        }
    }

    private void addEffect(@NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, ProjectileComponent projectileComponent, Ref<EntityStore> targetRef, Ref<EntityStore> attackerRef) {
        if (projectileComponent != null && SLOW_BOMB_ASSET.equals(projectileComponent.getProjectileAssetName())) {
            EffectControllerComponent effectController = store.getComponent(targetRef, EffectControllerComponent.getComponentType());
            EntityEffect effect = EntityEffect.getAssetMap().getAsset(SLOW_BOMB_EFFECT);

            if (targetRef != attackerRef) {
                effectController.addEffect(targetRef, effect, commandBuffer);
            }
        }
    }


}
