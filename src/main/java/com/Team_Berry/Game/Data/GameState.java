package com.Team_Berry.Game.Data;

import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;

public class GameState {
    private int clearedStagesNumber = 0;
    private SkillMilestoneCodec skillMilestones;
    private SkillMilestoneCodec.MilestoneEntry currentMilestone;

    // Flag to tell the GameManager when to eject the players
    private boolean isRunComplete = false;

    public void initialize(SkillMilestoneCodec skillMilestones) {
        this.skillMilestones = skillMilestones;
        this.clearedStagesNumber = 0;
        this.isRunComplete = false;
        updateMilestoneStatus();
    }

    public void incrementClearedStages() {
        this.clearedStagesNumber++;
        updateMilestoneStatus();
    }

    private void updateMilestoneStatus() {
        // Failsafe: If no milestones exist, the run is instantly over.
        if (skillMilestones == null || skillMilestones.milestones == null || skillMilestones.milestones.length == 0) {
            this.isRunComplete = true;
            return;
        }

        int cumulativeRoomsRequired = 0;
        boolean foundActiveMilestone = false;

        for (SkillMilestoneCodec.MilestoneEntry entry : skillMilestones.milestones) {
            // Add this milestone's required rooms to the running total.
            // Example: Milestone 1 needs 4. Milestone 2 needs 4 + 3 (7).
            cumulativeRoomsRequired += entry.roomCount;

            // If the total rooms required to pass this milestone is GREATER than our current clears,
            // it means we haven't beaten it yet. We are currently playing inside this milestone!
            if (cumulativeRoomsRequired > this.clearedStagesNumber) {
                this.currentMilestone = entry;
                foundActiveMilestone = true;
                break; // Stop looking, we found where the player is.
            }
        }


        if (!foundActiveMilestone) {
            this.currentMilestone = skillMilestones.milestones[skillMilestones.milestones.length - 1];
            this.isRunComplete = true;
        } else {
            this.isRunComplete = false;
        }
    }

    public void reset() {
        this.clearedStagesNumber = 0;
        this.isRunComplete = false;
        updateMilestoneStatus();
    }

    public SkillMilestoneCodec.MilestoneEntry getCurrentMilestone() {
        return currentMilestone;
    }

    public int getClearedStagesNumber() {
        return clearedStagesNumber;
    }

    public boolean isRunComplete() {
        return isRunComplete;
    }
}