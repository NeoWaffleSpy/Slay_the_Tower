package com.Team_Berry.Game.Data;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public class Quest {
    private final int spawnedMobs;
    private List<Ref<EntityStore>> questMobList;
    private int deadMobs = 0;


    public Quest(List<Ref<EntityStore>> questMobList) {
        this.questMobList = questMobList;
        this.spawnedMobs = questMobList.size();
    }

    public Quest(int spawnedMobs) {
        this.spawnedMobs = spawnedMobs;
    }

    public void incrementDeadMobs() {
        this.deadMobs++;
    }

    public boolean isComplete() {
        return deadMobs >= spawnedMobs;
    }
}