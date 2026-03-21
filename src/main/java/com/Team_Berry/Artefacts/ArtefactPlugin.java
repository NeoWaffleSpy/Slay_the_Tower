package com.Team_Berry.Artefacts;

import com.Team_Berry.Artefacts.Items.TestCodecs;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
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
        //noinspection unchecked
        getAssetRegistry().register(HytaleAssetStore.builder(TestCodecs.class, new DefaultAssetMap<>())
                .setPath("TestCodecs")
                .setCodec(TestCodecs.CODEC)
                .setKeyFunction(TestCodecs::getId)
                .setReplaceOnRemove(TestCodecs::new)
                .build());

        getEventRegistry().register(
                LoadedAssetsEvent.class,
                TestCodecs.class,
                this::onAssetsLoaded);
    }

    private void onAssetsLoaded(IBaseEvent<Class<TestCodecs>> iEvent) {
        LoadedAssetsEvent<String, TestCodecs, DefaultAssetMap<String, TestCodecs>> event = (LoadedAssetsEvent) iEvent;
        for (TestCodecs asset : event.getAssetMap().getAssetMap().values()) {
            LOGGER.atInfo().log("Loaded asset: " + asset.getId());
        }
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down", this.getName());
        commands.forEach(Registration::unregister);
        commands.clear();
        super.shutdown();
    }
}
