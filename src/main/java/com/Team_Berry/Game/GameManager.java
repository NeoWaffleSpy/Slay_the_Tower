package com.Team_Berry.Game;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.Artefacts.UI.ArtefactSelection;
import com.Team_Berry.Artefacts.UI.SkillSelection;
import com.Team_Berry.Game.Data.GameState;
import com.Team_Berry.Game.Data.Quest;
import com.Team_Berry.Game.Enums.EndStageResult;
import com.Team_Berry.Game.Enums.QuestUpdate;
import com.Team_Berry.Game.Objectives.CustomRoomTask;
import com.Team_Berry.Rooms.Codecs.MobGroupCodec;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;
import com.Team_Berry.Rooms.Utils.RoomNPCSpawner;
import com.Team_Berry.Rooms.Utils.RoomTeleporter;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.adventure.objectives.Objective;
import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.builtin.adventure.objectives.task.ObjectiveTask;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.builtin.weather.resources.WeatherResource;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import it.unimi.dsi.fastutil.Pair;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class GameManager {
    private static final List<String> RANDOM_WEATHERS = Arrays.asList(
            "Weather_Red", "Weather_Blue", "Weather_Purple", "Weather_Green"
    );
    private final GameState gameState;
    private final RoomCodec lobby;
    private final Set<PlayerRef> activeParticipants = new HashSet<>();
    private final Set<PlayerRef> deadParticipants = new HashSet<>();
    private final Set<PlayerRef> participantsInRoom = new HashSet<>();
    private final Map<UUID, Integer> historicalSkillCounts = new HashMap<>();
    private final Map<UUID, String> playerClasses = new HashMap<>();
    private final Map<UUID, Set<String>> playerOwnedSkills = new HashMap<>();
    private final Map<UUID, List<String>> playerOwnedArtefacts = new HashMap<>();
    private final Set<PlayerRef> playersReady = new HashSet<>();
    private final Set<UUID> currentRoomMobs = new HashSet<>();
    private final Set<UUID> futureRoomMobs = new HashSet<>();
    private World world;
    private UUID currentRoomObjectiveId = null;
    private boolean pendingMilestoneTransition = false;
    private Pair<RoomCodec, Quest> currentRoom;
    private Pair<RoomCodec, Quest> futureRoom;
    private int globalMaxSkills = 1;
    private boolean statueSpawn = false;

    public GameManager(World world, SkillMilestoneCodec milestoneData) {
        this.world = world;
        this.gameState = new GameState();
        this.gameState.initialize(milestoneData);
        this.lobby = findLobbyRoom();
        log("Manager initialized for world.");
    }

    public void setStatueSpawn() {
        if (!statueSpawn) {
            WorldConfig config = world.getWorldConfig();
            if (lobby.getFirstSpawnPosition() != null) {
                Transform transform = new Transform(lobby.getFirstSpawnPosition());
                config.setSpawnProvider(new GlobalSpawnProvider(transform));
                config.markChanged();
                statueSpawn = true;
                log("Statue spawn set!");
            } else {
                log("Could not set statue spawn because there is no lobby or lobby SpawnPoint");
            }
        }
    }

    private void log(String message) {
        String worldName = (world != null) ? world.getName() : "UnknownWorld";
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", worldName, message));
    }

    public void initializeMilestone() {
        log("Initializing Milestone buffer...");
        cleanupLeftoverObjectives();

        this.currentRoom = prepareRoom(null, currentRoomMobs);
        this.futureRoom = prepareRoom(this.currentRoom.left(), futureRoomMobs);

        log(String.format("Buffer Ready. Current: %s | Future: %s",
                currentRoom.left().worldName, futureRoom.left().worldName));
    }


    public Pair<RoomCodec, Quest> prepareRoom(@Nullable RoomCodec exclude, Set<UUID> targetMobTracker) {
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
                List<UUID> spawnedMobs = RoomNPCSpawner.spawnMobGroup(world.getEntityStore().getStore(), selectedRoom, selectedGroup);
                targetMobTracker.clear();
                targetMobTracker.addAll(spawnedMobs);
                log("Successfully registered " + spawnedMobs.size() + " mob UUIDs for room.");
            });
        } else {
            quest = new Quest(0);
            log("Room selected: " + selectedRoom.worldName + " (Empty: No mob groups found).");
        }

        log("Room preparation complete.");
        return Pair.of(selectedRoom, quest);
    }

    public void cleanRoom(Set<UUID> mobTracker) {
        if (mobTracker.isEmpty()) return;

        List<UUID> mobsToDelete = new ArrayList<>(mobTracker);
        mobTracker.clear();

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            int deletedCount = 0;

            for (UUID mobId : mobsToDelete) {
                Ref<EntityStore> ref = getRefByUUID(store, mobId);
                if (ref != null && ref.isValid()) {
                    store.removeEntity(ref, RemoveReason.REMOVE);
                    deletedCount++;
                }
            }
            log("Room cleaned. " + deletedCount + " remaining mobs deleted.");
        });
    }

    private Ref<EntityStore> getRefByUUID(Store<EntityStore> store, UUID targetUUID) {
        AtomicReference<Ref<EntityStore>> foundRef = new AtomicReference<>(null);

        store.forEachChunk(UUIDComponent.getComponentType(), (chunk, commandBuffer) -> {
            if (foundRef.get() != null) return;

            for (int i = 0; i < chunk.size(); i++) {
                UUIDComponent uuidComp =
                        (UUIDComponent) chunk.getComponent(i, UUIDComponent.getComponentType());

                if (uuidComp != null && uuidComp.getUuid().equals(targetUUID)) {
                    foundRef.set(chunk.getReferenceTo(i));
                    break;
                }
            }
        });

        return foundRef.get();
    }

    public Pair<RoomCodec, Quest> getCurrentRoom() {
        return this.currentRoom;
    }

    public void advanceToNextRoom() {
        log("Advancing tower. Shifting buffer...");
        this.participantsInRoom.clear();
        cleanRoom(currentRoomMobs);

        this.currentRoom = this.futureRoom;

        this.currentRoomMobs.clear();
        this.currentRoomMobs.addAll(this.futureRoomMobs);
        this.futureRoomMobs.clear();

        tpParticipantsToRoom();

        this.futureRoom = prepareRoom(this.currentRoom.left(), futureRoomMobs);
        log("Advance complete. Current Room is now: " + currentRoom.left().worldName);
    }

    public void addParticipant(PlayerRef playerRef) {
        log("Player joined the party: " + playerRef.getUsername());
        if (this.activeParticipants.add(playerRef)) {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref.getStore();
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            //TODO : Add 50 max health to the player and then remove it on remove participant
            // stats.addStatValue(EntityStatType.getAssetMap().getIndex("Health"), 50);

            teleportPlayerToSpawn(playerRef);
            forceSurvivalMode(playerRef);
        }

        restorePlayerArtefacts(playerRef);

        if (!playerClasses.containsKey(playerRef.getUuid())) {
            setPlayerClass(playerRef, "Dagger");
        }

        if (this.historicalSkillCounts.putIfAbsent(playerRef.getUuid(), 0) == null) {
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw("SLAY THE TOWER"),
                    Message.raw("Good luck, traveler..."),
                    true
            );
        }
    }

    private void restorePlayerArtefacts(PlayerRef playerRef) {
        if (playerOwnedArtefacts.containsKey(playerRef.getUuid())) {
            StatEffectComponent statComp = StatEffectComponent.getPlayerStatComp(playerRef);
            if (statComp != null) {
                DefaultAssetMap<String, ArtefactCodec> artefactMap = ArtefactCodec.getAssetMap();
                List<String> savedArtefacts = playerOwnedArtefacts.get(playerRef.getUuid());

                for (String artefactId : savedArtefacts) {
                    ArtefactCodec artefact = artefactMap.getAsset(artefactId);
                    if (artefact != null) {
                        statComp.addArtifact(artefact);
                    }
                }
                log("Restored " + savedArtefacts.size() + " artefacts for returning player: " + playerRef.getUsername());
            }
        }
    }

    public void removeParticipant(PlayerRef playerRef) {
        //TODO : Remove the added 50 max health to the player on add participant

        log("Player left the party: " + playerRef.getUsername());

        cleanupLeftoverObjectives();
        this.activeParticipants.remove(playerRef);
        this.participantsInRoom.remove(playerRef);
        this.deadParticipants.remove(playerRef);
        this.playersReady.remove(playerRef);

        if (!activeParticipants.isEmpty() && playersReady.size() >= activeParticipants.size()) {
            log("Final undecided player left. Force-completing reward phase.");
            completeRewardPhase();
        }
    }

    public void setPlayerClass(PlayerRef playerRef, String className) {
        playerClasses.put(playerRef.getUuid(), className);
        log("Assigned class '" + className + "' to " + playerRef.getUsername());
    }

    public boolean canPlayerPickSkill(PlayerRef playerRef) {
        int possessedSkills = historicalSkillCounts.getOrDefault(playerRef.getUuid(), 0);
        return possessedSkills < globalMaxSkills;
    }

    public Set<PlayerRef> getActiveParticipants() {
        return this.activeParticipants;
    }

    public void incrementPlayerSkillCount(PlayerRef playerRef) {
        int currentSkills = historicalSkillCounts.getOrDefault(playerRef.getUuid(), 0);
        historicalSkillCounts.put(playerRef.getUuid(), currentSkills + 1);
        log("Player " + playerRef.getUsername() + " incremented skills to: " + (currentSkills + 1));
    }

    public void updateQuest(QuestUpdate questUpdate, @Nullable PlayerRef playerRef, @Nullable UUID deadMobId) {
        if (currentRoom == null || currentRoom.right() == null) return;

        if (questUpdate == QuestUpdate.MOB_DEATH && deadMobId != null) {

            if (currentRoomMobs.remove(deadMobId)) {
                Quest currentQuest = currentRoom.right();
                currentQuest.incrementDeadMobs();
                log(String.format("Current Room Mob Death. Progress: %d/%d", currentQuest.getDeadMobs(), currentQuest.getSpawnedMobs()));

                if (playerRef != null) {
                    playerRef.sendMessage(Message.raw("Mob killed! only " + currentQuest.getMobsLeft() + " left!"));
                }
                updateSharedRoomObjective();

                if (currentQuest.isComplete()) {
                    log("Room Quest successfully completed.");
                    if (playerRef != null) playerRef.sendMessage(Message.raw("Room Complete"));
                    endStage(EndStageResult.SUCCESS);
                }
            } else if (futureRoom != null && futureRoom.right() != null && futureRoomMobs.remove(deadMobId)) {
                futureRoom.right().incrementDeadMobs();
                log("A Future Room Mob died prematurely! Lowered future quest requirements.");
            }

        } else if (questUpdate == QuestUpdate.PLAYER_DEATH && playerRef != null) {
            log("Player Death event: " + playerRef.getUsername());
            resolvePlayerDeath(playerRef);
        }
    }

    public void endStage(EndStageResult result) {
        completeSharedRoomObjective();
        log("Triggering EndStage with result: " + result);
        if (result == EndStageResult.SUCCESS) {
            handleStageSuccess();
        } else {
            handleStageFailure();
        }
    }

    private void handleStageSuccess() {
        completeSharedRoomObjective();
        SkillMilestoneCodec.MilestoneEntry oldMilestone = this.gameState.getCurrentMilestone();
        this.gameState.incrementClearedStages();
        SkillMilestoneCodec.MilestoneEntry newMilestone = this.gameState.getCurrentMilestone();

        reviveDeadPlayers();

        setForcedWeather("Weather_Transition", world.getEntityStore().getStore());
        log("Weather changed to Transition state.");

        if (this.gameState.isRunComplete()) {
            log("VICTORY! The final milestone has been cleared.");
            handleRunVictory();
            return;
        }

        if (oldMilestone != newMilestone) {
            this.globalMaxSkills++;
            this.pendingMilestoneTransition = true;
            log("MILESTONE REACHED. Skill capacity increased to: " + globalMaxSkills);
            broadcastEventTitle(
                    "MILESTONE REACHED",
                    "Skill capacity increased to " + globalMaxSkills,
                    true,
                    null
            );
        } else {
            this.pendingMilestoneTransition = false;
        }

        grantArtifactRewards();
    }

    private void handleRunVictory() {
        log("Ending successful run and ejecting players.");
        broadcastEventTitle("TOWER CONQUERED", "You have beaten the Slay the Tower!", true, null);
        ejectPlayersFromInstanceAndDestroy();
    }

    private void grantArtifactRewards() {
        log("Granting artifact rewards UI to all players.");
        broadcastEventTitle("ROOM CLEAR", "Select your reward...", false, null);

        for (PlayerRef player : activeParticipants) {
            ArtefactSelection ui = new ArtefactSelection(player, world.getEntityStore().getStore());
            ui.buildPage();
        }
    }

    public void grantSkillRewards(PlayerRef playerRef) {
        if (!canPlayerPickSkill(playerRef)) {
            log("Player " + playerRef.getUsername() + " attempted to claim a skill, but is already at their cap.");
            playerRef.sendMessage(Message.raw("You have already remembered all you can right now..."));
            return;
        }

        log("Granting skill rewards UI to : " + playerRef.getUsername());

        EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                Message.raw("Choose a skill!"),
                Message.raw("The statue helps you remember training.."),
                false,
                null,
                2.0F, 0.5F, 0.5F
        );

        List<Item> skillsToOffer = generateSkillOptions(playerRef);

        SkillSelection ui = new SkillSelection(playerRef, world.getEntityStore().getStore());
        ui.buildPageWithList(skillsToOffer);
    }

    private List<Item> generateSkillOptions(PlayerRef playerRef) {
        DefaultAssetMap<String, Item> itemMap = Item.getAssetMap();

        String playerClass = playerClasses.getOrDefault(playerRef.getUuid(), "Dagger");
        String tagToSearch = "HotbarClass=" + playerClass;

        int tagIndex = AssetRegistry.getOrCreateTagIndex(tagToSearch);
        Set<String> validKeys = itemMap.getKeysForTag(tagIndex);
        if (validKeys == null || validKeys.isEmpty()) {
            log("WARNING: No skills found in Asset Store with tag: " + tagToSearch);
            return Collections.emptyList();
        }

        Set<String> ownedSkills = playerOwnedSkills.getOrDefault(playerRef.getUuid(), Collections.emptySet());

        List<Item> options = new ArrayList<>();
        for (String key : validKeys) {
            if (!ownedSkills.contains(key)) {
                Item item = itemMap.getAsset(key);
                if (item != null) {
                    options.add(item);
                }
            }
        }

        Collections.shuffle(options);

        if (options.size() > 3) {
            return options.subList(0, 3);
        }
        return options;
    }

    public void onPlayerClaimedSkillReward(PlayerRef playerRef, String claimedSkillId) {
        if (!activeParticipants.contains(playerRef)) return;
        if (!canPlayerPickSkill(playerRef)) {
            log("Player " + playerRef.getUsername() + " attempted to claim a skill, but is already at their cap.");
            return;
        }

        incrementPlayerSkillCount(playerRef);

        playerOwnedSkills.computeIfAbsent(playerRef.getUuid(), k -> new HashSet<>()).add(claimedSkillId);

        playerRef.sendMessage(Message.raw("Skill acquired!"));
        log(playerRef.getUsername() + " successfully claimed a skill reward: " + claimedSkillId);
    }

    public void onPlayerClaimedArtefactReward(PlayerRef playerRef, String artefactId) {
        if (!activeParticipants.contains(playerRef) || playersReady.contains(playerRef)) return;

        playerOwnedArtefacts.computeIfAbsent(playerRef.getUuid(), k -> new ArrayList<>()).add(artefactId);

        log("Reward claimed by: " + playerRef.getUsername() + " (Artefact: " + artefactId + ")");
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
            if (canPlayerPickSkill(p)) {
                unpreparedPlayers.add(p.getUsername());
            }
        }

        if (!unpreparedPlayers.isEmpty()) {
            String names = String.join(", ", unpreparedPlayers);
            log("Start aborted. Unprepared players: " + names);
            broadcastMessage("Cannot start yet! Waiting for players to select their skills: " + names + ". They should go to the statue.");
            return;
        }

        log("All players ready. Teleporting to first room.");
        tpParticipantsToRoom();
    }

    private void transitionToLobby() {
        log("Moving party to Lobby.");
        completeSharedRoomObjective();
        this.participantsInRoom.clear();
        cleanRoom(currentRoomMobs);
        cleanRoom(futureRoomMobs);

        tpParticipantsToLobby();
        initializeMilestone();
        this.pendingMilestoneTransition = false;
    }

    private void handleStageFailure() {
        log("CRITICAL: Party Fall. Resetting milestone progress.");
        completeSharedRoomObjective();
        broadcastEventTitle("DEFEATED", "The tower claims another soul...", true, null);


        ejectPlayersFromInstanceAndDestroy();
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

        if (this.lobby != null) {
            log("Teleporting participants to Lobby asset: " + lobby.worldName);
            for (PlayerRef participant : activeParticipants) {
                RoomTeleporter.teleportToRoom(participant, lobby, this.world);
            }
        }

        setLobbyWeather();
    }

    private void tpParticipantsToRoom() {
        if (currentRoom == null) return;
        log("Teleporting participants to Room asset: " + currentRoom.left().worldName);

        List<PlayerRef> playersToTeleport = new ArrayList<>();
        for (PlayerRef p : activeParticipants) {
            if (!participantsInRoom.contains(p)) {
                playersToTeleport.add(p);
                this.participantsInRoom.add(p);
                startSharedRoomObjective();
                setRandomRoomWeather();

            }
        }

        RoomTeleporter.teleportGroupToRoom(playersToTeleport, currentRoom.left(), this.world);

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

    private void ejectPlayersFromInstanceAndDestroy() {
        world.execute(() -> {
            for (PlayerRef p : activeParticipants) {
                Ref<EntityStore> ref = p.getReference();
                if (ref != null && ref.isValid()) {
                    Store<EntityStore> store = ref.getStore();
                    try {
                        InstancesPlugin.exitInstance(ref, store);
                        log("Successfully ejected " + p.getUsername());
                    } catch (Exception e) {
                        log("Failed to exit instance for player: " + p.getUsername());
                    }
                }

            }
            activeParticipants.clear();
            GamePlugin.get().destroyGameInstance(this.world);
        });
    }

    public World getWorld() {
        return this.world;
    }

    private void setForcedWeather(@Nullable String forcedWeather, ComponentAccessor<EntityStore> componentAccessor) {
        WeatherResource weatherResource = (WeatherResource) componentAccessor.getResource(WeatherResource.getResourceType());
        weatherResource.setForcedWeather(forcedWeather);
        WorldConfig config = this.world.getWorldConfig();
        config.setForcedWeather(forcedWeather);
        config.markChanged();
    }

    private void setRandomRoomWeather() {
        String randomWeather = RANDOM_WEATHERS.get(ThreadLocalRandom.current().nextInt(RANDOM_WEATHERS.size()));
        setForcedWeather(randomWeather, world.getEntityStore().getStore());
        log("Weather changed to random state: " + randomWeather);
    }

    private void setLobbyWeather() {
        setForcedWeather("Weather_Prison", world.getEntityStore().getStore());
        log("Weather changed to Prison state for the Lobby.");
    }

    private void startSharedRoomObjective() {
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
            updateSharedRoomObjective();
            log("Started shared UI objective for party: " + currentRoomObjectiveId);
        }
    }

    private void updateSharedRoomObjective() {
        if (currentRoomObjectiveId == null || currentRoom == null || currentRoom.right() == null) return;

        Objective obj = ObjectivePlugin.get().getObjectiveDataStore().getObjective(currentRoomObjectiveId);
        if (obj != null && obj.getCurrentTasks() != null && obj.getCurrentTasks().length > 0) {
            ObjectiveTask task = obj.getCurrentTasks()[0];

            if (task instanceof CustomRoomTask customTask) {
                int deadMobs = currentRoom.right().getDeadMobs();
                int totalMobs = currentRoom.right().getSpawnedMobs();

                customTask.setProgress(deadMobs, totalMobs);
                customTask.sendUpdateObjectiveTaskPacket(obj);
            }
        }
    }

    private void completeSharedRoomObjective() {
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

    private void teleportPlayerToSpawn(PlayerRef playerRef) {
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                Transform spawn = world.getWorldConfig().getSpawnProvider().getSpawnPoint(world, playerRef.getUuid());

                if (spawn != null) {
                    world.execute(() -> {
                        Teleport teleportComponent = Teleport.createForPlayer(world, spawn);
                        store.addComponent(ref, Teleport.getComponentType(), teleportComponent);
                        log("Teleported " + playerRef.getUsername() + " to world spawn.");
                    });
                }
            }
        });
    }

    private void forceSurvivalMode(PlayerRef playerRef) {
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();

                Player.setGameMode(ref, GameMode.Adventure, store);
                log("Forced " + playerRef.getUsername() + " into Survival mode.");
            }
        });
    }

    private void cleanupLeftoverObjectives() {
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