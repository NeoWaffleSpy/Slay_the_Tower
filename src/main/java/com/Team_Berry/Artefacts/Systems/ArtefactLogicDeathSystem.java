package com.Team_Berry.Artefacts.Systems;

import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnKill;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class ArtefactLogicDeathSystem extends RefChangeSystem<EntityStore, DeathComponent> {

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public @NotNull ComponentType<EntityStore, DeathComponent> componentType() {
        return DeathComponent.getComponentType();
    }

    @Override
    public void onComponentAdded(@NotNull Ref<EntityStore> deadEntityRef, @NotNull DeathComponent death, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        if (death.getDeathInfo() == null) return;

        Damage.Source source = death.getDeathInfo().getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            StatEffectComponent statComp = store.getComponent(attackerRef, StatEffectComponent.getComponentType());

            if (statComp != null) {
                statComp.triggerLogic(IOnKill.class, (artefact, logic) -> {
                    logic.onKill(artefact, attackerRef, deadEntityRef, statComp, store, commandBuffer);
                });
            }
        }
    }

    @Override
    public void onComponentSet(@NotNull Ref<EntityStore> ref, @Nullable DeathComponent oldComponent, @NotNull DeathComponent newComponent, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentRemoved(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent death, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
    }
}