package com.Team_Berry.WeaponInteraction;

import com.Team_Berry.Camera.Camera.MouseControl.UltMouseControl;
import com.Team_Berry.WeaponInteraction.Component.BleedComponent;
import com.Team_Berry.WeaponInteraction.Component.PiercedCooldownComponent;
import com.Team_Berry.WeaponInteraction.Component.UltExplosionComponent;
import com.Team_Berry.WeaponInteraction.Interactions.BleedInteraction;
import com.Team_Berry.WeaponInteraction.Interactions.ResetCameraInteraction;
import com.Team_Berry.WeaponInteraction.Interactions.SetCameraInteraction;
import com.Team_Berry.WeaponInteraction.Systems.*;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class WeaponInteractionPlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static WeaponInteractionPlugin instance;
    private static ComponentType<EntityStore, UltExplosionComponent> ultExplosionComponentComponentType;
    private static ComponentType<EntityStore, BleedComponent> bleedComponentType;
    private static ComponentType<EntityStore, PiercedCooldownComponent> piercedCooldownComponentType;

    public WeaponInteractionPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static WeaponInteractionPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        ultExplosionComponentComponentType = getEntityStoreRegistry().registerComponent(UltExplosionComponent.class, UltExplosionComponent::new);
        bleedComponentType = getEntityStoreRegistry().registerComponent(BleedComponent.class, BleedComponent::new);
        piercedCooldownComponentType = getEntityStoreRegistry().registerComponent(PiercedCooldownComponent.class, PiercedCooldownComponent::new);

        UltMouseControl.ULT_EXPLOSION_COMPONENT_TYPE = ultExplosionComponentComponentType;

        this.getCodecRegistry(Interaction.CODEC).register("SetCameraInteraction", SetCameraInteraction.class, SetCameraInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ResetCameraInteraction", ResetCameraInteraction.class, ResetCameraInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("BleedInteraction", BleedInteraction.class, BleedInteraction.CODEC);
        BleedInteraction.BLEED_COMPONENT_TYPE = bleedComponentType;


    }

    @Override
    protected void start() {
        getEntityStoreRegistry().registerSystem(new UltExplosionTickingSystem(ultExplosionComponentComponentType));
        getEntityStoreRegistry().registerSystem(new BleedTickingSystem(bleedComponentType));
        getEntityStoreRegistry().registerSystem(new SlowBombDamageSystem());
        getEntityStoreRegistry().registerSystem(new PierceProjectileDamageSystem(piercedCooldownComponentType));
        getEntityStoreRegistry().registerSystem(new HotbarManagerTickingSystem());
    }
}
