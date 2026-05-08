//TODO : I plan on coming back here in the very near future to rework all of this. I'm not going to lie this is my first time doing this kind of thing and I did not know how far a game manager could go. I need to go much more horizontally to make this monster more maintanable.

package com.Team_Berry.Game;

import com.Team_Berry.Artefacts.UI.WeaponSelection;
import com.Team_Berry.Game.Data.Quest;
import com.Team_Berry.Game.Enums.EndStageResult;
import com.Team_Berry.Game.Enums.QuestUpdate;
import com.Team_Berry.Game.Managers.*;
import com.Team_Berry.Rooms.Codecs.MobGroupCodec;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import it.unimi.dsi.fastutil.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class GameManager {
    private static final KeyedScheduler scheduler = new KeyedScheduler();
    private static final int TELEPORT_DELAY = 7;

    private final RunStateManager runStateManager;
    private final PlayerModelManager playerModelManager;
    private final WeatherManager weatherManager;
    private final RoomManager roomManager;
    private final RoomChestsManager roomChestsManager;
    private final TeleportManager teleportManager;
    private final RewardManager rewardManager;
    private final RoomMobManager roomMobManager;
    private final PlayerStateManager playerStateManager;
    private final ObjectiveManager objectiveManager;
    private final PartyManager partyManager;

    private final World world;

    public GameManager(World world, SkillMilestoneCodec milestoneData) {
        this.world = world;
        this.runStateManager = new RunStateManager(world, milestoneData);
        this.playerModelManager = new PlayerModelManager(scheduler);
        this.weatherManager = new WeatherManager(world);
        this.roomManager = new RoomManager(world);
        this.roomChestsManager = new RoomChestsManager(world);
        this.teleportManager = new TeleportManager(world);
        this.rewardManager = new RewardManager(world);
        this.roomMobManager = new RoomMobManager(world);
        this.playerStateManager = new PlayerStateManager(world);
        this.objectiveManager = new ObjectiveManager(world);
        this.partyManager = new PartyManager(world);
        log("Manager initialized for world.");
    }

    public void setStatueSpawn() {
        if (!runStateManager.isStatueSpawnSet()) {
            WorldConfig config = world.getWorldConfig();
            RoomCodec lobby = roomManager.getLobbyRoom();
            if (lobby != null && lobby.getFirstSpawnPosition() != null) {
                Transform transform = new Transform(lobby.getFirstSpawnPosition());
                config.setSpawnProvider(new GlobalSpawnProvider(transform));
                config.markChanged();
                runStateManager.setStatueSpawn(true);
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

        roomMobManager.resetSearchEffect();

        if (roomMobManager.hasCurrentMobs()) {
            log("Cleaning up leftover current room mobs...");
            roomMobManager.cleanCurrentRoom();
        }

        if (roomMobManager.hasFutureMobs()) {
            log("Cleaning up old future room mobs before milestone initialization...");
            roomMobManager.cleanFutureRoom();
        }

        if (runStateManager.isRunComplete()) {
            runStateManager.setBossStage(true);
        }

        roomManager.setCurrentRoom(prepareRoom(null, true));

        if (runStateManager.isBossStage()) {
            roomManager.setFutureRoom(null);
            log("Boss Buffer Ready. Current: " + (roomManager.getCurrentRoom() != null ? roomManager.getCurrentRoom().left().worldName : "ERROR"));
        } else {
            roomManager.setFutureRoom(prepareRoom(roomManager.getCurrentRoom().left(), false));
            log(String.format("Buffer Ready. Current: %s | Future: %s",
                    roomManager.getCurrentRoom().left().worldName, roomManager.getFutureRoom().left().worldName));
        }
    }

    public Pair<RoomCodec, Quest> prepareRoom(@Nullable RoomCodec exclude, boolean isCurrentRoom) {
        return generateRoomLogic(runStateManager.isBossStage(), isCurrentRoom, exclude);
    }

    private Pair<RoomCodec, Quest> generateRoomLogic(boolean isBoss, boolean isCurrentRoom, @Nullable RoomCodec exclude) {
        log(isBoss ? "Preparing the Final Boss Room..." : "Preparing a new regular room...");

        RoomCodec selectedRoom = roomManager.findRoomByCategory(isBoss ? "Boss" : "Room", exclude);
        if (selectedRoom == null) return null;

        int difficulty = runStateManager.getCurrentDifficulty();
        List<MobGroupCodec> mobGroups = roomMobManager.findMobGroups(difficulty, isBoss);
        Quest quest;

        if (mobGroups != null && !mobGroups.isEmpty()) {
            MobGroupCodec selectedGroup = mobGroups.get(ThreadLocalRandom.current().nextInt(mobGroups.size()));
            quest = new Quest(selectedGroup.getTotalMobCount());

            log(String.format("Room selected: %s. Spawning %d mobs (Boss: %b)",
                    selectedRoom.worldName, quest.getSpawnedMobs(), isBoss));

            world.execute(() -> {
                List<UUID> spawnedMobs = roomMobManager.spawnMobGroup(selectedRoom, selectedGroup);
                if (isCurrentRoom) {
                    roomMobManager.registerCurrentMobs(spawnedMobs);
                } else {
                    roomMobManager.registerFutureMobs(spawnedMobs);
                }
                log("Successfully registered " + spawnedMobs.size() + " UUIDs.");
            });

            if (!isBoss) {
                roomChestsManager.setupRoomChests(selectedRoom);
            }

        } else {
            quest = new Quest(0);
            log("Room Error: " + selectedRoom.worldName + " (No mob groups found).");
        }

        return Pair.of(selectedRoom, quest);
    }

    public Pair<RoomCodec, Quest> getCurrentRoom() {
        return roomManager.getCurrentRoom();
    }

    public void advanceToNextRoom() {
        log("Queueing tower advance in " + TELEPORT_DELAY + " seconds...");
        weatherManager.setTransitionWeather();

        scheduler.schedule("advance_room_" + world.getName(), () -> world.execute(() -> {
            log("Advancing tower. Shifting buffer...");
            partyManager.clearParticipantsInRoom();
            roomMobManager.cleanCurrentRoom();

            roomMobManager.resetSearchEffect();

            roomManager.shiftRooms();

            roomMobManager.shiftMobsToCurrent();

            rewardManager.resetClaimedChests();

            tpParticipantsToRoom();

            roomManager.setFutureRoom(prepareRoom(roomManager.getCurrentRoom().left(), false));
            log("Advance complete. Current Room is now: " + roomManager.getCurrentRoom().left().worldName);
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

        rewardManager.grantStartingArtefact(playerRef);
        rewardManager.restorePlayerArtefacts(playerRef);


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
        objectiveManager.detachPlayerFromObjective(playerRef);
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
        return rewardManager.canPlayerPickSkill(playerRef, runStateManager.getGlobalMaxSkills());
    }

    public Set<PlayerRef> getActiveParticipants() {
        return partyManager.getActiveParticipants();
    }

    public void incrementPlayerSkillCount(PlayerRef playerRef) {
        rewardManager.incrementPlayerSkillCount(playerRef);
    }

    public void updateQuest(QuestUpdate questUpdate, @Nullable PlayerRef playerRef, @Nullable UUID deadMobId) {
        if (questUpdate == QuestUpdate.PLAYER_DEATH && playerRef != null) {
            log("Player Death event: " + playerRef.getUsername());
            resolvePlayerDeath(playerRef);
            return;
        }

        if (questUpdate == QuestUpdate.MOB_DEATH && deadMobId != null) {
            Quest currentQuest = roomManager.getCurrentRoom() != null ? roomManager.getCurrentRoom().right() : null;
            Quest futureQuest = roomManager.getFutureRoom() != null ? roomManager.getFutureRoom().right() : null;

            if (roomMobManager.handleMobDeath(deadMobId, currentQuest, futureQuest)) {
                objectiveManager.updateSharedRoomObjective(roomManager.getCurrentRoom());
                roomMobManager.checkAndApplySearchEffect(currentQuest, partyManager.getActiveParticipants());

                if (currentQuest != null && currentQuest.isComplete()) {
                    log("Room Quest successfully completed.");
                    endStage(EndStageResult.SUCCESS);
                }
            }
        }
    }

    public void endStage(EndStageResult result) {
        objectiveManager.completeSharedRoomObjective();
        log("Triggering EndStage with result: " + result);
        if (result == EndStageResult.SUCCESS) {
            handleStageSuccess();
        } else {
            handleStageFailure();
        }
    }

    private void handleStageSuccess() {
        objectiveManager.completeSharedRoomObjective();

        if (runStateManager.isBossStage()) {
            log("VICTORY! The Boss has been defeated.");
            handleRunVictory();
            return;
        }

        SkillMilestoneCodec.MilestoneEntry oldMilestone = runStateManager.getCurrentMilestone();
        runStateManager.incrementClearedStages();

        runStateManager.handleMilestoneProgression(oldMilestone);
        reviveDeadPlayers();
        completeRewardPhase();
    }

    private void handleRunVictory() {
        log("Ending successful run and ejecting players.");
        broadcastEventTitle("MISSION COMPLETE !", "You have saved the kweebecs !");
        runStateManager.setRunEnded(true);
        tpParticipantsToPostgame();
    }

    public void grantSkillRewards(PlayerRef playerRef) {
        rewardManager.grantSkillRewards(playerRef, runStateManager.getGlobalMaxSkills());
    }

    public void onPlayerClaimedSkillReward(PlayerRef playerRef, String claimedSkillId) {
        if (!partyManager.isParticipantActive(playerRef)) return;
        rewardManager.onPlayerClaimedSkillReward(playerRef, claimedSkillId, runStateManager.getGlobalMaxSkills(), playerModelManager);
    }

    public void onPlayerClaimedArtefactReward(PlayerRef playerRef, String artefactId) {
        if (!partyManager.isParticipantActive(playerRef)) return;
        rewardManager.onPlayerClaimedArtefactReward(playerRef, artefactId);
    }

    public void completeRewardPhase() {
        log("Completing Reward Phase. Path: " + (runStateManager.hasPendingMilestoneTransition() ? "Lobby" : "Next Room"));
        if (runStateManager.hasPendingMilestoneTransition()) {
            transitionToLobby();
        } else {
            advanceToNextRoom();
        }
        partyManager.clearPlayersReady();
    }

    public void startRoomFromLobby() {
        log("Request to start run from Lobby received.");
        if (roomManager.getCurrentRoom() == null) return;

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
            objectiveManager.completeSharedRoomObjective();
            partyManager.clearParticipantsInRoom();
            roomMobManager.cleanCurrentRoom();
            roomMobManager.cleanFutureRoom();

            roomMobManager.resetSearchEffect();
            rewardManager.resetClaimedChests();

            if (runStateManager.isRunEnded()) {
                log("Run has ended. Moving party to Postgame Room and queueing world destruction.");
                tpParticipantsToPostgame();

            } else {
                log("Moving party to Lobby.");
                tpParticipantsToLobby();
                initializeMilestone();
                runStateManager.setPendingMilestoneTransition(false);

                if (runStateManager.hasPendingSkillBroadcast()) {
                    broadcastEventTitle(
                            "MILESTONE REACHED",
                            "Skill capacity increased to " + runStateManager.getGlobalMaxSkills()
                    );
                    runStateManager.clearPendingSkillBroadcast();
                }
            }
        }), TELEPORT_DELAY, TimeUnit.SECONDS);
    }

    private void handleStageFailure() {
        log("CRITICAL: Party Fall. Resetting milestone progress.");
        objectiveManager.completeSharedRoomObjective();

        scheduler.cancel("advance_room_" + world.getName());
        scheduler.cancel("tp_lobby_" + world.getName());
        weatherManager.setPrisonWeather();
        broadcastEventTitle("DEFEATED", "The kweebecs still need you... Try again !");

        runStateManager.setRunEnded(true);
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
            playerStateManager.clearLobbyHeals();

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
        if (roomManager.getCurrentRoom() == null) return;

        if (runStateManager.isBossStage()) {
            log(">>> TELEPORTING PARTICIPANTS TO FINAL BOSS ARENA: " + roomManager.getCurrentRoom().left().worldName + " <<<");
        } else {
            log("Teleporting participants to Room asset: " + roomManager.getCurrentRoom().left().worldName);
        }

        List<PlayerRef> playersToTeleport = new ArrayList<>();
        for (PlayerRef p : partyManager.getActiveParticipants()) {
            if (!partyManager.isParticipantInRoom(p)) {
                playersToTeleport.add(p);
                partyManager.addParticipantInRoom(p);
            }
        }

        if (!playersToTeleport.isEmpty()) {
            objectiveManager.startSharedRoomObjective(partyManager.getActiveParticipants(), roomManager.getCurrentRoom());

            if (runStateManager.isBossStage()) {
                weatherManager.setBossWeather();
            } else {
                weatherManager.setRandomRoomWeather();
            }

            teleportManager.teleportParticipantsToRoom(playersToTeleport, roomManager.getCurrentRoom().left());
        }

        scheduler.schedule("late_cleanup_" + world.getName(), roomMobManager::processPendingCleanup, 1, TimeUnit.SECONDS);

        if (runStateManager.isBossStage()) {
            scheduler.schedule("boss_hitbox_" + world.getName(), roomMobManager::makeBossHitboxHard, 1, TimeUnit.SECONDS);
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
                // partyManager.hidePlayer(playerRef);
            }
        }
    }

    private void reviveDeadPlayers() {
        if (partyManager.getDeadParticipants().isEmpty()) return;
        // partyManager.showAllDeadPlayers();
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
        teleportManager.teleportToSpectatorPosition(playerRef, roomManager.getCurrentRoom() != null ? roomManager.getCurrentRoom().left() : null);
    }


    private void tpParticipantsToPostgame() {
        //  partyManager.showAllDeadPlayers();

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

        rewardManager.setPlayerClass(playerRef, weaponClassId);
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
        playerStateManager.kweebecMerchantInteraction(playerRef);
    }

}