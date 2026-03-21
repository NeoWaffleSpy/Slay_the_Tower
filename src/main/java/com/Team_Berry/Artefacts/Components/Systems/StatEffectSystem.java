package com.Team_Berry.Artefacts.Components.Systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class StatEffectSystem {
    public static class StatEffectDamageSystem extends DamageEventSystem {
        public StatEffectDamageSystem() {}

        @Override
        public void handle(int i, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull Damage damage) {

        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return null;
        }
    }

    public static class StatEffectTickingSystem extends EntityTickingSystem {
        public StatEffectTickingSystem() {}

        @Override
        public void tick(float v, int i, @NonNull ArchetypeChunk archetypeChunk, @NonNull Store store, @NonNull CommandBuffer commandBuffer) {

        }

        @Override
        public @Nullable Query getQuery() {
            return null;
        }
    }
}
