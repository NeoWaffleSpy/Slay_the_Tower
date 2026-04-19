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
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
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

    private final Set<PlayerRef> playersReady = new HashSet<>();

    private boolean pendingMilestoneTransition = false;

    private World world = null;
    private Pair<RoomCodec, Quest> currentRoom;
    private Pair<RoomCodec, Quest> futureRoom;
    private int globalMaxSpells = 0;

    public GameManager(World world, SkillMilestoneCodec milestoneData) {
        this.world = world;
        this.gameState = new GameState();
        this.gameState.initialize(milestoneData);
        log("Manager initialized for world.");
    }

    private void log(String message) {
        String worldName = (world != null) ? world.getName() : "UnknownWorld";
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", worldName, message));
    }

    public void initializeMilestone() {
        log("Initializing Milestone buffer...");
        massiveCleanWorldNPCs(); //if we can live without it, we probably should

        this.currentRoom = prepareRoom(null);
        this.futureRoom = prepareRoom(this.currentRoom.left());
        log(String.format("Buffer Ready. Current: %s | Future: %s",
                currentRoom.left().worldName, futureRoom.left().worldName));
    }

    public void massiveCleanWorldNPCs() {
        log("Commencing massive wipe of ALL NPCs in the world...");
        Store<EntityStore> store = world.getEntityStore().getStore();
        store.forEachChunk(NPCEntity.getComponentType(), (chunk, commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                commandBuffer.removeEntity(chunk.getReferenceTo(i), RemoveReason.REMOVE);
            }
        });
        log("World wipe complete.");
    }

    public Pair<RoomCodec, Quest> prepareRoom(@Nullable RoomCodec exclude) {
        log("Preparing a new room...");
        List<RoomCodec> validRooms = findValidRooms();

        if (exclude != null && validRooms.size() > 1) {
            validRooms.removeIf(room -> room.equals(exclude));
        }

        if (validRooms.isEmpty()) {
            log("FAILED: No valid rooms found in AssetMap.");
            return null;
        }

        RoomCodec selectedRoom = validRooms.get(ThreadLocalRandom.current().nextInt(validRooms.size()));
        int difficulty = this.gameState.getCurrentMilestone().difficulty;
        List<MobGroupCodec> mobGroups = findValidMobGroups(difficulty);
        Quest quest;

        if (mobGroups != null && !mobGroups.isEmpty()) {
            MobGroupCodec selectedGroup = mobGroups.get(ThreadLocalRandom.current().nextInt(mobGroups.size()));
            quest = new Quest(selectedGroup.getTotalMobCount());

            log(String.format("Room selected: %s. Spawning %d mobs (Difficulty: %d)",
                    selectedRoom.worldName, quest.getSpawnedMobs(), difficulty));

            world.execute(() -> {
                RoomNPCSpawner.spawnMobGroup(world.getEntityStore().getStore(), selectedRoom, selectedGroup);
            });
        } else {
            quest = new Quest(0);
            log("Room selected: " + selectedRoom.worldName + " (Empty: No mob groups found).");
        }

        log("Room preparation complete.");
        return Pair.of(selectedRoom, quest);
    }

    public Pair<RoomCodec, Quest> getCurrentRoom() {
        return this.currentRoom;
    }


    public void advanceToNextRoom() {
        log("Advancing tower. Shifting buffer...");
        this.participantsInRoom.clear();
        this.currentRoom = this.futureRoom;

        tpParticipantsToRoom();

        this.futureRoom = prepareRoom(this.currentRoom.left());
        log("Advance complete. Current Room is now: " + currentRoom.left().worldName);
    }

    public void addParticipant(PlayerRef playerRef) {
        log("Player joined the party: " + playerRef.getUsername());
        this.activeParticipants.add(playerRef);
        if (this.historicalSpellCounts.putIfAbsent(playerRef.getUuid(), 0) == null) {
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw("SLAY THE TOWER"),
                    Message.raw("Good luck, traveler..."),
                    true
            );
        }
    }

    public void removeParticipant(PlayerRef playerRef) {
        log("Player left the party: " + playerRef.getUsername());
        this.activeParticipants.remove(playerRef);
        this.participantsInRoom.remove(playerRef);
        this.deadParticipants.remove(playerRef);
        this.playersReady.remove(playerRef);

        if (!activeParticipants.isEmpty() && playersReady.size() >= activeParticipants.size()) {
            log("Final undecided player left. Force-completing reward phase.");
            completeRewardPhase();
        }
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
        log("Player " + playerRef.getUsername() + " incremented spells to: " + (currentSpells + 1));
    }

    public void updateQuest(QuestUpdate questUpdate, @Nullable PlayerRef playerRef) {
        if (currentRoom == null || currentRoom.right() == null) return;

        Quest currentQuest = currentRoom.right();

        if (questUpdate == QuestUpdate.MOB_DEATH) {
            currentQuest.incrementDeadMobs();
            log(String.format("Mob Death recorded. Progress: %d/%d", currentQuest.getDeadMobs(), currentQuest.getSpawnedMobs()));

            if (playerRef != null) {
                playerRef.sendMessage(Message.raw("Mob killed ! only " + currentQuest.getMobsLeft() + " left!"));
            }

            if (currentQuest.isComplete()) {
                log("Room Quest successfully completed.");
                if (playerRef != null) playerRef.sendMessage(Message.raw("Room Complete"));
                endStage(EndStageResult.SUCCESS);
            }
        } else if (questUpdate == QuestUpdate.PLAYER_DEATH && playerRef != null) {
            log("Player Death event: " + playerRef.getUsername());
            resolvePlayerDeath(playerRef);
        }
    }

    public void endStage(EndStageResult result) {
        log("Triggering EndStage with result: " + result);
        if (result == EndStageResult.SUCCESS) {
            handleStageSuccess();
        } else {
            handleStageFailure();
        }
    }

    private void handleStageSuccess() {
        SkillMilestoneCodec.MilestoneEntry oldMilestone = this.gameState.getCurrentMilestone();
        this.gameState.incrementClearedStages();
        SkillMilestoneCodec.MilestoneEntry newMilestone = this.gameState.getCurrentMilestone();

        reviveDeadPlayers();
        if (oldMilestone != newMilestone) {
            this.globalMaxSpells++;
            this.pendingMilestoneTransition = true;
            log("MILESTONE REACHED. Spell capacity increased to: " + globalMaxSpells);
            broadcastEventTitle(
                    "MILESTONE REACHED",
                    "Spell capacity increased to " + globalMaxSpells,
                    true,
                    null
            );
        } else {
            this.pendingMilestoneTransition = false;
        }

        grantArtifactRewards();
    }

    private void grantArtifactRewards() {
        log("Granting artifact rewards UI to all players.");
        broadcastEventTitle("ROOM CLEAR", "Select your reward...", false, null);

        for (PlayerRef player : activeParticipants) {
            com.Team_Berry.Artefacts.UI.ArtefactSelection ui = new com.Team_Berry.Artefacts.UI.ArtefactSelection(player, world.getEntityStore().getStore());
            ui.buildPage();
        }
    }

    public void onPlayerClaimedReward(PlayerRef playerRef) {
        if (!activeParticipants.contains(playerRef) || playersReady.contains(playerRef)) return;

        log("Reward claimed by: " + playerRef.getUsername());
        playersReady.add(playerRef);
        playerRef.sendMessage(Message.raw("Reward claimed! Waiting for the rest of your party..."));

        if (playersReady.size() >= activeParticipants.size()) {
            log("All players finished. Proceeding with phase completion.");
            completeRewardPhase();
        }
    }

    public void completeRewardPhase() {
        log("Completing Reward Phase. Path: " + (pendingMilestoneTransition ? "Lobby" : "Next Room"));
        if (pendingMilestoneTransition) {
            transitionToLobby();
        } else {
            advanceToNextRoom();
        }
        playersReady.clear();
    }

    public void startRoomFromLobby() {
        log("Request to start run from Lobby received.");
        if (currentRoom == null) return;

        List<String> unpreparedPlayers = new ArrayList<>();

        for (PlayerRef p : activeParticipants) {
            if (canPlayerPickSpell(p)) {
                unpreparedPlayers.add(p.getUsername());
            }
        }

        if (!unpreparedPlayers.isEmpty()) {
            String names = String.join(", ", unpreparedPlayers);
            log("Start aborted. Unprepared players: " + names);
            broadcastMessage("Cannot start yet! Waiting for players to select their spells: " + names);
            return;
        }

        log("All players ready. Teleporting to first room.");
        tpParticipantsToRoom();
    }

    private void transitionToLobby() {
        log("Moving party to Lobby.");
        this.participantsInRoom.clear();
        tpParticipantsToLobby();
        initializeMilestone();
        this.pendingMilestoneTransition = false;
    }

    private void handleStageFailure() {
        GamePlugin.get().destroyGameInstance(this.world);
        log("CRITICAL: Party Fall. Resetting milestone progress.");
        broadcastEventTitle("DEFEATED", "The tower claims another soul...", true, null);
        
        for (PlayerRef p : activeParticipants) {
            Ref<EntityStore> ref = p.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                try {
                    // This physically kicks them out of the instance and back to their original world/coordinates
                    com.hypixel.hytale.builtin.instances.InstancesPlugin.exitInstance(ref, store);
                } catch (IllegalArgumentException e) {
                    log("Failed to exit instance for player: " + p.getUsername());
                }
            }
        }
    }

    private void broadcastMessage(String text) {
        for (PlayerRef p : activeParticipants) {
            p.sendMessage(Message.raw(text));
        }
    }

    private void broadcastEventTitle(String primary, String secondary, boolean isMajor, @Nullable String icon) {
        Message primaryMsg = Message.raw(primary);
        Message secondaryMsg = Message.raw(secondary);

        for (PlayerRef p : activeParticipants) {
            EventTitleUtil.showEventTitleToPlayer(
                    p,
                    primaryMsg,
                    secondaryMsg,
                    isMajor,
                    icon,
                    2.0F,
                    0.5F,
                    0.5F
            );
        }
    }

    private void tpParticipantsToLobby() {
        RoomCodec lobby = findLobbyRoom();
        if (lobby != null) {
            log("Teleporting participants to Lobby asset: " + lobby.worldName);
            for (PlayerRef participant : activeParticipants) {
                RoomTeleporter.teleportToRoom(participant, lobby, this.world);
            }
        }
    }

    private void tpParticipantsToRoom() {
        if (currentRoom == null) return;
        log("Teleporting participants to Room asset: " + currentRoom.left().worldName);

        for (PlayerRef p : activeParticipants) {
            if (!participantsInRoom.contains(p)) {
                RoomTeleporter.teleportToRoom(p, currentRoom.left(), this.world);
                this.participantsInRoom.add(p);
            }
        }
    }

    public List<RoomCodec> findValidRooms() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Room");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) return Collections.emptyList();

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
            log(String.format("Death tracked: %s. Total Dead: %d/%d",
                    playerRef.getUsername(), deadParticipants.size(), participantsInRoom.size()));

            if (this.deadParticipants.size() == this.participantsInRoom.size()) {
                log("Full Wipe detected.");
                endStage(EndStageResult.FAILURE);
            }
        }
    }

    private void resetQuest() {
        log("Resetting Quest tracking states.");
        this.deadParticipants.clear();
        this.participantsInRoom.clear();
    }

    private void reviveDeadPlayers() {
        if (deadParticipants.isEmpty()) return;
        log("Reviving " + deadParticipants.size() + " dead participants.");
        deadParticipants.clear();
    }

    public World getWorld() {
        return this.world;
    }
}