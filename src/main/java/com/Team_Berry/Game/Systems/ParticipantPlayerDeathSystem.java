package com.Team_Berry.Game.Systems;

import com.Team_Berry.Game.Enums.QuestUpdate;
import com.Team_Berry.Game.GameManager;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ParticipantPlayerDeathSystem extends RefSystem<EntityStore> {

    private final GameManager gameManager;

    public ParticipantPlayerDeathSystem(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void onEntityAdded(@NonNull Ref<EntityStore> ref, @NonNull AddReason addReason, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

    }

    @Override
    public void onEntityRemove(@NonNull Ref<EntityStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        gameManager.updateQuest(QuestUpdate.PLAYER_DEATH, commandBuffer.getComponent(ref, PlayerRef.getComponentType()));

    }


}
