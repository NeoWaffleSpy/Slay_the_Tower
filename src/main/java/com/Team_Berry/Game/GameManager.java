package com.Team_Berry.Game;

import com.Team_Berry.Game.Components.QuestNPCComponent;
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
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.Pair;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    private final GameState gameState;
    private final Set<PlayerRef> participants = new HashSet<>();
    private final Set<PlayerRef> deadParticipants = new HashSet<>();
    private final Set<PlayerRef> participantsInRoom = new HashSet<>();
    private World world = null;
    private Pair<RoomCodec, Quest> currentRoom;
    private Pair<RoomCodec, Quest> futureRoom;

    public GameManager(SkillMilestoneCodec milestoneData) {
        this.gameState = new GameState();
        this.gameState.initialize(milestoneData);
    }

    public void startGame(PlayerRef playerRef) {
        if (world == null) {
            this.world = playerRef.getReference().getStore().getExternalData().getWorld();
            initializeMilestone();
        }
    }

    public Pair<RoomCodec, Quest> prepareRoom(@Nullable RoomCodec exclude) {

        List<RoomCodec> validRooms = findValidRooms();

        if (exclude != null && validRooms.size() > 1) {
            validRooms.removeIf(room -> room.equals(exclude));
        }
        if (validRooms.isEmpty()) return null;

        RoomCodec selectedRoom = validRooms.get(ThreadLocalRandom.current().nextInt(validRooms.size()));

        int difficulty = this.gameState.getCurrentMilestone().difficulty;
        List<MobGroupCodec> mobGroups = findValidMobGroups(difficulty);
        Quest quest = new Quest(0);
        if (mobGroups != null && !mobGroups.isEmpty()) {
            MobGroupCodec selectedGroup = mobGroups.get(ThreadLocalRandom.current().nextInt(mobGroups.size()));
            quest = new Quest(selectedGroup.getTotalMobCount());
            world.execute(() -> {
                killQuestMobsInRoom(selectedRoom);
                RoomNPCSpawner.spawnMobGroup(world.getEntityStore().getStore(), selectedRoom, selectedGroup);
            });
        }

        return Pair.of(selectedRoom, quest);
    }

    public void killQuestMobsInRoom(RoomCodec room) {
        Store<EntityStore> store = world.getEntityStore().getStore();


        store.forEachChunk(GamePlugin.getQuestNPCComponentType(), (chunk, commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                QuestNPCComponent questComp = chunk.getComponent(i, GamePlugin.getQuestNPCComponentType());

                if (questComp != null && questComp.getRoom().equals(room)) {
                    commandBuffer.removeEntity(chunk.getReferenceTo(i), RemoveReason.REMOVE);
                }
            }
        });
    }

    public void initializeMilestone() {
        this.currentRoom = prepareRoom(null);
        this.futureRoom = prepareRoom(this.currentRoom.left());
    }

    private void goNextRoom() {
        this.participantsInRoom.clear();

        this.currentRoom = this.futureRoom;

        tpParticipantsToRoom();

        this.futureRoom = prepareRoom(this.currentRoom.left());
    }


    public void addParticipant(PlayerRef playerRef) {
        if (this.participants.add(playerRef)) {
            playerRef.sendMessage(Message.raw("Welcome to slay the tower !"));
        }
        if (this.participants.size() == 1) startGame(playerRef);
    }

    public void updateQuest(QuestUpdate questUpdate, @Nullable PlayerRef playerRef) {
        if (currentRoom == null || currentRoom.right() == null) return;
        Quest currentQuest = currentRoom.right();
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

        switch (result) {
            case SUCCESS:

                break;

            case FAILURE:

                break;
        }
    }

    private void ResolveNextStage() {

    }


    private void tpParticipantsToLobby() {
        RoomCodec lobby = findLobbyRoom();
        if (lobby != null) {
            for (PlayerRef participant : participants) {
                RoomTeleporter.teleportToRoom(participant, lobby);
            }
        }
    }

    private void tpParticipantsToRoom() {
        if (currentRoom == null) return;

        for (PlayerRef p : participants) {
            if (!participantsInRoom.contains(p)) {
                RoomTeleporter.teleportToRoom(p, currentRoom.left());
                this.participantsInRoom.add(p);
            }
        }
    }


    public List<RoomCodec> findValidRooms() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Room");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<RoomCodec> validRooms = new ArrayList<>();
        for (String key : roomKeys) {
            RoomCodec room = roomMap.getAsset(key);
            if (RoomTeleporter.canTeleportToRoom(room)) {
                validRooms.add(room);
            }
        }

        return validRooms;
    }

    public List<MobGroupCodec> findValidMobGroups(int difficulty) {
        DefaultAssetMap<String, MobGroupCodec> map = MobGroupCodec.getAssetMap();
        String tagSearch = "Difficulty=" + difficulty;
        int tagIndex = AssetRegistry.getOrCreateTagIndex(tagSearch);
        Set<String> groupKeys = map.getKeysForTag(tagIndex);

        if (groupKeys == null || groupKeys.isEmpty()) return null;

        List<MobGroupCodec> validGroups = new ArrayList<>();
        for (String key : groupKeys) {
            MobGroupCodec group = map.getAsset(key);
            if (group != null) {
                validGroups.add(group);
            }
        }

        return validGroups.isEmpty() ? null : validGroups;
    }

    private RoomCodec findLobbyRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Lobby");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) return null;

        List<String> keyList = new ArrayList<>(roomKeys);
        String randomKey = keyList.get(ThreadLocalRandom.current().nextInt(keyList.size()));

        return roomMap.getAsset(randomKey);
    }

    public void resolvePlayerDeath(PlayerRef playerRef) {
        if (this.participantsInRoom.contains(playerRef)) {
            this.deadParticipants.add(playerRef);
            if (this.deadParticipants.size() == this.participantsInRoom.size()) {
                endStage(EndStageResult.FAILURE);
            }
        }
    }

    private void resetQuest() {
        this.deadParticipants.clear();
        this.participantsInRoom.clear();
    }

    private void reviveDeadPlayers() {
        if (deadParticipants.isEmpty()) return;

        // TODO: Revive the player at the location of a living participant
        deadParticipants.clear();
    }
}