package com.Team_Berry.Game.Systems;

import com.Team_Berry.Game.Enums.QuestUpdate;
import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class QuestNPCDeathSystem extends RefChangeSystem<EntityStore, DeathComponent> {

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public @NotNull ComponentType<EntityStore, DeathComponent> componentType() {
        return DeathComponent.getComponentType();
    }

    @Override
    public void onComponentAdded(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent death, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        World world = store.getExternalData().getWorld();
        GameManager manager = GamePlugin.get().getGameManagers().get(world);

        if (manager == null) return;

        PlayerRef killerPlayer = null;

        if (death.getDeathInfo() != null) {
            Damage fatalDamage = death.getDeathInfo();
            Damage.Source source = fatalDamage.getSource();

            if (source instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> attackerRef = entitySource.getRef();
                killerPlayer = store.getComponent(attackerRef, PlayerRef.getComponentType());
            }
        }

        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());

        if (uuidComp != null) {
            UUID deadMobUuid = uuidComp.getUuid();
            manager.updateQuest(QuestUpdate.MOB_DEATH, killerPlayer, deadMobUuid);
        }
    }

    @Override
    public void onComponentSet(@NotNull Ref<EntityStore> ref, @Nullable DeathComponent oldComponent, @NotNull DeathComponent newComponent, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentRemoved(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent death, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
    }
}