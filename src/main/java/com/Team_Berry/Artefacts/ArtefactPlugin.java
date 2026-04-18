package com.Team_Berry.Artefacts;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Codecs.StatusEffect.StatusEffectCodec;
import com.Team_Berry.Artefacts.Commandes.ArtefactCommand;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.Artefacts.Components.Systems.StatEffectSystem;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.registry.Registration;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
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

    public static IndexedLookupTableAssetMap<String, EntityStatType> getEntityStatTypeAssetStore() {
        return AssetRegistry.getAssetStore(EntityStatType.class).getAssetMap();
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
        StatusEffectCodec.register();
        StatCodec.register();
        ArtefactCodec.register();
        StatEffectSystem.register();
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, ArtefactPlugin::addComponent);
        this.getCommandRegistry().registerCommand(new ArtefactCommand());
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down", this.getName());
        StatEffectSystem.stop();
        commands.forEach(Registration::unregister);
        commands.clear();
        super.shutdown();
    }

    private static void addComponent(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();
        store.addComponent(ref, StatEffectComponent.getComponentType(), new StatEffectComponent());
    }
}
