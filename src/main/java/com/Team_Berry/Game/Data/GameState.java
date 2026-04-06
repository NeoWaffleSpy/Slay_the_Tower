package com.Team_Berry.Game.Data;

import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;

public class GameState {
    private RoomCodec currentRoom;
    private int clearedStagesNumber = 0;
    private SkillMilestoneCodec skillMilestones;
    private SkillMilestoneCodec.MilestoneEntry currentMilestone;

    public void initialize(SkillMilestoneCodec skillMilestones) {
        this.skillMilestones = skillMilestones;
        this.clearedStagesNumber = 0;
        updateMilestoneStatus();
    }

    public void incrementClearedStages() {
        this.clearedStagesNumber++;
        updateMilestoneStatus();
    }

    public void advanceToNextMilestone() {
        updateMilestoneStatus();
    }

    private void updateMilestoneStatus() {
        if (skillMilestones == null || skillMilestones.milestones.isEmpty()) return;

        SkillMilestoneCodec.MilestoneEntry bestMatch = skillMilestones.milestones.get(0);

        for (SkillMilestoneCodec.MilestoneEntry entry : skillMilestones.milestones) {
            if (this.clearedStagesNumber >= entry.roomCount) {
                bestMatch = entry;
            }
        }
        this.currentMilestone = bestMatch;
    }

    public int getRoomsUntilNextMilestone() {
        if (skillMilestones == null) return -1;

        for (SkillMilestoneCodec.MilestoneEntry entry : skillMilestones.milestones) {
            if (entry.roomCount > clearedStagesNumber) {
                return entry.roomCount - clearedStagesNumber;
            }
        }
        return 0; // All milestones completed
    }

    public void reset() {
        this.clearedStagesNumber = 0;
        this.currentRoom = null;
        updateMilestoneStatus();
    }

    public RoomCodec getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(RoomCodec room) {
        this.currentRoom = room;
    }

    public SkillMilestoneCodec.MilestoneEntry getCurrentMilestone() {
        return currentMilestone;
    }

    public int getClearedStagesNumber() {
        return clearedStagesNumber;
    }
}