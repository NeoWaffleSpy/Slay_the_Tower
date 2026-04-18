package com.Team_Berry.Game.Systems;

import com.Team_Berry.Game.Enums.QuestUpdate;
import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PlayerDeathSystem extends RefSystem<EntityStore> {

    public PlayerDeathSystem() {
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), DeathComponent.getComponentType());
    }

    @Override
    public void onEntityAdded(@NonNull Ref<EntityStore> ref, @NonNull AddReason addReason, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef != null) {
            World world = store.getExternalData().getWorld();

            if (world != null) {
                GameManager manager = GamePlugin.get().getGameManagers().get(world);

                if (manager != null) {
                    manager.updateQuest(QuestUpdate.PLAYER_DEATH, playerRef);
                }
            }
        }
    }

    @Override
    public void onEntityRemove(@NonNull Ref<EntityStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
    }
}