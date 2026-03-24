package com.Team_Berry.WeaponInteraction.Component;

import com.Team_Berry.WeaponInteraction.Utils.BleedRarity;
import com.Team_Berry.WeaponInteraction.Utils.BleedStage;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.Nullable;

public class BleedComponent implements Component<EntityStore> {

    static int MAX_BLEED_STACKS = 4;
    long bleedStartTime;
    long bleedDuration;
    int currentBleedStacks;
    BleedStage effectStage;
    long accumulatedTime = 0;
    BleedRarity bleedRarity = BleedRarity.NONE;


    @Override
    public @Nullable Component<EntityStore> clone() {
        BleedComponent copy = new BleedComponent();
        copy.bleedStartTime = this.bleedStartTime;
        copy.bleedDuration = this.bleedDuration;
        copy.currentBleedStacks = this.currentBleedStacks;
        copy.effectStage = this.effectStage;
        copy.accumulatedTime = this.accumulatedTime;
        return copy;
    }

    public void applyStacks(int bleedStacks, long now, long bleedDuration, String itemId) {
        this.bleedStartTime = now;
        this.bleedDuration = bleedDuration;

        this.currentBleedStacks = Math.min(this.currentBleedStacks + bleedStacks, MAX_BLEED_STACKS);

        this.effectStage = getStageFromStacks();
        setBleedRarityFromWeapon(itemId);
    }

    public BleedStage getStageFromStacks() {
        return switch (currentBleedStacks) {
            case 1 -> BleedStage.STAGE1;
            case 2 -> BleedStage.STAGE2;
            case 3 -> BleedStage.STAGE3;
            case 4 -> BleedStage.BLEEDING; // This is your MAX_BLEED_STACKS
            default -> BleedStage.NONE;
        };
    }

    public void setBleedRarityFromWeapon(String itemId) {
        this.bleedRarity = BleedRarity.fromString(itemId);
    }

    public String getDamageEffectKey() {
        return this.bleedRarity.getDamageKey();
    }

    public String getVisualEffectKey() {
        return this.effectStage.getEffectKey();
    }
    public boolean isExpired(long now) {
        return now >= bleedStartTime + bleedDuration;
    }

    public BleedStage getEffectStage() {
        return effectStage;
    }

    public long getBleedDuration() {
        return bleedDuration;
    }


    public long getAccumulatedTime() {
        return accumulatedTime;
    }

    public void setAccumulatedTime(long accumulatedTime) {
        this.accumulatedTime = accumulatedTime;
    }

    public int getCurrentBleedStacks() {
        return currentBleedStacks;
    }
}

