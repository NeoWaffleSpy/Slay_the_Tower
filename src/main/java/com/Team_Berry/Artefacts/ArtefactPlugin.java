package com.Team_Berry.Artefacts;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.Camera.CustomCameraSettings;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.event.IBaseEvent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.registry.Registration;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.ArrayList;
import java.util.List;

public class ArtefactPlugin extends JavaPlugin {
    public static ArtefactPlugin instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final List<CommandRegistration> commands = new ArrayList<>();

    public static ArtefactPlugin get() {
        return instance;
    }

    public ArtefactPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        getAssetRegistry().register(HytaleAssetStore.builder(ArtefactCodec.class, new DefaultAssetMap<>())
                .setPath("Artefacts")
                .setCodec(ArtefactCodec.CODEC)
                .setKeyFunction(ArtefactCodec::getId)
                .setReplaceOnRemove(ArtefactCodec::new)
                .build());

        getEventRegistry().register(
                LoadedAssetsEvent.class,
                ArtefactCodec.class,
                this::onArtefactLoaded);
    }

    private void onArtefactLoaded(LoadedAssetsEvent<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> event) {
        event.getLoadedAssets().forEach((name, cam) -> ArtefactCodec.updateCodecSetting(name));
    }

    private void onArtefactRemoved(RemovedAssetsEvent<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> event) {
        event.getRemovedAssets().forEach(ArtefactCodec::remove);
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down", this.getName());
        commands.forEach(Registration::unregister);
        commands.clear();
        super.shutdown();
    }
}
