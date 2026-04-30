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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PlayerDeathSystem extends RefChangeSystem<EntityStore, DeathComponent> {

    public PlayerDeathSystem() {
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public @NonNull ComponentType<EntityStore, DeathComponent> componentType() {
        return DeathComponent.getComponentType();
    }

    @Override
    public void onComponentAdded(@NonNull Ref<EntityStore> ref, @NonNull DeathComponent death, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef != null) {
            World world = store.getExternalData().getWorld();
            GameManager manager = GamePlugin.get().getGameManagers().get(world);
            if (manager != null) {
                death.setShowDeathMenu(false);
                DeathComponent.respawn(store, ref);
                manager.updateQuest(QuestUpdate.PLAYER_DEATH, playerRef, null);

            }
        }
    }

    @Override
    public void onComponentSet(@NonNull Ref<EntityStore> ref, @Nullable DeathComponent oldComponent, @NonNull DeathComponent newComponent, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentRemoved(@NonNull Ref<EntityStore> ref, @NonNull DeathComponent death, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        playerRef.sendMessage(Message.raw("Your journey continues"));
    }
}