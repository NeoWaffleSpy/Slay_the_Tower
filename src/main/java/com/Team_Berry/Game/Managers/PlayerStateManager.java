package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Game.Utils.PlayerInventory;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerStateManager {
    private static final String LOBBY_HEAL_ITEM = "Life";
    private final World world;
    private final Set<UUID> claimedLobbyHeal = new HashSet<>();

    public PlayerStateManager(World world) {
        this.world = world;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public void clearInventory(PlayerRef participant) {
        Ref<EntityStore> ref = participant.getReference();
        if (ref != null && ref.isValid()) {
            PlayerInventory.clearPlayerInventory(ref, ref.getStore());
            log("Cleared end-of-run inventory for: " + participant.getUsername());
        }
    }

    public void healPlayerToFull(PlayerRef playerRef) {
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());

                if (stats != null) {
                    stats.setStatValue(DefaultEntityStatTypes.getHealth(), stats.get(DefaultEntityStatTypes.getHealth()).getMax());
                    log("Healed " + playerRef.getUsername() + " to full health.");
                }
            }
        });
    }

    public void forceSurvivalMode(PlayerRef playerRef) {
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();

                Player.setGameMode(ref, GameMode.Adventure, store);
                log("Forced " + playerRef.getUsername() + " into Survival mode.");
            }
        });
    }

    public void clearLobbyHeals() {
        claimedLobbyHeal.clear();
    }

    public void kweebecMerchantInteraction(PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();

        if (claimedLobbyHeal.contains(playerId)) {
            playerRef.sendMessage(Message.raw("My supplies are exhausted! Come back later!"));
            healPlayerToFull(playerRef);
            return;
        }

        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                Player playerComponent = store.getComponent(ref, Player.getComponentType());

                if (playerComponent != null) {
                    claimedLobbyHeal.add(playerId);
                    healPlayerToFull(playerRef);

                    com.hypixel.hytale.server.core.inventory.ItemStack stack =
                            new com.hypixel.hytale.server.core.inventory.ItemStack(LOBBY_HEAL_ITEM, 3, null);

                    playerComponent.giveItem(stack, ref, store);

                    playerRef.sendMessage(Message.raw("Here take some of this! On the house."));
                    log(playerRef.getUsername() + " claimed their lobby supplies.");
                }
            }
        });
    }
}