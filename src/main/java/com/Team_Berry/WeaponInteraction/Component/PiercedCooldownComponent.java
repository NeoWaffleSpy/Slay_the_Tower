package com.Team_Berry.WeaponInteraction.Component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public class PiercedCooldownComponent implements Component<EntityStore> {

    long lastPierceTime;

    @Override
    public @Nullable Component<EntityStore> clone() {

        PiercedCooldownComponent copy = new PiercedCooldownComponent();
        copy.lastPierceTime = this.lastPierceTime;
        return copy;
    }

    public long getLastPierceTime() {
        return lastPierceTime;
    }

    public void setLastPierceTime(long lastPierceTime) {
        this.lastPierceTime = lastPierceTime;
    }


}
