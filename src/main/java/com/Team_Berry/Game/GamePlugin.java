package com.Team_Berry.Game;

import com.Team_Berry.Game.Commands.GetParticipantsIndices;
import com.Team_Berry.Game.Components.QuestNPCComponent;
import com.Team_Berry.Game.Interactions.*;
import com.Team_Berry.Game.Objectives.CustomRoomTaskAsset;
import com.Team_Berry.Game.Systems.*;
import com.Team_Berry.Game.Utils.PlayerInventory;
import com.Team_Berry.Rooms.Codecs.SkillMilestoneCodec;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GamePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final KeyedScheduler scheduler = new KeyedScheduler();
    public static GamePlugin instance;
    private static ComponentType<EntityStore, QuestNPCComponent> questNPCComponentType;
    private final Map<World, GameManager> gameManagers = new HashMap<>();
    private final Map<java.util.UUID, PlayerInventory> savedInventories = new java.util.concurrent.ConcurrentHashMap<>();
    private SkillMilestoneCodec cachedMilestoneData;
    private PacketFilter inboundDropFilter;

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

        this.getEventRegistry().registerGlobal(StartWorldEvent.class, this::onStartWorld);

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        getEntityStoreRegistry().registerSystem(new QuestNPCDeathSystem());
        getEntityStoreRegistry().registerSystem(new PlayerDeathSystem());
        getEntityStoreRegistry().registerSystem(new QuestNPCTaggerSystem(questNPCComponentType));
        getEntityStoreRegistry().registerSystem(new BreakBlockProtectionSystem(BreakBlockEvent.class));
        getEntityStoreRegistry().registerSystem(new PlaceBlockProtectionSystem(PlaceBlockEvent.class));


        CustomRoomTaskAsset.registerLogic();
    }

    @Override
    protected void setup() {
        questNPCComponentType = getEntityStoreRegistry().registerComponent(QuestNPCComponent.class, QuestNPCComponent::new);
        this.getCodecRegistry(Interaction.CODEC).register("InitiateStageInteraction", InstanceGameStartInteraction.class, InstanceGameStartInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("StartRoomFromLobbyInteraction", StartRoomFromLobbyInteraction.class, StartRoomFromLobbyInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("StatueClaimSkillInteraction", StatueClaimSkillInteraction.class, StatueClaimSkillInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ChestClaimInteraction", ChestClaimInteraction.class, ChestClaimInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ClaimStartWeaponInteraction", ClaimStartWeaponInteraction.class, ClaimStartWeaponInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("KweebecMerchantInteraction", KweebecMerchantInteraction.class, KweebecMerchantInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("SpawnAOEInteraction", SpawnAOEInteraction.class, SpawnAOEInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ProjectileRainInteraction", ProjectileRainInteraction.class, ProjectileRainInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("SetLobbyWeatherInteraction", SetLobbyWeatherInteraction.class, SetLobbyWeatherInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("SetPrisonWeatherInteraction", SetPrisonWeatherInteraction.class, SetPrisonWeatherInteraction.CODEC);

        this.getCommandRegistry().registerCommand(new GetParticipantsIndices("getparticipantsindices", "get all indexes of all active participants"));

        CustomRoomTaskAsset.registerCodecs();

        DropItemPacketHandler dropHandler = new DropItemPacketHandler();
        inboundDropFilter = PacketAdapters.registerInbound(dropHandler);
    }

    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down. Cleaning up all active instances...", this.getName());

        Set<World> activeWorlds = new HashSet<>(gameManagers.keySet());
        for (World world : activeWorlds) {
            destroyGameInstance(world);
        }
        if (inboundDropFilter != null) {
            PacketAdapters.deregisterInbound(inboundDropFilter);
        }
        super.shutdown();
    }

    private void onStartWorld(StartWorldEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();

        if (worldName.contains("SlayTheTower")) {
            WorldConfig config = world.getWorldConfig();
            config.setDeleteOnUniverseStart(true);
            config.markChanged();

            scheduler.schedule("init_gm_" + worldName, () -> {
                world.execute(() -> {
                    if (!world.isStarted()) return;

                    GameManager manager = new GameManager(world, cachedMilestoneData);
                    manager.initializeMilestone();
                    gameManagers.put(world, manager);

                    LOGGER.atInfo().log("Initialized GameManager and set auto-cleanup flags for instance: %s", worldName);

                    manager.addAllPresentPlayers();
                });
            }, 1, java.util.concurrent.TimeUnit.SECONDS);
        }
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

        if (worldName.contains("SlayTheTower")) {

            if (!savedInventories.containsKey(playerRef.getUuid())) {
                savedInventories.put(playerRef.getUuid(), PlayerInventory.fromPlayer(ref, store));
                PlayerInventory.clearPlayerInventory(ref, store);
                LOGGER.atInfo().log("Saved inventory to bank and cleared for new run: " + playerRef.getUsername());
            } else {
                LOGGER.atInfo().log("Player reconnected mid-run, keeping current SlayTheTower inventory & model: " + playerRef.getUsername());
            }

            GameManager manager = gameManagers.get(currentWorld);
            if (manager != null) {
                manager.addParticipant(playerRef);
            }

        } else {
            resetPlayerModel(ref, store);

            PlayerInventory inv = savedInventories.remove(playerRef.getUuid());
            if (inv != null) {
                inv.applyToPlayer(ref, store);
                LOGGER.atInfo().log("Restored inventory from bank for " + playerRef.getUsername());
            }
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        for (GameManager manager : gameManagers.values()) {
            if (manager.getActiveParticipants().contains(playerRef)) {
                manager.removeParticipant(playerRef);
            }
        }
    }

    public Map<World, GameManager> getGameManagers() {
        return gameManagers;
    }

    public void destroyGameInstance(World world) {
        if (world == null) return;

        gameManagers.remove(world);
        WorldConfig config = world.getWorldConfig();
        config.setDeleteOnRemove(true);
        config.markChanged();
        InstancesPlugin.safeRemoveInstance(world);
        LOGGER.atInfo().log("Instance marked for deletion: " + world.getName());
    }

    public GameManager getGameManager(World world) {
        return gameManagers.get(world);
    }

    public void saveAndClearPlayerInventory(PlayerRef playerRef) {
        playerRef.getReference().getStore().getExternalData().getWorld().execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                savedInventories.put(playerRef.getUuid(), PlayerInventory.fromPlayer(ref, ref.getStore()));
                PlayerInventory.clearPlayerInventory(ref, ref.getStore());
                LOGGER.atInfo().log("Saved and cleared inventory for " + playerRef.getUsername());
            }
        });
    }

    public void restorePlayerInventory(PlayerRef playerRef) {
        playerRef.getReference().getStore().getExternalData().getWorld().execute(() -> {
            PlayerInventory inv = savedInventories.remove(playerRef.getUuid());
            if (inv != null) {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref != null && ref.isValid()) {
                    inv.applyToPlayer(ref, ref.getStore());
                    LOGGER.atInfo().log("Restored inventory for " + playerRef.getUsername());
                }
            }
        });
    }

    public void resetPlayerModel(Ref<EntityStore> ref, Store<EntityStore> store) {
        store.getExternalData().getWorld().execute(() -> {
            PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());

            if (skinComponent != null) {
                Model newModel =
                        CosmeticsModule.get().createModel(skinComponent.getPlayerSkin());

                store.putComponent(ref, ModelComponent.getComponentType(),
                        new ModelComponent(newModel));

                skinComponent.setNetworkOutdated();

                LOGGER.atInfo().log("Successfully reset model for returning player.");
            }

        });
    }
}