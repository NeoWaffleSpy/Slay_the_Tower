package com.Team_Berry.Game;

import com.Team_Berry.Game.Components.QuestNPCComponent;
import com.Team_Berry.Game.Interactions.InitiateStageInteraction;
import com.Team_Berry.Game.Systems.ParticipantPlayerSystem;
import com.Team_Berry.Game.Systems.QuestNPCDeathSystem;
import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GamePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static GamePlugin instance;
    private static ComponentType<EntityStore, QuestNPCComponent> questNPCComponentType;
    private GameManager gameManager;

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

        DefaultAssetMap<String, SkillMilestoneCodec> assetMap = SkillMilestoneCodec.getAssetMap();
        SkillMilestoneCodec milestoneData = assetMap.getAsset("Slay_The_Tower_Milestones");

        this.gameManager = new GameManager(milestoneData);
        getEntityStoreRegistry().registerSystem(new QuestNPCDeathSystem(questNPCComponentType, gameManager));
        getEntityStoreRegistry().registerSystem(new ParticipantPlayerSystem(gameManager));


    }

    @Override
    protected void setup() {
        questNPCComponentType = getEntityStoreRegistry().registerComponent(QuestNPCComponent.class, QuestNPCComponent::new);
        this.getCodecRegistry(Interaction.CODEC).register("InitiateStageInteraction", InitiateStageInteraction.class, InitiateStageInteraction.CODEC);

    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down", this.getName());
        super.shutdown();
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}