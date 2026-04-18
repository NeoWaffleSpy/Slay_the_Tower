package com.Team_Berry.Game;

import com.Team_Berry.Game.Components.QuestNPCComponent;
import com.Team_Berry.Game.Interactions.InstanceGameStartInteraction;
import com.Team_Berry.Game.Interactions.StartRoomFromLobbyInteraction;
import com.Team_Berry.Game.Systems.PlayerDeathSystem;
import com.Team_Berry.Game.Systems.QuestNPCDeathSystem;
import com.Team_Berry.Game.Systems.QuestNPCTaggerSystem;
import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GamePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static GamePlugin instance;
    private static ComponentType<EntityStore, QuestNPCComponent> questNPCComponentType;
    private final Map<World, GameManager> gameManagers = new HashMap<>();
    private GameManager gameManager;
    private SkillMilestoneCodec cachedMilestoneData;

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
        this.cachedMilestoneData = assetMap.getAsset("Slay_The_Tower_Milestones");

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        getEntityStoreRegistry().registerSystem(new QuestNPCDeathSystem());
        getEntityStoreRegistry().registerSystem(new PlayerDeathSystem());
        getEntityStoreRegistry().registerSystem(new QuestNPCTaggerSystem());


    }

    @Override
    protected void setup() {
        questNPCComponentType = getEntityStoreRegistry().registerComponent(QuestNPCComponent.class, QuestNPCComponent::new);
        this.getCodecRegistry(Interaction.CODEC).register("InitiateStageInteraction", InstanceGameStartInteraction.class, InstanceGameStartInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("StartRoomFromLobbyInteraction", StartRoomFromLobbyInteraction.class, StartRoomFromLobbyInteraction.CODEC);

    }

    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down. Cleaning up all active instances...", this.getName());

        Set<World> activeWorlds = new HashSet<>(gameManagers.keySet());
        for (World world : activeWorlds) {
            destroyGameInstance(world);
        }

        super.shutdown();
    }


    private void onPlayerReady(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();
        World currentWorld = store.getExternalData().getWorld();

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        String worldName = currentWorld.getName();

        for (Map.Entry<World, GameManager> entry : gameManagers.entrySet()) {
            if (!entry.getKey().equals(currentWorld)) {
                entry.getValue().removeParticipant(playerRef);
            }
        }

        if (worldName != null && worldName.contains("SlayTheTower")) {
            GameManager manager = gameManagers.computeIfAbsent(currentWorld, w -> {
                GameManager newManager = new GameManager(currentWorld, cachedMilestoneData);
                newManager.initializeMilestone();
                return newManager;
            });

            for (PlayerRef pRef : currentWorld.getPlayerRefs()) {
                manager.addParticipant(pRef);
            }
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();

        if (playerRef != null) {
            World world = Universe.get().getWorld(playerRef.getWorldUuid());

            if (world != null) {
                String worldName = world.getName();

                if (worldName != null && worldName.contains("SlayTheTower")) {
                    GameManager manager = gameManagers.get(world);

                    if (manager != null) {
                        manager.removeParticipant(playerRef);
                    }
                }
            }
        }
    }

    public Map<World, GameManager> getGameManagers() {
        return gameManagers;
    }

    public void destroyGameInstance(World world) {
        if (world == null) return;

        gameManagers.remove(world);
        var config = world.getWorldConfig();
        config.setDeleteOnRemove(true);
        config.setDeleteOnUniverseStart(true);
        config.markChanged();
        InstancesPlugin.safeRemoveInstance(world);
        LOGGER.atInfo().log("Instance and Files marked for deletion: " + world.getName());
    
    }
}