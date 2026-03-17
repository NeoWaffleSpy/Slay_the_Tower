package com.Team_Berry.WeaponInteraction.Systems;

import com.Team_Berry.WeaponInteraction.Component.BleedComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BleedTickingSystem extends EntityTickingSystem<EntityStore> {
    private final ComponentType<EntityStore, BleedComponent> bleedComponentType;
    private static final long TICK_INTERVAL = 1000L;

    public BleedTickingSystem(ComponentType<EntityStore, BleedComponent> bleedComponentType) {
        this.bleedComponentType = bleedComponentType;
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return this.bleedComponentType;
    }

    @Override
    public void tick(float dt, int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> bleedTarget = archetypeChunk.getReferenceTo(i);
        BleedComponent bleedComponent = archetypeChunk.getComponent(i, this.bleedComponentType);

        long now = store.getResource(TimeResource.getResourceType()).getNow().toEpochMilli();


        EffectControllerComponent effectController = commandBuffer.getComponent(bleedTarget, EffectControllerComponent.getComponentType());

        long deltaMs = (long) (dt * 1000);
        bleedComponent.setAccumulatedTime(bleedComponent.getAccumulatedTime() + deltaMs);
        if (bleedComponent.getAccumulatedTime() >= TICK_INTERVAL) {
            EntityEffect effect = EntityEffect.getAssetMap().getAsset("Bleed_Damage");
            OverlapBehavior overlap = effect.getOverlapBehavior();
            effectController.addEffect(bleedTarget, effect, commandBuffer);

            bleedComponent.setAccumulatedTime(bleedComponent.getAccumulatedTime() - TICK_INTERVAL);
        }
        if(bleedComponent.isExpired(now)){
            commandBuffer.removeComponent(bleedTarget,bleedComponentType);
            int effectIndex = EntityEffect.getAssetMap().getIndex("Bleed_Effect");
            effectController.removeEffect(bleedTarget, effectIndex, store);
        }
    }

}
