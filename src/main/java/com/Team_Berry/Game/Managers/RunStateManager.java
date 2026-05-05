package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.Data.GameState;
import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;
import com.hypixel.hytale.server.core.universe.world.World;

public class RunStateManager {
    private final World world;
    private final GameState gameState;

    private boolean isBossStage = false;
    private int globalMaxSkills = 1;
    private boolean statueSpawn = false;
    private boolean runEnded = false;
    private boolean pendingMilestoneTransition = false;
    private boolean pendingSkillBroadcast = false;

    public RunStateManager(World world, SkillMilestoneCodec milestoneData) {
        this.world = world;
        this.gameState = new GameState();
        this.gameState.initialize(milestoneData);
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public boolean isStatueSpawnSet() {
        return statueSpawn;
    }

    public void setStatueSpawn(boolean statueSpawn) {
        this.statueSpawn = statueSpawn;
    }

    public boolean isRunEnded() {
        return runEnded;
    }

    public void setRunEnded(boolean runEnded) {
        this.runEnded = runEnded;
    }

    public int getGlobalMaxSkills() {
        return globalMaxSkills;
    }

    public boolean hasPendingMilestoneTransition() {
        return pendingMilestoneTransition;
    }

    public void setPendingMilestoneTransition(boolean pending) {
        this.pendingMilestoneTransition = pending;
    }

    public boolean hasPendingSkillBroadcast() {
        return pendingSkillBroadcast;
    }

    public void clearPendingSkillBroadcast() {
        this.pendingSkillBroadcast = false;
    }

    public boolean isRunComplete() {
        return gameState.isRunComplete();
    }

    public int getCurrentDifficulty() {
        return gameState.getCurrentMilestone().difficulty;
    }

    public void incrementClearedStages() {
        gameState.incrementClearedStages();
    }

    public boolean isBossStage() {
        return isBossStage;
    }

    public void setBossStage(boolean isBossStage) {
        this.isBossStage = isBossStage;
    }

    public SkillMilestoneCodec.MilestoneEntry getCurrentMilestone() {
        return gameState.getCurrentMilestone();
    }

    public void handleMilestoneProgression(SkillMilestoneCodec.MilestoneEntry oldMilestone) {
        SkillMilestoneCodec.MilestoneEntry newMilestone = gameState.getCurrentMilestone();

        if (gameState.isRunComplete()) {
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
    }
}