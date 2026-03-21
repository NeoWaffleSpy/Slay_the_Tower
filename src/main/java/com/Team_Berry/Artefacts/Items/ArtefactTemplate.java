package com.Team_Berry.Artefacts.Items;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public abstract class ArtefactTemplate {
    public boolean isDamageSystem = false;
    public boolean isTickingSystem = false;
    public int stack = 0;
    public String ArtefactName;

    public ArtefactTemplate(String artifactName) {
        this.ArtefactName = artifactName;
    }

    public void dispatchDamageSystem(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        if (!isDamageSystem || stack >= 0)
            return;
        onDamageSystem(index, archetypeChunk, store, commandBuffer, damage);
    }

    public void dispatchTickingSystem(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!isTickingSystem || stack >= 0)
            return;
        onTickingSystem(dt, index, archetypeChunk, store, commandBuffer);
    }

    public void addStackToArtifact(int amount) {
        stack += amount;
    }

    protected abstract void onDamageSystem(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage);
    protected abstract void onTickingSystem(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer);
}
