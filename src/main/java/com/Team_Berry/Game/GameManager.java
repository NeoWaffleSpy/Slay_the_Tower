package com.Team_Berry.Game;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.Artefacts.UI.ArtefactSelection;
import com.Team_Berry.Artefacts.UI.SkillSelection;
import com.Team_Berry.Artefacts.UI.WeaponSelection;
import com.Team_Berry.Game.Data.GameState;
import com.Team_Berry.Game.Data.Quest;
import com.Team_Berry.Game.Enums.EndStageResult;
import com.Team_Berry.Game.Enums.QuestUpdate;
import com.Team_Berry.Game.Objectives.CustomRoomTask;
import com.Team_Berry.Game.Utils.PlayerInventory;
import com.Team_Berry.Rooms.Codecs.MobGroupCodec;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;
import com.Team_Berry.Rooms.Utils.RoomNPCSpawner;
import com.Team_Berry.Rooms.Utils.RoomTeleporter;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
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
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class GameManager {
    private static final KeyedScheduler scheduler = new KeyedScheduler();
    private static final String LOBBY_HEAL_ITEM = "Life";
    private static final String invisibleHpArtefact = "Invisible_Hp_Artefact";
    private static final List<String> RANDOM_WEATHERS = Arrays.asList(
            "Weather_Red", "Weather_Blue", "Weather_Purple", "Weather_Green"
    );
    private static final String MOB_SEARCH_EFFECT = "Mob_Search_Effect";
    private static final String SFX_ROOM_START = "SFX_Room_Start";
    private static final String RELICS_CHEST = "Relics_Chest";
    private static final int TELEPORT_DELAY = 7;
    private static final List<String> STARTING_MODELS = Arrays.asList(
            "Skeleton", "Skeleton_Fighter", "Skeleton_Mage", "Skeleton_Pirate_Striker", "Skeleton_Knight"
    );
    private static final String ROOM_MUSIC = "Room_Music";
    private static final String TREE_MUSIC = "Tree_Music";
    private final GameState gameState;
    private final RoomCodec lobby;
    private final RoomCodec postgameRoom;
    private final RoomCodec prisonSpawnRoom;
    private final Set<PlayerRef> activeParticipants = new HashSet<>();
    private final Set<PlayerRef> deadParticipants = new HashSet<>();
    private final Set<PlayerRef> participantsInRoom = new HashSet<>();
    private final Set<UUID> claimedLobbyHeal = new HashSet<>();
    private final Map<UUID, Integer> historicalSkillCounts = new HashMap<>();
    private final Map<UUID, String> playerClasses = new HashMap<>();
    private final Map<UUID, Set<String>> playerOwnedSkills = new HashMap<>();
    private final Map<UUID, List<String>> playerOwnedArtefacts = new HashMap<>();
    private final Set<PlayerRef> playersReady = new HashSet<>();
    private final Set<UUID> currentRoomMobs = new HashSet<>();
    private final Set<UUID> futureRoomMobs = new HashSet<>();
    private final Map<UUID, Set<BlockPosition>> playerClaimedChests = new HashMap<>();
    private final Map<UUID, BlockPosition> pendingChestClaims = new HashMap<>();
    private final Set<UUID> pendingCleanup = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean isBossStage = false;
    private World world;
    private UUID currentRoomObjectiveId = null;
    private boolean pendingMilestoneTransition = false;
    private boolean pendingSkillBroadcast = false;
    private Pair<RoomCodec, Quest> currentRoom;
    private Pair<RoomCodec, Quest> futureRoom;
    private int globalMaxSkills = 1;
    private boolean statueSpawn = false;
    private boolean searchEffectApplied = false;
    private boolean runEnded = false;

    public GameManager(World world, SkillMilestoneCodec milestoneData) {
        this.world = world;
        this.postgameRoom = findPostgameRoom();
        this.gameState = new GameState();
        this.gameState.initialize(milestoneData);
        this.lobby = findLobbyRoom();
        this.prisonSpawnRoom = findPrisonSpawnRoom();
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

        this.searchEffectApplied = false;

        if (!currentRoomMobs.isEmpty()) {
            log("Cleaning up leftover current room mobs...");
            cleanRoom(currentRoomMobs);
        }

        if (!futureRoomMobs.isEmpty()) {
            log("Cleaning up old future room mobs before milestone initialization...");
            cleanRoom(futureRoomMobs);
        }

        if (this.gameState.isRunComplete()) {
            this.isBossStage = true;
            this.currentRoom = prepareBossRoom(currentRoomMobs);
            this.futureRoom = null;
            log("Boss Buffer Ready. Current: " + (currentRoom != null ? currentRoom.left().worldName : "ERROR"));
        } else {
            this.currentRoom = prepareRoom(null, currentRoomMobs);
            this.futureRoom = prepareRoom(this.currentRoom.left(), futureRoomMobs);
            log(String.format("Buffer Ready. Current: %s | Future: %s",
                    currentRoom.left().worldName, futureRoom.left().worldName));
        }
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
            setupRoomChests(selectedRoom);


        } else {
            quest = new Quest(0);
            log("Room selected: " + selectedRoom.worldName + " (Empty: No mob groups found).");
        }

        log("Room preparation complete.");
        return Pair.of(selectedRoom, quest);
    }

    public void cleanRoom(Set<UUID> mobTracker) {
        if (mobTracker.isEmpty()) return;

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Set<UUID> foundMobs = new HashSet<>();

            store.forEachChunk(UUIDComponent.getComponentType(), (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    UUIDComponent uuidComp = (UUIDComponent) chunk.getComponent(i, UUIDComponent.getComponentType());

                    if (uuidComp != null && mobTracker.contains(uuidComp.getUuid())) {
                        commandBuffer.removeEntity(chunk.getReferenceTo(i), RemoveReason.REMOVE);
                        foundMobs.add(uuidComp.getUuid());
                    }
                }
            });

            for (UUID id : mobTracker) {
                if (!foundMobs.contains(id)) {
                    pendingCleanup.add(id);
                }
            }

            log("Room cleaned. " + foundMobs.size() + " active mobs evaporated. " +
                    (mobTracker.size() - foundMobs.size()) + " asleep mobs sent to pending cleanup.");

            mobTracker.clear();
        });
    }

    private void processPendingCleanup() {
        if (pendingCleanup.isEmpty()) return;

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Set<UUID> foundMobs = new HashSet<>();

            store.forEachChunk(UUIDComponent.getComponentType(), (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    UUIDComponent uuidComp = (UUIDComponent) chunk.getComponent(i, UUIDComponent.getComponentType());

                    if (uuidComp != null && pendingCleanup.contains(uuidComp.getUuid())) {
                        commandBuffer.removeEntity(chunk.getReferenceTo(i), RemoveReason.REMOVE);
                        foundMobs.add(uuidComp.getUuid());
                    }
                }
            });

            for (UUID id : foundMobs) {
                pendingCleanup.remove(id);
            }

            if (!foundMobs.isEmpty()) {
                log("Late Cleanup: Successfully  removed " + foundMobs.size() + " sleeping mobs that just loaded in.");
            }
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
        log("Queueing tower advance in " + TELEPORT_DELAY + " seconds...");
        setForcedWeather("Weather_Transition", world.getEntityStore().getStore());

        scheduler.schedule("advance_room_" + world.getName(), () -> {
            world.execute(() -> {
                log("Advancing tower. Shifting buffer...");
                this.participantsInRoom.clear();
                cleanRoom(currentRoomMobs);

                this.searchEffectApplied = false;

                this.currentRoom = this.futureRoom;

                this.currentRoomMobs.clear();
                this.currentRoomMobs.addAll(this.futureRoomMobs);
                this.futureRoomMobs.clear();

                resetClaimedChests();

                tpParticipantsToRoom();

                this.futureRoom = prepareRoom(this.currentRoom.left(), futureRoomMobs);
                log("Advance complete. Current Room is now: " + currentRoom.left().worldName);
            });
        }, TELEPORT_DELAY, TimeUnit.SECONDS);
    }

    public void addAllPresentPlayers() {
        world.execute(() -> {
            log("Scanning world for already-connected players...");

            // Use the non-deprecated getPlayerRefs() directly!
            for (PlayerRef playerRef : world.getPlayerRefs()) {
                if (playerRef != null && playerRef.getReference() != null && playerRef.getReference().isValid()) {
                    addParticipant(playerRef);
                }
            }
        });
    }

    public void addParticipant(PlayerRef playerRef) {
        log("Player joined the party: " + playerRef.getUsername());
        if (this.activeParticipants.add(playerRef)) {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref.getStore();
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());

            teleportPlayerToSpawn(playerRef);
            forceSurvivalMode(playerRef);
            applyRandomStartingModel(playerRef);
        }

        if (!playerOwnedArtefacts.containsKey(playerRef.getUuid())) {

            playerOwnedArtefacts.put(playerRef.getUuid(), new ArrayList<>(List.of(invisibleHpArtefact)));
            log("Granted starting Invisible_Hp_Artefact to new participant: " + playerRef.getUsername());
        }

        restorePlayerArtefacts(playerRef);


        if (this.historicalSkillCounts.putIfAbsent(playerRef.getUuid(), 0) == null) {
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw("Save the kweebecs !"),
                    Message.raw("Good luck, adventurer..."),
                    true
            );
        }

        scheduler.schedule("tp_after_weapon_" + world.getName(), () -> {
            world.execute(() -> {
                healPlayerToFull(playerRef);
            });
        }, 500, TimeUnit.MILLISECONDS);

    }

    private void restorePlayerArtefacts(PlayerRef playerRef) {
        if (playerOwnedArtefacts.containsKey(playerRef.getUuid())) {
            StatEffectComponent statComp = StatEffectComponent.getPlayerStatComp(playerRef);
            if (statComp != null) {

                DefaultAssetMap<String, ArtefactCodec> artefactMap = ArtefactCodec.getAssetMap();
                statComp.addStackToArtifact(artefactMap.getAsset(invisibleHpArtefact), 0);
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

        log("Player left the party: " + playerRef.getUsername());

        world.execute(() -> {
            UUID leavingId = playerRef.getUuid();
            for (PlayerRef otherPlayer : activeParticipants) {
                if (!otherPlayer.equals(playerRef)) {
                    otherPlayer.getHiddenPlayersManager().showPlayer(leavingId);
                    playerRef.getHiddenPlayersManager().showPlayer(otherPlayer.getUuid());
                }
            }
        });
        resetPlayerModel(playerRef);
        detachPlayerFromObjective(playerRef);
        this.activeParticipants.remove(playerRef);
        this.participantsInRoom.remove(playerRef);
        this.deadParticipants.remove(playerRef);
        // this.playersReady.remove(playerRef);

        if (!activeParticipants.isEmpty() && playersReady.size() >= activeParticipants.size()) {
            log("Final undecided player left. Force-completing reward phase.");
            completeRewardPhase();
        }
    }

    private void detachPlayerFromObjective(PlayerRef playerRef) {
        if (currentRoomObjectiveId != null) {
            ObjectivePlugin.get().removePlayerFromExistingObjective(
                    world.getEntityStore().getStore(),
                    playerRef.getUuid(),
                    currentRoomObjectiveId
            );
            log("Detached objective from player: " + playerRef.getUsername());
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
        Quest currentQuest = currentRoom.right();

        if (questUpdate == QuestUpdate.MOB_DEATH && deadMobId != null) {

            boolean isQuestMob = false;

            if (currentRoomMobs.remove(deadMobId)) {
                currentQuest.incrementDeadMobs();
                log(String.format("Current Room Mob Death. Progress: %d/%d", currentQuest.getDeadMobs(), currentQuest.getSpawnedMobs()));
                isQuestMob = true;
            } else if (futureRoom != null && futureRoom.right() != null && futureRoomMobs.remove(deadMobId)) {
                futureRoom.right().incrementDeadMobs();
                log("A Future Room Mob died prematurely! Lowered future quest requirements.");
            }


            if (isQuestMob) {

                validateRemainingMobs();
                updateSharedRoomObjective();

                if (!searchEffectApplied && currentQuest.getMobsLeft() <= (currentQuest.getSpawnedMobs() / 3)) {
                    searchEffectApplied = true;
                    broadcastMessage("The remaining monsters have been revealed!");
                    applyEffectToMobs(currentRoomMobs, MOB_SEARCH_EFFECT);
                }

                if (currentQuest.isComplete()) {
                    log("Room Quest successfully completed.");
                    endStage(EndStageResult.SUCCESS);
                }
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

        if (this.isBossStage) {
            log("VICTORY! The Boss has been defeated.");
            handleRunVictory();
            return;
        }

        SkillMilestoneCodec.MilestoneEntry oldMilestone = this.gameState.getCurrentMilestone();
        this.gameState.incrementClearedStages();
        SkillMilestoneCodec.MilestoneEntry newMilestone = this.gameState.getCurrentMilestone();

        reviveDeadPlayers();

        if (this.gameState.isRunComplete()) {
            log("FINAL MILESTONE REACHED. Preparing for the Boss!");
            this.pendingMilestoneTransition = true;
        } else if (oldMilestone != newMilestone) {
            this.pendingMilestoneTransition = true;

            if (this.globalMaxSkills < 4) {
                this.globalMaxSkills++;
                this.pendingSkillBroadcast = true;
                log("MILESTONE REACHED. Skill capacity increased to: " + globalMaxSkills);
            } else {
                log("MILESTONE REACHED. Skill capacity is already at max (4).");
            }
        } else {
            this.pendingMilestoneTransition = false;
        }

        completeRewardPhase();
    }

    private void handleRunVictory() {
        log("Ending successful run and ejecting players.");
        broadcastEventTitle("MISSION COMPLETE !", "You have saved the kweebecs !", true, null);
        //ejectPlayersFromInstanceAndDestroy();
        this.runEnded = true;
        tpParticipantsToPostgame();
    }

//    private void grantArtifactRewards() {
//        log("Granting artifact rewards UI to all players.");
//        broadcastEventTitle("ROOM CLEAR", "Select your reward...", false, null);
//
//        for (PlayerRef player : activeParticipants) {
//            ArtefactSelection ui = new ArtefactSelection(player, world.getEntityStore().getStore());
//            ui.buildPage();
//        }
//    }

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

        playerRef.sendMessage(Message.raw("You remembered a part of yourself!"));
        log(playerRef.getUsername() + " successfully claimed a skill reward: " + claimedSkillId);
        int currentSkills = historicalSkillCounts.getOrDefault(playerRef.getUuid(), 0);
        if (currentSkills >= 4) {
            resetPlayerModel(playerRef);
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw("HUMANITY REGAINED !"),
                    Message.raw("Your true form has been restored."),
                    true,
                    null,
                    2.0F, 0.5F, 0.5F
            );
        }
    }

    public void onPlayerClaimedArtefactReward(PlayerRef playerRef, String artefactId) {
        if (!activeParticipants.contains(playerRef)) return;

        playerOwnedArtefacts.computeIfAbsent(playerRef.getUuid(), k -> new ArrayList<>()).add(artefactId);

        BlockPosition pendingPos = pendingChestClaims.remove(playerRef.getUuid());
        if (pendingPos != null) {
            playerClaimedChests.computeIfAbsent(playerRef.getUuid(), k -> new HashSet<>()).add(pendingPos);
            log("Reward claimed by: " + playerRef.getUsername() + " (Artefact: " + artefactId + ") from chest.");
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
        log("Queueing transition in " + TELEPORT_DELAY + " seconds...");
        setForcedWeather("Weather_Transition", world.getEntityStore().getStore());

        scheduler.schedule("tp_lobby_" + world.getName(), () -> {
            world.execute(() -> {
                completeSharedRoomObjective();
                this.participantsInRoom.clear();
                cleanRoom(currentRoomMobs);
                cleanRoom(futureRoomMobs);

                this.searchEffectApplied = false;
                resetClaimedChests();

                if (runEnded) {
                    log("Run has ended. Moving party to Postgame Room and queueing world destruction.");
                    tpParticipantsToPostgame();

                } else {
                    log("Moving party to Lobby.");
                    tpParticipantsToLobby();
                    initializeMilestone();
                    this.pendingMilestoneTransition = false;

                    if (this.pendingSkillBroadcast) {
                        broadcastEventTitle(
                                "MILESTONE REACHED",
                                "Skill capacity increased to " + globalMaxSkills,
                                true,
                                null
                        );
                        this.pendingSkillBroadcast = false;
                    }
                }
            });
        }, TELEPORT_DELAY, TimeUnit.SECONDS);
    }

    private void handleStageFailure() {
        log("CRITICAL: Party Fall. Resetting milestone progress.");
        completeSharedRoomObjective();

        broadcastEventTitle("DEFEATED", "The kweebecs still need you... Try again !", true, null);


        //ejectPlayersFromInstanceAndDestroy();
        this.runEnded = true;
        tpParticipantsToPostgame();
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
            claimedLobbyHeal.clear();

            log("Teleporting participants to Lobby asset: " + lobby.worldName);
            world.execute(() -> {
                for (PlayerRef participant : activeParticipants) {
                    RoomTeleporter.teleportToRoom(participant, lobby, this.world);
                    healPlayerToFull(participant);

                    for (PlayerRef other : activeParticipants) {
                        if (!participant.equals(other)) {
                            participant.getHiddenPlayersManager().showPlayer(other.getUuid());
                        }
                    }
                }
            });
        }


        setLobbyWeather();
    }

    private void tpParticipantsToRoom() {
        if (currentRoom == null) return;

        if (isBossStage) {
            log("Teleporting participants to Boss Room asset: " + currentRoom.left().worldName + " <<<");
        } else {
            log("Teleporting participants to Room asset: " + currentRoom.left().worldName);
        }

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

        scheduler.schedule("late_cleanup_" + world.getName(), this::processPendingCleanup, 1, TimeUnit.SECONDS);
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
        GamePlugin.LOGGER.atInfo().log("Looking for Mob Groups with tag: " + tagSearch);
        int tagIndex = AssetRegistry.getOrCreateTagIndex(tagSearch);
        Set<String> groupKeys = map.getKeysForTag(tagIndex);

        if (groupKeys == null || groupKeys.isEmpty()) return null;

        List<MobGroupCodec> validGroups = new ArrayList<>();
        for (String key : groupKeys) {
            MobGroupCodec group = map.getAsset(key);
            if (group != null) {
                validGroups.add(group);
                GamePlugin.LOGGER.atInfo().log("Pulled group: " + key);
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

    private RoomCodec findPostgameRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Postgame");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) {
            log("Warning: No Postgame room found with tag 'Category=Postgame'.");
            return null;
        }

        List<String> keyList = new ArrayList<>(roomKeys);
        String randomKey = keyList.get(ThreadLocalRandom.current().nextInt(keyList.size()));

        return roomMap.getAsset(randomKey);
    }

    private RoomCodec findPrisonSpawnRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Prisonspawn");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) {
            log("Warning: No Prisonspawn room found with tag 'Category=Prisonspawn'.");
            return null;
        }

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
            } else {
                teleportToSpectatorPosition(playerRef);
                hidePlayer(playerRef);
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
        showAllDeadPlayers();
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
        setForcedWeather("Weather_Transition", world.getEntityStore().getStore());
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
                if (totalMobs <= 0) {
                    totalMobs = 1;
                }
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

    private void applyEffectToMobs(Set<UUID> mobTracker, String effectId) {
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);

            if (effect == null) {
                log("Warning: Cannot apply effect! Asset not found: " + effectId);
                return;
            }

            int appliedCount = 0;
            for (UUID mobId : mobTracker) {
                Ref<EntityStore> ref = getRefByUUID(store, mobId);
                if (ref != null && ref.isValid()) {
                    EffectControllerComponent effectController = store.getComponent(ref, EffectControllerComponent.getComponentType());

                    if (effectController != null) {
                        effectController.addEffect(ref, effect, store);
                        appliedCount++;
                    }
                }
            }
            log("Applied " + effectId + " to " + appliedCount + " mobs.");
        });
    }

