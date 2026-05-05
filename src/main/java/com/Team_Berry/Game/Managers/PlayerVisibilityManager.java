package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerVisibilityManager {
    private final World world;

    public PlayerVisibilityManager(World world) {
        this.world = world;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public void hidePlayer(PlayerRef deadPlayer, Set<PlayerRef> activeParticipants) {
        UUID deadUuid = deadPlayer.getUuid();

        world.execute(() -> {
            for (PlayerRef participant : activeParticipants) {
                if (!participant.equals(deadPlayer)) {
                    participant.getHiddenPlayersManager().hidePlayer(deadUuid);
                }
            }
            log("Hid " + deadPlayer.getUsername() + " from all active participants.");
        });
    }

    public void showAllDeadPlayers(Set<PlayerRef> deadParticipants, Set<PlayerRef> activeParticipants) {
        if (deadParticipants.isEmpty()) return;
        log("inside show");

        List<PlayerRef> deadPlayersSnapshot = new java.util.ArrayList<>(deadParticipants);
        world.execute(() -> {
            for (PlayerRef deadPlayer : deadPlayersSnapshot) {
                UUID deadUuid = deadPlayer.getUuid();
                log("dead player found : " + deadPlayer.getUsername());

                for (PlayerRef participant : activeParticipants) {
                    if (!participant.equals(deadPlayer)) {
                        participant.getHiddenPlayersManager().showPlayer(deadUuid);
                        log("shown : " + deadPlayer.getUsername());
                    }
                }
            }
            log("Revealed all dead participants before teleportation.");
        });
    }
}
