package com.Team_Berry.WeaponInteraction.Component.Ult;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public class UltExplosionComponent  implements Component<EntityStore> {

    long explosionDelay;
    long explosionDelayStart;
    Vector3d explosionPosition;

    public UltExplosionComponent() {}

    public UltExplosionComponent(long explosionDelay, long explosionDelayStart, Vector3d explosionPosition) {
        this.explosionDelay = explosionDelay;
        this.explosionDelayStart = explosionDelayStart;
        this.explosionPosition = new Vector3d(explosionPosition);
    }

    public UltExplosionComponent(Vector3d explosionPosition) {
        this.explosionPosition = new Vector3d(explosionPosition);
        this.explosionDelay = 0L;
        this.explosionDelayStart = 0L;
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new UltExplosionComponent(
                this.explosionDelay,
                this.explosionDelayStart,
                new Vector3d(this.explosionPosition)
        );
    }

    public boolean delayPassed(long now) {
        return now >= explosionDelayStart + explosionDelay;
    }

    public Vector3d getExplosionPosition() {
        return explosionPosition;
    }
}
