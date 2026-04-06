package com.Team_Berry.Game;

import com.Team_Berry.Game.Components.QuestNPCComponent;
import com.Team_Berry.Game.Systems.QuestNPCDeathSystem;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GamePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static GamePlugin instance;
    private static ComponentType<EntityStore, QuestNPCComponent> questNPCComponentType;


    public GamePlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static GamePlugin get() {
        return instance;
    }

    public static ComponentType<EntityStore, QuestNPCComponent> getQuestNPCComponentType() {
        return questNPCComponentType;
    }

    @Override
    protected void start() {
        GameManager gameManager = new GameManager();
        getEntityStoreRegistry().registerSystem(new QuestNPCDeathSystem(questNPCComponentType, gameManager));

    }

    @Override
    protected void setup() {
        questNPCComponentType = getEntityStoreRegistry().registerComponent(QuestNPCComponent.class, QuestNPCComponent::new);

    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down", this.getName());
        super.shutdown();
    }
}