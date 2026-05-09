package com.Team_Berry.Artefacts.Systems;

import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Interfaces.IOnDealPostDamage;
import com.Team_Berry.Artefacts.Interfaces.IOnTakePostDamage;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ArtefactLogicPostDamageSystem extends DamageEventSystem {

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int i, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull Damage damage) {
        if (damage.isCancelled() || damage.getAmount() <= 0.0f) return;

        Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(i);
        Damage.Source source = damage.getSource();
        Ref<EntityStore> attackerRef;

        if (source instanceof Damage.EntitySource entitySource) {
            attackerRef = entitySource.getRef();
            if (attackerRef == targetRef) return;
        } else {
            attackerRef = null;
        }

        StatEffectComponent defenderStatComp = commandBuffer.getComponent(targetRef, StatEffectComponent.getComponentType());
        if (defenderStatComp != null) {
            defenderStatComp.triggerLogic(IOnTakePostDamage.class, (artefact, logic) -> {
                logic.onTakePostDamage(artefact, targetRef, damage, defenderStatComp, commandBuffer);
            });
        }

        if (attackerRef != null) {
            StatEffectComponent attackerStatComp = commandBuffer.getComponent(attackerRef, StatEffectComponent.getComponentType());
            if (attackerStatComp != null) {
                attackerStatComp.triggerLogic(IOnDealPostDamage.class, (artefact, logic) -> {
                    logic.onDealPostDamage(artefact, attackerRef, targetRef, damage, attackerStatComp, commandBuffer);
                });
            }
        }
    }
}