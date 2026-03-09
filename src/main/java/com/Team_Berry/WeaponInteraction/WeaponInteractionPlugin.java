package com.Team_Berry.WeaponInteraction;

import com.Team_Berry.Camera.Camera.MouseControl.UltMouseControl;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.Team_Berry.WeaponInteraction.Component.Ult.UltExplosionComponent;
import com.Team_Berry.WeaponInteraction.System.Ult.UltExplosionTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class WeaponInteractionPlugin extends JavaPlugin {
    public static WeaponInteractionPlugin instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ComponentType<EntityStore, UltExplosionComponent> ultExplosionComponentComponentType;

    public static WeaponInteractionPlugin get() {
        return instance;
    }

    public WeaponInteractionPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        ultExplosionComponentComponentType = getEntityStoreRegistry().registerComponent(UltExplosionComponent.class, UltExplosionComponent::new);
        UltMouseControl.ULT_EXPLOSION_COMPONENT_TYPE = ultExplosionComponentComponentType;
        getEntityStoreRegistry().registerSystem(new UltExplosionTickingSystem(ultExplosionComponentComponentType));
    }
}
