package com.Team_Berry.WeaponInteraction.Component;

import com.Team_Berry.WeaponInteraction.Utils.BleedStage;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.Nullable;

public class BleedComponent implements Component<EntityStore> {

    static int MAX_BLEED_STACKS = 4;
    long bleedStarTime;
    long bleedDuration;
    int currentBleedStacks;
    BleedStage effectStage;
    long accumulatedTime = 0;


    @Override
    public @Nullable Component<EntityStore> clone() {
        BleedComponent copy = new BleedComponent();
        copy.bleedStarTime = this.bleedStarTime;
        copy.bleedDuration = this.bleedDuration;
        copy.currentBleedStacks = this.currentBleedStacks;
        copy.effectStage = this.effectStage;
        copy.accumulatedTime = this.accumulatedTime;
        return copy;
    }

    public void applyStacks(int bleedStacks, long now, long bleedDuration) {
        this.bleedStarTime = now;
        this.bleedDuration = bleedDuration;
        if (currentBleedStacks >= MAX_BLEED_STACKS) return;

        this.currentBleedStacks = Math.min(this.currentBleedStacks + bleedStacks, MAX_BLEED_STACKS);

        this.effectStage = getStageFromStacks();
    }

    public BleedStage getStageFromStacks() {
        return switch (currentBleedStacks) {
            case 1 -> BleedStage.Stage1;
            case 2 -> BleedStage.Stage2;
            case 3 -> BleedStage.Stage3;
            case 4 -> BleedStage.Bleeding; // This is your MAX_BLEED_STACKS
            default -> BleedStage.None;
        };
    }

    public boolean isExpired(long now) {
        return now >= bleedStarTime + bleedDuration;
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
}

