package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PartyManager {
    private final World world;
    private final Set<PlayerRef> activeParticipants = new HashSet<>();
    private final Set<PlayerRef> deadParticipants = new HashSet<>();
    private final Set<PlayerRef> participantsInRoom = new HashSet<>();
    private final Set<PlayerRef> playersReady = new HashSet<>();

    public PartyManager(World world) {
        this.world = world;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    // --- STATE MANAGEMENT ---

    public boolean addParticipant(PlayerRef playerRef) {
        return activeParticipants.add(playerRef);
    }

    public void removeParticipant(PlayerRef playerRef) {
        activeParticipants.remove(playerRef);
        participantsInRoom.remove(playerRef);
        deadParticipants.remove(playerRef);
        playersReady.remove(playerRef);
    }

    public void addParticipantInRoom(PlayerRef playerRef) {
        participantsInRoom.add(playerRef);
    }

    public void addDeadParticipant(PlayerRef playerRef) {
        deadParticipants.add(playerRef);
    }

    public void addPlayerReady(PlayerRef playerRef) {
        playersReady.add(playerRef);
    }

    public Set<PlayerRef> getActiveParticipants() {
        return activeParticipants;
    }

    public Set<PlayerRef> getDeadParticipants() {
        return deadParticipants;
    }

    public Set<PlayerRef> getParticipantsInRoom() {
        return participantsInRoom;
    }

    public Set<PlayerRef> getPlayersReady() {
        return playersReady;
    }

    public boolean isParticipantInRoom(PlayerRef playerRef) {
        return participantsInRoom.contains(playerRef);
    }

    public boolean isParticipantActive(PlayerRef playerRef) {
        return activeParticipants.contains(playerRef);
    }

    public void clearParticipantsInRoom() {
        participantsInRoom.clear();
    }

    public void clearDeadParticipants() {
        deadParticipants.clear();
    }

    public void clearPlayersReady() {
        playersReady.clear();
    }

    public void clearAll() {
        activeParticipants.clear();
        participantsInRoom.clear();
        deadParticipants.clear();
        playersReady.clear();
    }

    public String getParticipantIndices() {
        return activeParticipants.stream()
                .filter(p -> p.getReference() != null && p.getReference().isValid())
                .map(p -> p.getUsername() + " " + p.getReference().getIndex())
                .collect(Collectors.joining("\n"));
    }


    public void hidePlayer(PlayerRef deadPlayer) {
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

    public void showAllDeadPlayers() {
        if (deadParticipants.isEmpty()) return;
        log("inside show");

        List<PlayerRef> deadPlayersSnapshot = new ArrayList<>(deadParticipants);
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