package com.Team_Berry.Artefacts;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Codecs.StatusEffect.StatusEffectCodec;
import com.Team_Berry.Artefacts.Commandes.ArtefactCommand;
import com.Team_Berry.Artefacts.Components.HomingMissileComponent;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.Registry.ArtefactLogicRegistry;
import com.Team_Berry.Artefacts.Systems.*;
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
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static ArtefactPlugin instance;
    private static ComponentType<EntityStore, HomingMissileComponent> homingMissileComponentType;
    private final List<CommandRegistration> commands = new ArrayList<>();
    private ComponentType<EntityStore, StatEffectComponent> statEffectComponentType;


    public ArtefactPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static ArtefactPlugin get() {
        return instance;
    }

    public static IndexedLookupTableAssetMap<String, EntityStatType> getEntityStatTypeAssetStore() {
        return AssetRegistry.getAssetStore(EntityStatType.class).getAssetMap();
    }

    private static void addComponent(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();
        StatEffectComponent comp = store.getComponent(ref, StatEffectComponent.getComponentType());
        if (comp != null)
            comp.flush();
        else
            store.addComponent(ref, StatEffectComponent.getComponentType(), new StatEffectComponent());
    }

    public static ComponentType<EntityStore, HomingMissileComponent> getHomingMissileComponentType() {
        return homingMissileComponentType;
    }

    public ComponentType<EntityStore, StatEffectComponent> getStatEffectComponentType() {
        return this.statEffectComponentType;
    }

    @Override
    protected void setup() {
        this.statEffectComponentType = this.getEntityStoreRegistry().registerComponent(StatEffectComponent.class, () -> {
            throw new UnsupportedOperationException("Not implemented!");
        });
        this.homingMissileComponentType = getEntityStoreRegistry().registerComponent(HomingMissileComponent.class, HomingMissileComponent::new);

        ArtefactLogicRegistry.registerAll();
        StatusEffectCodec.register();
        StatCodec.register();
        ArtefactCodec.register();
        StatEffectSystem.register();

        getEntityStoreRegistry().registerSystem(new HomingMissileTickingSystem());
        getEntityStoreRegistry().registerSystem(new ArtefactCooldownSystem());
        getEntityStoreRegistry().registerSystem(new ArtefactLogicPreDamageSystem());
        getEntityStoreRegistry().registerSystem(new ArtefactLogicPostDamageSystem());
        getEntityStoreRegistry().registerSystem(new ArtefactLogicDeathSystem());
        getEntityStoreRegistry().registerSystem(new ArtefactLogicRegenTickingSystem());

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

}