package com.Team_Berry.Game.Data;

import com.Team_Berry.Rooms.Codecs.RoomCodec;

public class GameState {
    private RoomCodec currentRoomId; // Reference to the Room entity or ID
    private int clearedStagesNumber = 0;
    private int currentMilestoneMax = 5; // Example: Portal at 5 stages

    public int getClearedStagesNumber() {
        return clearedStagesNumber;
    }

    public void incrementClearedStages() {
        this.clearedStagesNumber++;
    }

    public boolean isMilestoneReached() {
        return clearedStagesNumber >= currentMilestoneMax;
    }

    public void reset() {
        this.clearedStagesNumber = 0;
        this.currentRoomId = null;
    }
}