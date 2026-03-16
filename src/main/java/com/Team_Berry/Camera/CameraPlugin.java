package com.Team_Berry.Camera;

import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.Camera.MouseControl.UltMouseControl;
import com.Team_Berry.Camera.Commands.Camera.CameraCommand;
import com.Team_Berry.Camera.Commands.CameraGroup.CameraGroupCommand;
import com.Team_Berry.Camera.Commands.Cinematic.CinematicCommand;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.Team_Berry.Camera.Component.System.PlayerPOVSystem;
import com.Team_Berry.Camera.Interactions.UltInteraction;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
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
        this.getEntityStoreRegistry().registerSystem(new PlayerPOVSystem());
        this.getCommandRegistry().registerCommand(new CameraCommand());
        this.getCommandRegistry().registerCommand(new CameraGroupCommand());
        this.getCommandRegistry().registerCommand(new CinematicCommand());
        this.getCodecRegistry(Interaction.CODEC).register("UltInteraction", UltInteraction.class, UltInteraction.CODEC);


    }

    @Override
    protected void start() {
        CameraInitializer.init();
    }
}
