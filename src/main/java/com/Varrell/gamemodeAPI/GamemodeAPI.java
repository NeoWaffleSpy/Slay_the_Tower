package com.Varrell.gamemodeAPI;

import com.Varrell.gamemodeAPI.Camera.CameraInitializer;
import com.Varrell.gamemodeAPI.Commands.Camera.CameraCommand;
import com.Varrell.gamemodeAPI.Commands.CameraGroup.CameraGroupCommand;
import com.Varrell.gamemodeAPI.Component.Data.PlayerPOVComponent;
import com.Varrell.gamemodeAPI.Component.System.PlayerPOVSystem;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GamemodeAPI extends JavaPlugin {
    private static GamemodeAPI instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private ComponentType<EntityStore, PlayerPOVComponent> playerPOVComponentType;

    public static GamemodeAPI get() {
        return instance;
    }

    public ComponentType<EntityStore, PlayerPOVComponent> getPlayerPOVComponentType() {
        return this.playerPOVComponentType;
    }

    public GamemodeAPI(JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        this.playerPOVComponentType = this.getEntityStoreRegistry().registerComponent(PlayerPOVComponent.class, () -> {
            throw new UnsupportedOperationException("Not implemented!");
        });
        CameraInitializer.init();
        this.getEntityStoreRegistry().registerSystem(new PlayerPOVSystem());
        this.getCommandRegistry().registerCommand(new CameraCommand());
        this.getCommandRegistry().registerCommand(new CameraGroupCommand());
    }
}
