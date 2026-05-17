package com.Team_Berry.Artefacts.ArtefactLogic;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnTick;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class StaminaArtefact implements IOnTick {

    @Override
    public void onTick(ArtefactCodec codec, int stacks, Ref<EntityStore> playerRef, StatEffectComponent statComp, Store<EntityStore> store, float dt) {

        EntityStatMap statMap = store.getComponent(playerRef, EntityStatsModule.get().getEntityStatMapComponentType());
        if (statMap == null || statComp == null) return;

        MovementStatesComponent movementComp = store.getComponent(playerRef, MovementStatesComponent.getComponentType());
        if (movementComp != null) {
            if (movementComp.getMovementStates().sprinting || movementComp.getMovementStates().gliding) {
                statComp.customArtefactData.put("stamina_regen_accumulator", 0.0f);
                return;
            }
        }

        DamageDataComponent damageData = store.getComponent(playerRef, DamageDataComponent.getComponentType());
        if (damageData != null && damageData.getCurrentWielding() != null) {
            statComp.customArtefactData.put("stamina_regen_accumulator", 0.0f);
            return;
        }

        int delayIndex = EntityStatType.getAssetMap().getIndex("StaminaRegenDelay");
        if (delayIndex != Integer.MIN_VALUE) {
            EntityStatValue delayStat = statMap.get(delayIndex);
            if (delayStat != null && delayStat.get() < 0.0f) {
                statComp.customArtefactData.put("stamina_regen_accumulator", 0.0f);
                return;
            }
        }

        int staminaIndex = EntityStatType.getAssetMap().getIndex("Stamina");
        if (staminaIndex == Integer.MIN_VALUE) return;

        EntityStatValue staminaStat = statMap.get(staminaIndex);
        if (staminaStat == null) return;

        float maxStamina = staminaStat.getMax();
        float currentStamina = staminaStat.get();

        if (currentStamina >= maxStamina) {
            statComp.customArtefactData.put("stamina_regen_accumulator", 0.0f);
            return;
        }

        float baseStamina = 10.0f;
        float bonusStamina = Math.max(0, maxStamina - baseStamina);

        if (bonusStamina <= 0) return;

        float deltaSeconds = dt;
        if (dt > 1.0f) {
            deltaSeconds = dt / 1000.0f;
        }

        float accumulator = (float) statComp.customArtefactData.getOrDefault("stamina_regen_accumulator", 0.0f);
        accumulator += deltaSeconds;

        if (accumulator >= 0.1f) {
            int intervals = (int) (accumulator / 0.1f);
            accumulator -= (intervals * 0.1f);

            float artificialRegen = intervals * 0.03f * bonusStamina;

            float newStamina = Math.min(currentStamina + artificialRegen, maxStamina);
            statMap.setStatValue(staminaIndex, newStamina);
        }

        statComp.customArtefactData.put("stamina_regen_accumulator", accumulator);
    }
}