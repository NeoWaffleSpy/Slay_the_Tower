package com.Team_Berry.Camera;

import com.Team_Berry.Camera.Camera.CustomCameraSettings;
import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.Cinematic.CinematicManager;
import com.Team_Berry.Camera.Cinematic.CinematicPlayer;
import com.Team_Berry.Camera.Commands.Camera.CameraCommand;
import com.Team_Berry.Camera.Commands.CameraGroup.CameraGroupCommand;
import com.Team_Berry.Camera.Commands.Cinematic.CinematicCommand;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.Team_Berry.Camera.Component.System.PlayerPOVSystem;
import com.Team_Berry.Camera.Interactions.UltInteraction;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CameraPlugin extends JavaPlugin {
    private static CameraPlugin instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private ComponentType<EntityStore, PlayerPOVComponent> playerPOVComponentType;


    public static CameraPlugin get() {
        return instance;
    }

    public ComponentType<EntityStore, PlayerPOVComponent> getPlayerPOVComponentType() {
        return this.playerPOVComponentType;
    }

    public CameraPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        this.playerPOVComponentType = this.getEntityStoreRegistry().registerComponent(PlayerPOVComponent.class, () -> {
            throw new UnsupportedOperationException("Not implemented!");
        });
        getAssetRegistry().register(HytaleAssetStore.builder(CustomCameraSettings.class, new DefaultAssetMap<>())
                .setPath("CameraSettings")
                .setCodec(CustomCameraSettings.CODEC)
                .setKeyFunction(CustomCameraSettings::getId)
                .setReplaceOnRemove(CustomCameraSettings::new)
                .build());
        getAssetRegistry().register(HytaleAssetStore.builder(CinematicPlayer.class, new DefaultAssetMap<>())
                .setPath("CinemaKeys")
                .setCodec(CinematicPlayer.CODEC)
                .setKeyFunction(CinematicPlayer::getId)
                .setReplaceOnRemove(CinematicPlayer::new)
                .build());
        getEventRegistry().register(LoadedAssetsEvent.class, CustomCameraSettings.class, this::onCameraLoaded);
        getEventRegistry().register(LoadedAssetsEvent.class, CinematicPlayer.class, this::onCinemaLoaded);
        getEventRegistry().register(RemovedAssetsEvent.class, CustomCameraSettings.class, this::onCameraRemoved);
        getEventRegistry().register(RemovedAssetsEvent.class, CinematicPlayer.class, this::onCinemaRemoved);
        this.getEntityStoreRegistry().registerSystem(new PlayerPOVSystem());
        this.getCommandRegistry().registerCommand(new CameraCommand());
        this.getCommandRegistry().registerCommand(new CameraGroupCommand());
        this.getCommandRegistry().registerCommand(new CinematicCommand());
        this.getCodecRegistry(Interaction.CODEC).register("UltInteraction", UltInteraction.class, UltInteraction.CODEC);
    }

    private void onCameraLoaded(LoadedAssetsEvent<String, CustomCameraSettings, DefaultAssetMap<String, CustomCameraSettings>> event) {
        event.getLoadedAssets().forEach((name, cam) -> CameraInitializer.updateCodecSetting(name));
    }

    private void onCameraRemoved(RemovedAssetsEvent<String, CustomCameraSettings, DefaultAssetMap<String, CustomCameraSettings>> event) {
        event.getRemovedAssets().forEach(CameraInitializer::remove);
    }

    private void onCinemaLoaded(LoadedAssetsEvent<String, CinematicPlayer, DefaultAssetMap<String, CinematicPlayer>> event) {
        event.getLoadedAssets().forEach((name, cam) -> CinematicManager.updateCodecSetting(name));
    }

    private void onCinemaRemoved(RemovedAssetsEvent<String, CinematicPlayer, DefaultAssetMap<String, CinematicPlayer>> event) {
        event.getRemovedAssets().forEach(CinematicManager::remove);
    }

    @Override
    protected void start() {
        CameraInitializer.init();
        CinematicManager.init();
    }
}
