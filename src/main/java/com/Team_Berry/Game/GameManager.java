//TODO : I plan on coming back here in the very near future to rework all of this. I'm not going to lie this is my first time doing this kind of thing and I did not know how far a game manager could go. I need to go much more horizontally to make this monster more maintanable.

package com.Team_Berry.Game;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.Artefacts.UI.WeaponSelection;
import com.Team_Berry.Game.Data.GameState;
import com.Team_Berry.Game.Data.Quest;
import com.Team_Berry.Game.Enums.EndStageResult;
import com.Team_Berry.Game.Enums.QuestUpdate;
import com.Team_Berry.Game.Managers.*;
import com.Team_Berry.Rooms.Codecs.MobGroupCodec;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig;
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
import java.util.stream.Collectors;

public class GameManager {
    private static final KeyedScheduler scheduler = new KeyedScheduler();
    private static final String LOBBY_HEAL_ITEM = "Life";
    private static final String invisibleHpArtefact = "Invisible_Hp_Artefact";
    private static final String MOB_SEARCH_EFFECT = "Mob_Search_Effect";
    private static final int TELEPORT_DELAY = 7;
    private final GameState gameState;
    private final PlayerModelManager playerModelManager;
    private final WeatherManager weatherManager;
    private final RoomManager roomManager;
    private final RoomChestsManager roomChestsManager;
    private final TeleportManager teleportManager;
    private final PlayerVisibilityManager playerVisibilityManager;
    private final RewardManager rewardManager;
    private final RoomMobManager roomMobManager;
    private final PlayerStateManager playerStateManager;
    private final ObjectiveManager objectiveManager;
    private final PartyManager partyManager;
    private final Set<UUID> claimedLobbyHeal = new HashSet<>();
    private final Set<UUID> currentRoomMobs = new HashSet<>();
    private final Set<UUID> futureRoomMobs = new HashSet<>();
    private boolean isBossStage = false;
    private final World world;
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
        this.gameState = new GameState();
        this.gameState.initialize(milestoneData);
        this.playerModelManager = new PlayerModelManager(scheduler);
        this.weatherManager = new WeatherManager(world);
        this.roomManager = new RoomManager(world);
        this.roomChestsManager = new RoomChestsManager(world);
        this.teleportManager = new TeleportManager(world);
        this.playerVisibilityManager = new PlayerVisibilityManager(world);
        this.rewardManager = new RewardManager(world);
        this.roomMobManager = new RoomMobManager(world);
        this.playerStateManager = new PlayerStateManager(world);
        this.objectiveManager = new ObjectiveManager(world);
        this.partyManager = new PartyManager();
        log("Manager initialized for world.");
    }

    public void setStatueSpawn() {
        if (!statueSpawn) {
            WorldConfig config = world.getWorldConfig();
            RoomCodec lobby = roomManager.getLobbyRoom();
            if (lobby != null && lobby.getFirstSpawnPosition() != null) {
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
        objectiveManager.cleanupLeftoverObjectives();

        this.searchEffectApplied = false;

        if (!currentRoomMobs.isEmpty()) {
            log("Cleaning up leftover current room mobs...");
            roomMobManager.cleanRoom(currentRoomMobs);
        }

        if (!futureRoomMobs.isEmpty()) {
            log("Cleaning up old future room mobs before milestone initialization...");
            roomMobManager.cleanRoom(futureRoomMobs);
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
        List<RoomCodec> validRooms = roomManager.findValidRooms();

        if (exclude != null && validRooms.size() > 1) {
            validRooms.removeIf(room -> room.equals(exclude));
        }

        if (validRooms.isEmpty()) {
            log("FAILED: No valid rooms found in AssetMap.");
            return null;
        }

        RoomCodec selectedRoom = validRooms.get(ThreadLocalRandom.current().nextInt(validRooms.size()));
        int difficulty = this.gameState.getCurrentMilestone().difficulty;
        List<MobGroupCodec> mobGroups = roomManager.findValidMobGroups(difficulty);
        Quest quest;

        if (mobGroups != null && !mobGroups.isEmpty()) {
            MobGroupCodec selectedGroup = mobGroups.get(ThreadLocalRandom.current().nextInt(mobGroups.size()));
            quest = new Quest(selectedGroup.getTotalMobCount());

            log(String.format("Room selected: %s. Spawning %d mobs (Difficulty: %d)",
                    selectedRoom.worldName, quest.getSpawnedMobs(), difficulty));

            world.execute(() -> {
                List<UUID> spawnedMobs = roomMobManager.spawnMobGroup(selectedRoom, selectedGroup);
                targetMobTracker.clear();
                targetMobTracker.addAll(spawnedMobs);
                log("Successfully registered " + spawnedMobs.size() + " mob UUIDs for room.");
            });
            roomChestsManager.setupRoomChests(selectedRoom);


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
        log("Queueing tower advance in " + TELEPORT_DELAY + " seconds...");
        weatherManager.setTransitionWeather();

        scheduler.schedule("advance_room_" + world.getName(), () -> world.execute(() -> {
            log("Advancing tower. Shifting buffer...");
            partyManager.clearParticipantsInRoom();
            roomMobManager.cleanRoom(currentRoomMobs);

            this.searchEffectApplied = false;

            this.currentRoom = this.futureRoom;

            this.currentRoomMobs.clear();
            this.currentRoomMobs.addAll(this.futureRoomMobs);
            this.futureRoomMobs.clear();

            rewardManager.resetClaimedChests();

            tpParticipantsToRoom();

            this.futureRoom = prepareRoom(this.currentRoom.left(), futureRoomMobs);
            log("Advance complete. Current Room is now: " + currentRoom.left().worldName);
        }), TELEPORT_DELAY, TimeUnit.SECONDS);
    }

    public void addAllPresentPlayers() {
        world.execute(() -> {
            log("Scanning world for already-connected players...");

            for (PlayerRef playerRef : world.getPlayerRefs()) {
                if (playerRef != null && playerRef.getReference() != null && playerRef.getReference().isValid()) {
                    addParticipant(playerRef);
                }
            }
        });
    }

    public void addParticipant(PlayerRef playerRef) {
        log("Player joined the party: " + playerRef.getUsername());
        if (partyManager.addParticipant(playerRef)) {
            playerStateManager.forceSurvivalMode(playerRef);
            playerModelManager.applyRandomStartingModel(playerRef);
        }

        rewardManager.grantStartingArtefact(playerRef, invisibleHpArtefact);
        rewardManager.restorePlayerArtefacts(playerRef, invisibleHpArtefact);


        if (rewardManager.getPlayerSkillCount(playerRef) == 0) {
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw("Save the kweebecs !"),
                    Message.raw("Good luck, adventurer..."),
                    true
            );
        }

        scheduler.schedule("tp_after_weapon_" + world.getName(), () -> playerStateManager.healPlayerToFull(playerRef), 500, TimeUnit.MILLISECONDS);

    }

    public void removeParticipant(PlayerRef playerRef) {

        log("Player left the party: " + playerRef.getUsername());

        playerModelManager.resetPlayerModel(playerRef);
        objectiveManager.detachPlayerFromObjective(playerRef, currentRoomObjectiveId);
        partyManager.removeParticipant(playerRef);

        if (!partyManager.getActiveParticipants().isEmpty() && partyManager.getPlayersReady().size() >= partyManager.getActiveParticipants().size()) {
            log("Final undecided player left. Force-completing reward phase.");
            completeRewardPhase();
        }
    }

    public void setPlayerClass(PlayerRef playerRef, String className) {
        rewardManager.setPlayerClass(playerRef, className);
    }

    public boolean canPlayerPickSkill(PlayerRef playerRef) {
        return rewardManager.canPlayerPickSkill(playerRef, globalMaxSkills);
    }

    public Set<PlayerRef> getActiveParticipants() {
        return partyManager.getActiveParticipants();
    }

    public void incrementPlayerSkillCount(PlayerRef playerRef) {
        rewardManager.incrementPlayerSkillCount(playerRef);
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

                roomMobManager.validateRemainingMobs(currentRoomMobs, currentQuest);
                objectiveManager.updateSharedRoomObjective(currentRoomObjectiveId, currentRoom);

                if (!searchEffectApplied && currentQuest.getMobsLeft() <= (currentQuest.getSpawnedMobs() / 3)) {
                    searchEffectApplied = true;
                    broadcastMessage("The remaining monsters have been revealed!");
                    roomMobManager.applyEffectToMobs(currentRoomMobs, MOB_SEARCH_EFFECT);
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
        objectiveManager.completeSharedRoomObjective(currentRoomObjectiveId);
        currentRoomObjectiveId = null;
        log("Triggering EndStage with result: " + result);
        if (result == EndStageResult.SUCCESS) {
            handleStageSuccess();
        } else {
            handleStageFailure();
        }
    }

    private void handleStageSuccess() {
        objectiveManager.completeSharedRoomObjective(currentRoomObjectiveId);
        currentRoomObjectiveId = null;

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

            if (this.globalMaxSkills < 4) {
                this.globalMaxSkills++;
                this.pendingSkillBroadcast = true;
                log("BOSS REACHED. Skill capacity increased to max (4).");
            }
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
        broadcastEventTitle("MISSION COMPLETE !", "You have saved the kweebecs !");
        this.runEnded = true;
        tpParticipantsToPostgame();
    }

    public void grantSkillRewards(PlayerRef playerRef) {
        rewardManager.grantSkillRewards(playerRef, globalMaxSkills);
    }

    public void onPlayerClaimedSkillReward(PlayerRef playerRef, String claimedSkillId) {
        if (!partyManager.isParticipantActive(playerRef)) return;
        rewardManager.onPlayerClaimedSkillReward(playerRef, claimedSkillId, globalMaxSkills, playerModelManager);
    }

    public void onPlayerClaimedArtefactReward(PlayerRef playerRef, String artefactId) {
        if (!partyManager.isParticipantActive(playerRef)) return;
        rewardManager.onPlayerClaimedArtefactReward(playerRef, artefactId);
    }

    public void completeRewardPhase() {
        log("Completing Reward Phase. Path: " + (pendingMilestoneTransition ? "Lobby" : "Next Room"));
        if (pendingMilestoneTransition) {
            transitionToLobby();
        } else {
            advanceToNextRoom();
        }
        partyManager.clearPlayersReady();
    }

    public void startRoomFromLobby() {
        log("Request to start run from Lobby received.");
        if (currentRoom == null) return;

        List<String> unpreparedPlayers = new ArrayList<>();

        for (PlayerRef p : partyManager.getActiveParticipants()) {
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
        weatherManager.setTransitionWeather();

        scheduler.schedule("tp_lobby_" + world.getName(), () -> world.execute(() -> {
            objectiveManager.completeSharedRoomObjective(currentRoomObjectiveId);
            currentRoomObjectiveId = null;
            partyManager.clearParticipantsInRoom();
            roomMobManager.cleanRoom(currentRoomMobs);
            roomMobManager.cleanRoom(futureRoomMobs);

            this.searchEffectApplied = false;
            rewardManager.resetClaimedChests();

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
                            "Skill capacity increased to " + globalMaxSkills
                    );
                    this.pendingSkillBroadcast = false;
                }
            }
        }), TELEPORT_DELAY, TimeUnit.SECONDS);
    }

    private void handleStageFailure() {
        log("CRITICAL: Party Fall. Resetting milestone progress.");
        objectiveManager.completeSharedRoomObjective(currentRoomObjectiveId);
        currentRoomObjectiveId = null;

        broadcastEventTitle("DEFEATED", "The kweebecs still need you... Try again !");

        this.runEnded = true;
        tpParticipantsToPostgame();
    }

    private void broadcastMessage(String text) {
        for (PlayerRef p : partyManager.getActiveParticipants()) {
            p.sendMessage(Message.raw(text));
        }
    }

    private void broadcastEventTitle(String primary, String secondary) {
        Message primaryMsg = Message.raw(primary);
        Message secondaryMsg = Message.raw(secondary);

        for (PlayerRef p : partyManager.getActiveParticipants()) {
            EventTitleUtil.showEventTitleToPlayer(
                    p,
                    primaryMsg,
                    secondaryMsg,
                    true,
                    null,
                    2.0F,
                    0.5F,
                    0.5F
            );
        }
    }

    private void tpParticipantsToLobby() {
        RoomCodec lobby = roomManager.getLobbyRoom();
        if (lobby != null) {
            claimedLobbyHeal.clear();

            world.execute(() -> {
                for (PlayerRef participant : partyManager.getActiveParticipants()) {
                    playerStateManager.healPlayerToFull(participant);
                }
            });

            teleportManager.teleportParticipantsToLobby(partyManager.getActiveParticipants(), lobby);
        }

        weatherManager.setLobbyWeather();
    }

    private void tpParticipantsToRoom() {
        if (currentRoom == null) return;

        if (isBossStage) {
            log(">>> TELEPORTING PARTICIPANTS TO FINAL BOSS ARENA: " + currentRoom.left().worldName + " <<<");
        } else {
            log("Teleporting participants to Room asset: " + currentRoom.left().worldName);
        }

        List<PlayerRef> playersToTeleport = new ArrayList<>();
        for (PlayerRef p : partyManager.getActiveParticipants()) {
            if (!partyManager.isParticipantInRoom(p)) {
                playersToTeleport.add(p);
                partyManager.addParticipantInRoom(p);
            }
        }

        if (!playersToTeleport.isEmpty()) {
            currentRoomObjectiveId = objectiveManager.startSharedRoomObjective(partyManager.getActiveParticipants(), currentRoomObjectiveId);

            if (isBossStage) {
                weatherManager.setBossWeather();
            } else {
                weatherManager.setRandomRoomWeather();
            }

            teleportManager.teleportParticipantsToRoom(playersToTeleport, currentRoom.left());
        }

        scheduler.schedule("late_cleanup_" + world.getName(), roomMobManager::processPendingCleanup, 1, TimeUnit.SECONDS);

        if (isBossStage) {
            scheduler.schedule("boss_hitbox_" + world.getName(), this::makeBossHitboxHard, 1, TimeUnit.SECONDS);
        }
    }

    public void resolvePlayerDeath(PlayerRef playerRef) {
        if (partyManager.isParticipantInRoom(playerRef)) {
            partyManager.addDeadParticipant(playerRef);
            log(String.format("Death tracked: %s. Total Dead: %d/%d",
                    playerRef.getUsername(), partyManager.getDeadParticipants().size(), partyManager.getParticipantsInRoom().size()));

            if (partyManager.getDeadParticipants().size() == partyManager.getParticipantsInRoom().size()) {
                log("Full Wipe detected.");
                endStage(EndStageResult.FAILURE);
            } else {
                teleportToSpectatorPosition(playerRef);
                playerVisibilityManager.hidePlayer(playerRef, partyManager.getActiveParticipants());
            }
        }
    }

    private void reviveDeadPlayers() {
        if (partyManager.getDeadParticipants().isEmpty()) return;
        playerVisibilityManager.showAllDeadPlayers(partyManager.getDeadParticipants(), partyManager.getActiveParticipants());
        log("Reviving " + partyManager.getDeadParticipants().size() + " dead participants.");
        partyManager.clearDeadParticipants();
    }

    private void ejectPlayersFromInstanceAndDestroy() {
        world.execute(() -> {
            for (PlayerRef p : partyManager.getActiveParticipants()) {
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
            partyManager.clearAll();
            GamePlugin.get().destroyGameInstance(this.world);
        });
    }

    public World getWorld() {
        return this.world;
    }

    public void setLobbyWeather() {
        weatherManager.setLobbyWeather();
    }

    public void setPrisonWeather() {
        weatherManager.setPrisonWeather();
    }


    public void playerChestClaim(BlockPosition pos, PlayerRef playerRef) {
        if (!partyManager.isParticipantActive(playerRef)) return;
        rewardManager.playerChestClaim(pos, playerRef);
    }

    public String getParticipantIndices() {
        return partyManager.getParticipantIndices();
    }

    public void breakBlockAt(BlockPosition pos) {
        roomChestsManager.breakBlockAt(pos);
    }

    public void setupRoomChests(RoomCodec room) {
        roomChestsManager.setupRoomChests(room);
    }

    public void randomizeRoomChests(RoomCodec room) {
        roomChestsManager.randomizeRoomChests(room);
    }

    public void restoreRoomChests(RoomCodec room) {
        roomChestsManager.restoreRoomChests(room);
    }

    private void teleportToSpectatorPosition(PlayerRef playerRef) {
        teleportManager.teleportToSpectatorPosition(playerRef, currentRoom != null ? currentRoom.left() : null);
    }


    private void tpParticipantsToPostgame() {
        playerVisibilityManager.showAllDeadPlayers(partyManager.getDeadParticipants(), partyManager.getActiveParticipants());

        world.execute(() -> {
            RoomCodec postgameRoom = roomManager.getPostgameRoom();
            if (postgameRoom != null) {
                log("Teleporting participants to Postgame asset: " + postgameRoom.worldName);
                for (PlayerRef participant : partyManager.getActiveParticipants()) {
                    playerModelManager.resetPlayerModel(participant);
                    playerStateManager.clearInventory(participant);

                    teleportManager.teleportParticipantsToRoom(List.of(participant), postgameRoom);
                }
            } else {
                log("Error: No postgame room defined! Falling back to immediate instance ejection.");
                ejectPlayersFromInstanceAndDestroy();
            }
        });
        GamePlugin.get().destroyGameInstance(this.world);
    }

    public void startingKweebecInteraction(PlayerRef playerRef, CommandBuffer<EntityStore> commandBuffer) {
        if (!partyManager.isParticipantActive(playerRef)) return;

        world.execute(() -> {
            if (rewardManager.hasPlayerClass(playerRef)) {
                log(playerRef.getUsername() + " already has a weapon class.");
                playerRef.sendMessage(Message.raw("You are already geared up! Come find me in the tree!"));
            } else {
                log(playerRef.getUsername() + " has no weapon. Opening Weapon Selection UI.");
                WeaponSelection ui = new WeaponSelection(playerRef, world.getEntityStore().getStore());
                ui.buildPage();
            }
        });
    }

    public void onPlayerSelectedWeapon(PlayerRef playerRef, String weaponClassId) {
        if (!partyManager.isParticipantActive(playerRef)) return;

        setPlayerClass(playerRef, weaponClassId);
        playerRef.sendMessage(Message.raw("Weapon acquired!"));
        log(playerRef.getUsername() + " selected weapon: " + weaponClassId + ".");

        EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                Message.raw("Save the kweebecs !"),
                Message.raw("Good luck, adventurer..."),
                true,
                null,
                2.0F,
                0.5F,
                0.5F
        );
    }

    public void kweebecMerchantInteraction(PlayerRef playerRef) {
        if (!partyManager.isParticipantActive(playerRef)) return;

        UUID playerId = playerRef.getUuid();

        if (claimedLobbyHeal.contains(playerId)) {
            playerRef.sendMessage(Message.raw("My supplies are exhausted! Come back later!"));
            playerStateManager.healPlayerToFull(playerRef);
            return;
        }

        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                Player playerComponent = store.getComponent(ref, Player.getComponentType());

                if (playerComponent != null) {
                    claimedLobbyHeal.add(playerId);

                    playerStateManager.healPlayerToFull(playerRef);

                    com.hypixel.hytale.server.core.inventory.ItemStack stack =
                            new com.hypixel.hytale.server.core.inventory.ItemStack(LOBBY_HEAL_ITEM, 3, null);

                    playerComponent.giveItem(stack, ref, store);


                    playerRef.sendMessage(Message.raw("Here take some of this! On the house."));
                    log(playerRef.getUsername() + " claimed their lobby supplies.");
                }
            }
        });
    }

    public Pair<RoomCodec, Quest> prepareBossRoom(Set<UUID> targetMobTracker) {
        log("Preparing the Final Boss Room...");
        RoomCodec bossRoom = roomManager.findBossRoom();

        if (bossRoom == null) return null;

        List<MobGroupCodec> bossGroups = roomManager.findBossMobGroups();
        Quest quest;

        if (bossGroups != null && !bossGroups.isEmpty()) {
            MobGroupCodec selectedGroup = bossGroups.get(ThreadLocalRandom.current().nextInt(bossGroups.size()));
            quest = new Quest(selectedGroup.getTotalMobCount());

            log(String.format("Boss Room selected: %s. Spawning %d BOSS mobs.", bossRoom.worldName, quest.getSpawnedMobs()));

            world.execute(() -> {
                List<UUID> spawnedMobs = roomMobManager.spawnMobGroup(bossRoom, selectedGroup);
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

    private void makeBossHitboxHard() {
        if (currentRoomMobs.isEmpty()) return;
        UUID bossUuid = currentRoomMobs.iterator().next();

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> bossRef = roomMobManager.getRefByUUID(store, bossUuid);

            if (bossRef != null && bossRef.isValid()) {

                HitboxCollisionConfig hardConfig = HitboxCollisionConfig.getAssetMap().getAsset("HardCollision");
                if (hardConfig != null) {
                    if (!store.getArchetype(bossRef).contains(HitboxCollision.getComponentType())) {
                        store.addComponent(bossRef, HitboxCollision.getComponentType(), new HitboxCollision(hardConfig));
                    } else {
                        store.putComponent(bossRef, HitboxCollision.getComponentType(), new HitboxCollision(hardConfig));
                    }
                    log("Successfully made the Boss HARD. :p");
                } else {
                    log("Warning: Could not find hard collision config in the Asset Store.");
                }
            } else {
                log("Warning: Could not find Boss entity reference to apply hard hitbox.");
            }
        });
    }
}
