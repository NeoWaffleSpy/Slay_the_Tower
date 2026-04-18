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

    private final Set<PlayerRef> activeParticipants = new HashSet<>();
    private final Set<PlayerRef> deadParticipants = new HashSet<>();
    private final Set<PlayerRef> participantsInRoom = new HashSet<>();
    private final Map<UUID, Integer> historicalSpellCounts = new HashMap<>();

    private World world = null;
    private Pair<RoomCodec, Quest> currentRoom;
    private Pair<RoomCodec, Quest> futureRoom;
    private int globalMaxSpells = 1;

    public GameManager(World world, SkillMilestoneCodec milestoneData) {
        this.world = world;
        this.gameState = new GameState();
        this.gameState.initialize(milestoneData);
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
        this.activeParticipants.add(playerRef);
        System.out.println("player added " + playerRef.getUsername());
        if (this.historicalSpellCounts.putIfAbsent(playerRef.getUuid(), 0) == null) {
            playerRef.sendMessage(Message.raw("Welcome to Slay the Tower!"));
        }
    }

    public void removeParticipant(PlayerRef playerRef) {
        this.activeParticipants.remove(playerRef);
        this.participantsInRoom.remove(playerRef);
        this.deadParticipants.remove(playerRef);
    }

    public boolean canPlayerPickSpell(PlayerRef playerRef) {
        int possessedSpells = historicalSpellCounts.getOrDefault(playerRef.getUuid(), 0);
        return possessedSpells < globalMaxSpells;
    }

    public Set<PlayerRef> getActiveParticipants() {
        return this.activeParticipants;
    }

    public void incrementPlayerSpellCount(PlayerRef playerRef) {
        int currentSpells = historicalSpellCounts.getOrDefault(playerRef.getUuid(), 0);
        historicalSpellCounts.put(playerRef.getUuid(), currentSpells + 1);
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
                int oldDifficulty = this.gameState.getCurrentMilestone().difficulty;
                this.gameState.incrementClearedStages();
                int newDifficulty = this.gameState.getCurrentMilestone().difficulty;

                // Check for milestone level up
                if (newDifficulty > oldDifficulty) {
                    this.globalMaxSpells++;
                    for (PlayerRef p : activeParticipants) {
                        p.sendMessage(Message.raw("Milestone Reached! Your spell capacity is now " + globalMaxSpells));
                    }
                }

                reviveDeadPlayers();
                goNextRoom();
                break;

            case FAILURE:
                tpParticipantsToLobby();
                this.gameState.reset();
                resetQuest();
                break;
        }
    }

    private void tpParticipantsToLobby() {
        RoomCodec lobby = findLobbyRoom();
        if (lobby != null) {
            // Updated to use activeParticipants
            for (PlayerRef participant : activeParticipants) {
                RoomTeleporter.teleportToRoom(participant, lobby);
            }
        }
    }

    private void tpParticipantsToRoom() {
        if (currentRoom == null) return;

        for (PlayerRef p : activeParticipants) {
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

    public World getWorld() {
        return this.world;
    }
}