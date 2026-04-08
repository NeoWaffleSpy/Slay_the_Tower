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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    private final GameState gameState;
    private final Set<PlayerRef> participants = new HashSet<>(); // Added
    private Quest currentQuest;

    public GameManager(SkillMilestoneCodec milestoneData) {
        this.gameState = new GameState();
        this.gameState.initialize(milestoneData);
    }

    // event => initiateStage(event)
    public void initiateStage(PlayerRef playerRef) {
        boolean isNewStage = (this.currentQuest == null);
        this.participants.add(playerRef);

        if (isNewStage) {
            Store<EntityStore> store = playerRef.getReference().getStore();

            RoomCodec room = pickRoom();
            if(RoomTeleporter.canTeleportToRoom(room)){
                gameState.setCurrentRoom(room);

                MobGroupCodec mobGroups = pickMobGroup(gameState.getCurrentMilestone().difficulty);
                int mobs = mobGroups.getTotalMobCount();
                RoomNPCSpawner.spawnMobGroup(store, room, mobGroups);
                startQuest(mobGroups.getTotalMobCount());

                // List<Ref<EntityStore>> questMobList = RoomNPCSpawner.spawnMobGroup(store, room, mobGroups);
    //            if (questMobList != null) {
    //                startQuest(questMobList);
    //            } else {
    //                startQuest(mobGroups.getTotalMobCount());
    //            }


                for (PlayerRef p : participants) {
                    RoomTeleporter.teleportToRoom(p, room);
                }
            }else {
                playerRef.sendMessage(Message.raw("Can't teleport to room"));
            }
        } else {
            RoomTeleporter.teleportToRoom(playerRef, gameState.getCurrentRoom());
        }
        
    }

    private void startQuest(List<Ref<EntityStore>> questMobList) {
        this.currentQuest = new Quest(questMobList);
    }

    private void startQuest(int spawnedMobs) {
        this.currentQuest = new Quest(spawnedMobs);
    }


    public void updateQuest(QuestUpdate questUpdate) {
        if (currentQuest == null) return;

        if (questUpdate == QuestUpdate.MOB_DEATH) {
            currentQuest.incrementDeadMobs();
            if (currentQuest.isComplete()) {
                endStage(EndStageResult.SUCCESS);
            }
        } else if (questUpdate == QuestUpdate.PLAYER_DEATH) {
            endStage(EndStageResult.FAILURE);
        }
    }

    public void endStage(EndStageResult result) {
        this.currentQuest = null;
        switch (result) {
            case SUCCESS:
                ResolveNextStage();
                break;

            case FAILURE:
                gameState.reset();
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

        // Cleanup participants for the next run
        this.participants.clear();
    }

    public GameState getGameState() {
        return gameState;
    }

    public RoomCodec pickRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Room");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) {
            return null;
        }

        List<String> keyList = new ArrayList<>(roomKeys);
        String randomKey = keyList.get(ThreadLocalRandom.current().nextInt(keyList.size()));

        return roomMap.getAsset(randomKey);
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
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Lobby"); // Search for Lobby category
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) {
            return null;
        }

        List<String> keyList = new ArrayList<>(roomKeys);
        String randomKey = keyList.get(ThreadLocalRandom.current().nextInt(keyList.size()));

        return roomMap.getAsset(randomKey);
    }
}