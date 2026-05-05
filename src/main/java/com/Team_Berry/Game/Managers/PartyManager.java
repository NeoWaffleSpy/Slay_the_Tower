package com.Team_Berry.Game.Managers;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PartyManager {
    private final Set<PlayerRef> activeParticipants = new HashSet<>();
    private final Set<PlayerRef> deadParticipants = new HashSet<>();
    private final Set<PlayerRef> participantsInRoom = new HashSet<>();
    private final Set<PlayerRef> playersReady = new HashSet<>();

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
}
