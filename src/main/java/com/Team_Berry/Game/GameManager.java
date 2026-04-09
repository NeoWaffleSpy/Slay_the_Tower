package com.Team_Berry.Game;

import com.Team_Berry.Game.Data.GameState;
import com.Team_Berry.Game.Data.Quest;
import com.Team_Berry.Game.Enums.EndStageResult;
import com.Team_Berry.Game.Enums.QuestUpdate;
import com.Team_Berry.Rooms.Codecs.MobGroupCodec;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;
import com.Team_Berry.Rooms.Utils.RoomNPCSpawner;
import com.Team_Berry.Rooms.Utils.RoomTeleporter;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {

    private final GameState gameState;
    private final Set<PlayerRef> participants = new HashSet<>();
    private final Set<PlayerRef> deadPlayers = new HashSet<>();
    private Quest currentQuest;
    private boolean isStartingStage = false;
    private long lastRoomStartTick = -1;

    public GameManager(SkillMilestoneCodec milestoneData) {
        this.gameState = new GameState();
        this.gameState.initialize(milestoneData);
    }


    public void initiateStage(PlayerRef playerRef) {
        long currentTick = playerRef.getReference().getStore()
                .getResource(TimeResource.getResourceType())
                .getNow().toEpochMilli();


        synchronized (this) {
            if (isStartingStage) {
                this.participants.add(playerRef);
                return;
            }

            if (this.currentQuest != null || (currentTick - lastRoomStartTick < 1000)) {
                this.participants.add(playerRef);
                if (gameState.getCurrentRoom() != null) {
                    RoomTeleporter.teleportToRoom(playerRef, gameState.getCurrentRoom());
                }
                return;
            }

            isStartingStage = true;
            lastRoomStartTick = currentTick;
        }

        try {
            this.participants.add(playerRef);
            playerRef.sendMessage(Message.raw("Starting stage..."));

            RoomCodec room = pickRoom();
            if (room != null) {
                gameState.setCurrentRoom(room);
                Store<EntityStore> store = playerRef.getReference().getStore();
                MobGroupCodec mobGroups = pickMobGroup(gameState.getCurrentMilestone().difficulty);

                startQuest(mobGroups.getTotalMobCount());
                RoomNPCSpawner.spawnMobGroup(store, room, mobGroups);

                for (PlayerRef p : participants) {
                    RoomTeleporter.teleportToRoom(p, room);
                }
            }
        } catch (Exception e) {
            this.currentQuest = null;
            playerRef.sendMessage(Message.raw("Error starting room."));
        } finally {
            isStartingStage = false;
        }
    }

    private void startQuest(List<Ref<EntityStore>> questMobList) {
        this.currentQuest = new Quest(questMobList);
    }

    private void startQuest(int spawnedMobs) {
        this.currentQuest = new Quest(spawnedMobs);
    }

    public void updateQuest(QuestUpdate questUpdate, @Nullable PlayerRef playerRef) {
        if (currentQuest == null) return;

        if (questUpdate == QuestUpdate.MOB_DEATH) {
            currentQuest.incrementDeadMobs();
            if (currentQuest.isComplete()) {
                endStage(EndStageResult.SUCCESS);
            }
        } else if (questUpdate == QuestUpdate.PLAYER_DEATH && playerRef != null) {
            resolvePlayerDeath(playerRef);
        }
    }

    public void endStage(EndStageResult result) {
        this.currentQuest = null;
        switch (result) {
            case SUCCESS:
                reviveDeadPlayers();
                ResolveNextStage();
                break;

            case FAILURE:
                resetGame(); // Use the new unified reset method
                goToLobby();
                break;
        }
    }

    private void ResolveNextStage() {
        gameState.incrementClearedStages();

        if (gameState.getRoomsUntilNextMilestone() <= 0) {
            gameState.advanceToNextMilestone();
            goToLobby();
        } else {
            goNextRoom();
        }
    }

    private void goNextRoom() {
        if (!participants.isEmpty()) {
            initiateStage(participants.iterator().next());
        }
    }

    private void goToLobby() {
        RoomCodec lobby = pickLobbyRoom();
        if (lobby != null) {
            for (PlayerRef participant : participants) {
                RoomTeleporter.teleportToRoom(participant, lobby);
            }
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    public RoomCodec pickRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Room");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) return null;

        List<RoomCodec> validRooms = new ArrayList<>();
        for (String key : roomKeys) {
            RoomCodec room = roomMap.getAsset(key);
            if (RoomTeleporter.canTeleportToRoom(room)) {
                validRooms.add(room);
            }
        }

        if (validRooms.isEmpty()) return null;

        return validRooms.get(ThreadLocalRandom.current().nextInt(validRooms.size()));
    }

    public MobGroupCodec pickMobGroup(int difficulty) {
        DefaultAssetMap<String, MobGroupCodec> map = MobGroupCodec.getAssetMap();
        String tagSearch = "Difficulty=" + difficulty;
        int tagIndex = AssetRegistry.getOrCreateTagIndex(tagSearch);
        Set<String> groupKeys = map.getKeysForTag(tagIndex);

        if (groupKeys == null || groupKeys.isEmpty()) return null;

        List<String> keyList = new ArrayList<>(groupKeys);
        String randomKey = keyList.get(ThreadLocalRandom.current().nextInt(keyList.size()));

        return map.getAsset(randomKey);
    }

    private RoomCodec pickLobbyRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Lobby");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) return null;

        List<String> keyList = new ArrayList<>(roomKeys);
        String randomKey = keyList.get(ThreadLocalRandom.current().nextInt(keyList.size()));

        return roomMap.getAsset(randomKey);
    }

    public void resolvePlayerDeath(PlayerRef playerRef) {
        boolean wasActiveParticipant = this.participants.remove(playerRef);

        if (wasActiveParticipant) {
            this.deadPlayers.add(playerRef);
            if (this.participants.isEmpty()) {
                endStage(EndStageResult.FAILURE);
            }
        }
    }

    private void resetGame() {
        this.currentQuest = null;
        this.participants.clear();
        this.deadPlayers.clear();
    }

    private void reviveDeadPlayers() {
        if (deadPlayers.isEmpty()) return;

        // TODO: Revive the player at the location of a living participant
        this.participants.addAll(deadPlayers);
        deadPlayers.clear();
    }
}