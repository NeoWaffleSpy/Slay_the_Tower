package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Game.Objectives.CustomRoomTask;
import com.hypixel.hytale.builtin.adventure.objectives.Objective;
import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.builtin.adventure.objectives.task.ObjectiveTask;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.Team_Berry.Game.Data.Quest;
import it.unimi.dsi.fastutil.Pair;
import com.Team_Berry.Rooms.Codecs.RoomCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ObjectiveManager {
    private final World world;
    private UUID currentRoomObjectiveId = null;

    public ObjectiveManager(World world) {
        this.world = world;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public void startSharedRoomObjective(Set<PlayerRef> activeParticipants, Pair<RoomCodec, Quest> currentRoom) {
        completeSharedRoomObjective();

        Set<UUID> playerUUIDs = activeParticipants.stream()
                .map(PlayerRef::getUuid)
                .collect(Collectors.toSet());

        if (playerUUIDs.isEmpty()) return;

        Objective obj = ObjectivePlugin.get().startObjective(
                "Slay_The_Tower_Room_Quest",
                playerUUIDs,
                world.getWorldConfig().getUuid(),
                null,
                world.getEntityStore().getStore()
        );

        if (obj != null) {
            this.currentRoomObjectiveId = obj.getObjectiveUUID();
            updateSharedRoomObjective(currentRoom);
            log("Started shared UI objective for party: " + currentRoomObjectiveId);
        }
    }

    public void updateSharedRoomObjective(Pair<RoomCodec, Quest> currentRoom) {
        if (currentRoomObjectiveId == null || currentRoom == null || currentRoom.right() == null) return;

        Objective obj = ObjectivePlugin.get().getObjectiveDataStore().getObjective(currentRoomObjectiveId);
        if (obj != null && obj.getCurrentTasks() != null && obj.getCurrentTasks().length > 0) {
            ObjectiveTask task = obj.getCurrentTasks()[0];

            if (task instanceof CustomRoomTask customTask) {
                int deadMobs = currentRoom.right().getDeadMobs();
                int totalMobs = currentRoom.right().getSpawnedMobs();
                if (totalMobs <= 0) {
                    totalMobs = 1;
                }
                customTask.setProgress(deadMobs, totalMobs);
                customTask.sendUpdateObjectiveTaskPacket(obj);
            }
        }
    }

    public void completeSharedRoomObjective() {
        if (currentRoomObjectiveId == null) return;

        Objective obj = ObjectivePlugin.get().getObjectiveDataStore().getObjective(currentRoomObjectiveId);
        if (obj != null) {
            Store<EntityStore> store = world.getEntityStore().getStore();

            if (obj.getCurrentTasks() != null) {
                for (ObjectiveTask task : obj.getCurrentTasks()) {
                    task.complete(obj, null);
                }
            }
            obj.checkTaskSetCompletion(store);
            log("Cleared shared UI objective.");
        }
        currentRoomObjectiveId = null;
    }

    public void detachPlayerFromObjective(PlayerRef playerRef) {
        if (currentRoomObjectiveId != null) {
            ObjectivePlugin.get().removePlayerFromExistingObjective(
                    world.getEntityStore().getStore(),
                    playerRef.getUuid(),
                    currentRoomObjectiveId
            );
            log("Detached objective from player: " + playerRef.getUsername());
        }
    }

    public void cleanupLeftoverObjectives() {
        ObjectivePlugin plugin = ObjectivePlugin.get();
        if (plugin != null && plugin.getObjectiveDataStore() != null) {
            Store<EntityStore> store = world.getEntityStore().getStore();

            List<Objective> activeObjectives = new ArrayList<>(plugin.getObjectiveDataStore().getObjectiveCollection());

            for (Objective obj : activeObjectives) {
                if (obj.getObjectiveId().equals("Slay_The_Tower_Room_Quest")) {
                    plugin.cancelObjective(obj.getObjectiveUUID(), store);
                    log("Forcefully cleaned up a ghost objective from a previous game: " + obj.getObjectiveUUID());
                }
            }
        }
    }
}