package com.Team_Berry.Artefacts;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Codecs.StatusEffect.StatusEffectCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.Artefacts.Components.Systems.StatEffectSystem;
import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.Camera.CustomCameraSettings;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.event.IBaseEvent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.registry.Registration;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsSystems;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

public class ArtefactPlugin extends JavaPlugin {
    public static ArtefactPlugin instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final List<CommandRegistration> commands = new ArrayList<>();
    private ComponentType<EntityStore, StatEffectComponent> statEffectComponentType;

    public static ArtefactPlugin get() {
        return instance;
    }

    public ComponentType<EntityStore, StatEffectComponent> getStatEffectComponentType() {
        return this.statEffectComponentType;
    }

    public ArtefactPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        this.statEffectComponentType = this.getEntityStoreRegistry().registerComponent(StatEffectComponent.class, () -> {
            throw new UnsupportedOperationException("Not implemented!");
        });
        ArtefactCodec.register();
        StatusEffectCodec.register();
        StatCodec.register();
        //this.entityStatusEffectComponentType = this.getEntityStoreRegistry().registerComponent(EntityStatMap.class, "EntityStats", EntityStatMap.CODEC);
        this.getEntityStoreRegistry().registerSystem(new StatEffectSystem.StatEffectTickingSystem(EntityStatsModule.get().getEntityStatMapComponentType()));
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down", this.getName());
        commands.forEach(Registration::unregister);
        commands.clear();
        super.shutdown();
    }
}
