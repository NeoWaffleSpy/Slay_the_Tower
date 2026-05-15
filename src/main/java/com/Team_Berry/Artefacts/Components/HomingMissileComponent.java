package com.Team_Berry.Artefacts.Components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public class HomingMissileComponent implements Component<EntityStore> {
    public Ref<EntityStore> target = null;
    public Ref<EntityStore> ownerRef = null;
    public float damage = 10.0f;
    public String hitParticle = "Explosion_Small";
    public String hitSound = "";
    public float particleScale = 1.0f;
    public boolean reachedApex = false;

    public double px, py, pz;
    public double vx, vy, vz;

    public double seekSpeed = 25.0;
    public double turnRate = 6.0;
    public double gravity = 18.0;

    public HomingMissileComponent() {
    }

    public HomingMissileComponent(double px, double py, double pz, double vx, double vy, double vz, Ref<EntityStore> ownerRef, float damage, String hitParticle, String hitSound, float particleScale) {
        this.px = px;
        this.py = py;
        this.pz = pz;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.ownerRef = ownerRef;
        this.damage = damage;
        this.hitParticle = hitParticle;
        this.hitSound = hitSound;
        this.particleScale = particleScale;
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        HomingMissileComponent copy = new HomingMissileComponent(px, py, pz, vx, vy, vz, ownerRef, damage, hitParticle, hitSound, particleScale);
        copy.reachedApex = reachedApex;
        copy.target = target;
        copy.seekSpeed = seekSpeed;
        copy.turnRate = turnRate;
        return copy;
    }
}