//    private void playSoundToPlayers(List<PlayerRef> players, String soundEventId) {
//        int soundIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
//
//        if (soundIndex == 0) {
//            log("Warning: Sound ID not found: " + soundEventId);
//            return;
//        }
//
//        for (PlayerRef p : players) {
//            SoundUtil.playSoundEvent2dToPlayer(p, soundIndex, SoundCategory.SFX);
//        }
//        log("Played Sound");
//    }

    public void playerChestClaim(BlockPosition pos, PlayerRef playerRef) {
        if (!activeParticipants.contains(playerRef)) return;

        UUID playerId = playerRef.getUuid();
        Set<BlockPosition> claimedChests = playerClaimedChests.getOrDefault(playerId, Collections.emptySet());

        if (claimedChests.contains(pos)) {
            playerRef.sendMessage(Message.raw("You have already looted this chest!"));
            return;
        }

        pendingChestClaims.put(playerId, pos.clone());
        log(playerRef.getUsername() + " is viewing chest loot at " + pos.x + ", " + pos.y + ", " + pos.z);

        EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                Message.raw("Chest Looted!"),
                Message.raw("Select your reward..."),
                false,
                null,
                2.0F, 0.5F, 0.5F
        );

        ArtefactSelection ui = new ArtefactSelection(playerRef, world.getEntityStore().getStore());
        ui.buildPage();
    }

    private void resetClaimedChests() {
        this.playerClaimedChests.clear();
        this.pendingChestClaims.clear();
        log("Reset chest claim and pending histories for the new room.");
    }

    public String getParticipantIndices(Collection<PlayerRef> activeParticipants) {
        return activeParticipants.stream()
                .filter(p -> p.getReference() != null && p.getReference().isValid())
                .map(p -> p.getUsername() + " " + p.getReference().getIndex())
                .collect(Collectors.joining("\n"));
    }

    public void breakBlockAt(BlockPosition pos) {
        if (pos == null || world == null) return;

        world.execute(() -> {
            boolean success = world.breakBlock(pos.x, pos.y, pos.z, 0);

            if (success) {
                log("Successfully broke the block at " + pos.x + ", " + pos.y + ", " + pos.z);
            } else {
                log("Failed to break block at " + pos.x + ", " + pos.y + ", " + pos.z + ". It may already be empty.");
            }
        });
    }

    public void setupRoomChests(RoomCodec room) {
        if (room == null || room.chestPositions == null || room.chestPositions.length == 0) {
            return;
        }

        world.execute(() -> {

            int restoredCount = 0;
            for (BlockPosition pos : room.chestPositions) {
                BlockType currentBlock = world.getBlockType(pos.x, pos.y, pos.z);

                if (currentBlock == null || !currentBlock.getId().equals(RELICS_CHEST)) {
                    world.setBlock(pos.x, pos.y, pos.z, RELICS_CHEST);
                    restoredCount++;
                }
            }

            if (restoredCount > 0) {
                log("Restored " + restoredCount + " missing " + RELICS_CHEST + "(s) in room " + room.getId());
            }

            List<BlockPosition> validChests = new ArrayList<>(Arrays.asList(room.chestPositions));
            Collections.shuffle(validChests);

            int amountToBreak = validChests.size() / 2;

            for (int i = 0; i < amountToBreak; i++) {
                BlockPosition pos = validChests.get(i);

                boolean success = world.breakBlock(pos.x, pos.y, pos.z, 0);
                if (success) {
                    log("Successfully broke the block at " + pos.x + ", " + pos.y + ", " + pos.z);
                }
            }

            log(String.format("Chest RNG: Setup complete. Removed %d '%s'(s) in room '%s'.",
                    amountToBreak, RELICS_CHEST, room.getId()));
        });
    }

    public void randomizeRoomChests(RoomCodec room) {
        if (room == null || room.chestPositions == null || room.chestPositions.length == 0) {
            return;
        }

        world.execute(() -> {
            List<BlockPosition> validChests = new ArrayList<>();

            for (BlockPosition pos : room.chestPositions) {
                BlockType blockType = world.getBlockType(pos.x, pos.y, pos.z);

                if (blockType != null && blockType.getId().equals(RELICS_CHEST)) {
                    validChests.add(pos);
                } else {
                    String actualId = (blockType != null) ? blockType.getId().toString() : "Empty/Air";
                    log(String.format("Position %d, %d, %d is NOT a '%s'! (Found: %s)",
                            pos.x, pos.y, pos.z, RELICS_CHEST, actualId));
                }
            }

            log(String.format("Chest RNG: Found a total of %d '%s'(s) in room '%s'.",
                    validChests.size(), RELICS_CHEST, room.getId()));

            if (!validChests.isEmpty()) {
                Collections.shuffle(validChests);
                int amountToBreak = validChests.size() / 2;

                for (int i = 0; i < amountToBreak; i++) {
                    BlockPosition pos = validChests.get(i);
                    breakBlockAt(pos);
                }

                log(String.format("Chest RNG: Queued removal of %d '%s'(s).", amountToBreak, RELICS_CHEST));
            }
        });
    }

    public void restoreRoomChests(RoomCodec room) {
        if (room == null || room.chestPositions == null || room.chestPositions.length == 0) {
            return;
        }

        world.execute(() -> {


            int restoredCount = 0;
            for (BlockPosition pos : room.chestPositions) {
                BlockType currentBlock = world.getBlockType(pos.x, pos.y, pos.z);

                if (currentBlock == null || !currentBlock.getId().equals(RELICS_CHEST)) {
                    world.setBlock(pos.x, pos.y, pos.z, RELICS_CHEST);
                    restoredCount++;
                }
            }

            if (restoredCount > 0) {
                log("Restored " + restoredCount + " missing " + RELICS_CHEST + "(s) in room " + room.getId());
            }
        });
    }

    private void teleportToSpectatorPosition(PlayerRef playerRef) {
        if (currentRoom == null || currentRoom.left() == null || currentRoom.left().spectatePosition == null) {
            log("Warning: No spectate position defined for the current room.");
            return;
        }

        Position pos = currentRoom.left().spectatePosition;
        Transform spectateTransform = new Transform(new Vector3d(pos.x, pos.y, pos.z));

        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                Teleport teleportComponent = Teleport.createForPlayer(world, spectateTransform);
                store.addComponent(ref, Teleport.getComponentType(), teleportComponent);
                log("Teleported spectator " + playerRef.getUsername() + " to the spectate position.");
            }
        });
    }

    private void hidePlayer(PlayerRef deadPlayer) {
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

    private void showAllDeadPlayers() {
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

    private void tpParticipantsToPostgame() {
        showAllDeadPlayers();

        world.execute(() -> {
            if (this.postgameRoom != null) {
                log("Teleporting participants to Postgame asset: " + postgameRoom.worldName);
                for (PlayerRef participant : activeParticipants) {

                    Ref<EntityStore> ref = participant.getReference();
                    if (ref != null && ref.isValid()) {
                        resetPlayerModel(participant);
                        PlayerInventory.clearPlayerInventory(ref, ref.getStore());
                        log("Cleared end-of-run inventory for: " + participant.getUsername());
                    }

                    RoomTeleporter.teleportToRoom(participant, postgameRoom, this.world);
                }
            } else {
                log("Error: No postgame room defined! Falling back to immediate instance ejection.");
                ejectPlayersFromInstanceAndDestroy();
            }
        });
        GamePlugin.get().destroyGameInstance(this.world);
    }

    public void startingKweebecInteraction(PlayerRef playerRef) {
        if (!activeParticipants.contains(playerRef)) return;

        world.execute(() -> {
            if (playerClasses.containsKey(playerRef.getUuid())) {
                log(playerRef.getUsername() + " already has a weapon class. Teleporting to Lobby.");
                if (this.lobby != null) {
                    RoomTeleporter.teleportToRoom(playerRef, this.prisonSpawnRoom, this.world);
                    setLobbyWeather();
                }
            } else {
                log(playerRef.getUsername() + " has no weapon. Opening Weapon Selection UI.");
                WeaponSelection ui = new WeaponSelection(playerRef, world.getEntityStore().getStore());
                ui.buildPage();
            }
        });
    }

    public void onPlayerSelectedWeapon(PlayerRef playerRef, String weaponClassId) {
        if (!activeParticipants.contains(playerRef)) return;

        setPlayerClass(playerRef, weaponClassId);
        playerRef.sendMessage(Message.raw("Weapon acquired!"));
        log(playerRef.getUsername() + " selected weapon: " + weaponClassId + ". Teleporting to Prisonspawn.");

        world.execute(() -> {
            if (this.prisonSpawnRoom != null) {
                scheduler.schedule("tp_after_weapon_" + world.getName(), () -> {
                    world.execute(() -> {
                        RoomTeleporter.teleportToRoom(playerRef, this.prisonSpawnRoom, this.world);
                        EventTitleUtil.showEventTitleToPlayer(
                                playerRef,
                                Message.raw("Save the kweebecs !"),
                                Message.raw("Good luck, adventurer..."),
                                true
                        );
                    });
                }, 1000, TimeUnit.MILLISECONDS);

            } else {
                log("Error: Prisonspawn room not found!");
            }
        });
    }

    private void healPlayerToFull(PlayerRef playerRef) {
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());

                if (stats != null) {
                    stats.setStatValue(DefaultEntityStatTypes.getHealth(), stats.get(DefaultEntityStatTypes.getHealth()).getMax());
                    log("Healed " + playerRef.getUsername() + " to full health.");
                }
            }
        });
    }

    private void validateRemainingMobs() {
        if (currentRoomMobs.isEmpty()) return;

        Store<EntityStore> store = world.getEntityStore().getStore();
        List<UUID> vanishedIds = new ArrayList<>();

        for (UUID mobId : currentRoomMobs) {
            Ref<EntityStore> ref = getRefByUUID(store, mobId);
            if (ref == null || !ref.isValid()) {
                vanishedIds.add(mobId);
            }
        }

        if (!vanishedIds.isEmpty()) {
            for (UUID id : vanishedIds) {
                currentRoomMobs.remove(id);
                currentRoom.right().incrementDeadMobs();
                log("Anti-Softlock: Detected and cleared vanished mob UUID: " + id);
            }
        }
    }

    private void applyRandomStartingModel(PlayerRef playerRef) {
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();

                String randomModelId = STARTING_MODELS.get(ThreadLocalRandom.current().nextInt(STARTING_MODELS.size()));

                ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(randomModelId);
                if (modelAsset != null) {
                    Model model = Model.createScaledModel(modelAsset, 1.0f);
                    store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(model));
                    log("Applied starting model '" + randomModelId + "' to " + playerRef.getUsername());
                } else {
                    log("Warning: Could not find ModelAsset for ID: " + randomModelId);
                }
            }
        });
    }

    private void resetPlayerModel(PlayerRef playerRef) {
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();

                PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
                if (skinComponent != null) {
                    Model newModel = CosmeticsModule.get().createModel(skinComponent.getPlayerSkin());
                    store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
                    skinComponent.setNetworkOutdated();


                    log("Restored original player skin for: " + playerRef.getUsername());
                }
            }
        });
    }

    public void kweebecMerchantInteraction(PlayerRef playerRef) {
        if (!activeParticipants.contains(playerRef)) return;

        UUID playerId = playerRef.getUuid();

        if (claimedLobbyHeal.contains(playerId)) {
            playerRef.sendMessage(Message.raw("My supplies are exhausted! Come back later!"));
            healPlayerToFull(playerRef);
            return;
        }

        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                Player playerComponent = store.getComponent(ref, Player.getComponentType());

                if (playerComponent != null) {
                    claimedLobbyHeal.add(playerId);

                    healPlayerToFull(playerRef);

                    com.hypixel.hytale.server.core.inventory.ItemStack stack =
                            new com.hypixel.hytale.server.core.inventory.ItemStack(LOBBY_HEAL_ITEM, 2, null);

                    com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction transaction =
                            playerComponent.giveItem(stack, ref, store);


                    playerRef.sendMessage(Message.raw("Here take some of this! On the house."));
                    log(playerRef.getUsername() + " claimed their lobby supplies.");
                }
            }
        });
    }

    private RoomCodec findBossRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Boss");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) {
            log("SEVERE: No Boss room found with tag 'Category=Boss'.");
            return null;
        }

        List<String> keyList = new ArrayList<>(roomKeys);
        return roomMap.getAsset(keyList.get(ThreadLocalRandom.current().nextInt(keyList.size())));
    }

    public List<MobGroupCodec> findBossMobGroups() {
        DefaultAssetMap<String, MobGroupCodec> map = MobGroupCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Difficulty=Boss");
        Set<String> groupKeys = map.getKeysForTag(tagIndex);

        if (groupKeys == null || groupKeys.isEmpty()) {
            log("SEVERE: No Boss mob groups found with tag 'Difficulty=Boss'.");
            return null;
        }

        List<MobGroupCodec> validGroups = new ArrayList<>();
        for (String key : groupKeys) {
            validGroups.add(map.getAsset(key));
        }
        return validGroups;
    }

    public Pair<RoomCodec, Quest> prepareBossRoom(Set<UUID> targetMobTracker) {
        log("Preparing the Final Boss Room...");
        RoomCodec bossRoom = findBossRoom();

        if (bossRoom == null) return null;

        List<MobGroupCodec> bossGroups = findBossMobGroups();
        Quest quest;

        if (bossGroups != null && !bossGroups.isEmpty()) {
            MobGroupCodec selectedGroup = bossGroups.get(ThreadLocalRandom.current().nextInt(bossGroups.size()));
            quest = new Quest(selectedGroup.getTotalMobCount());

            log(String.format("Boss Room selected: %s. Spawning %d BOSS mobs.", bossRoom.worldName, quest.getSpawnedMobs()));

            world.execute(() -> {
                List<UUID> spawnedMobs = RoomNPCSpawner.spawnMobGroup(world.getEntityStore().getStore(), bossRoom, selectedGroup);
                targetMobTracker.clear();
                targetMobTracker.addAll(spawnedMobs);
                log("Successfully registered " + spawnedMobs.size() + " Boss UUIDs.");
            });

        } else {
            quest = new Quest(0);
            log("Boss Room selected: " + bossRoom.worldName + " (Error: No boss groups found).");
        }

        return Pair.of(bossRoom, quest);
    }

}