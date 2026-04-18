package com.Team_Berry.Game.Data;

public class Quest {
    private final int spawnedMobs;
    private int deadMobs = 0;

    public Quest(int spawnedMobs) {
        this.spawnedMobs = spawnedMobs;
    }

    public void incrementDeadMobs() {
        this.deadMobs++;
    }

    public boolean isComplete() {
        return deadMobs >= spawnedMobs;
    }

    public int getMobsLeft() {
        return spawnedMobs - deadMobs;
    }

    public int getSpawnedMobs() {
        return spawnedMobs;
    }

    public int getDeadMobs() {
        return deadMobs;
    }
}