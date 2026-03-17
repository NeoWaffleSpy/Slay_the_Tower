package com.Team_Berry.WeaponInteraction.Utils;

import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;

public class BleedEffectUtil {

    /**
     * Clears all potential bleed visual effects.
     * Works with both Store<EntityStore> and CommandBuffer<EntityStore>.
     */
    public static void clearAllVisual(Ref<EntityStore> target, EffectControllerComponent controller, Store<EntityStore> store) {
        for (BleedStage stage : BleedStage.values()) {
            if (stage == BleedStage.NONE) continue;

            String key = stage.getEffectKey();
            int index = EntityEffect.getAssetMap().getIndex(key);

            controller.removeEffect(target, index, store);
        }
    }
}