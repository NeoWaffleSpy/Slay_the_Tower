package com.Team_Berry.Slay;

import com.Team_Berry.Slay.Camera.CameraInitializer;
import com.Team_Berry.Slay.Commands.Camera.CameraCommand;
import com.Team_Berry.Slay.Commands.CameraGroup.CameraGroupCommand;
import com.Team_Berry.Slay.Component.Data.PlayerPOVComponent;
import com.Team_Berry.Slay.Component.System.PlayerPOVSystem;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SlayTheTower extends JavaPlugin {
    private static SlayTheTower instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private ComponentType<EntityStore, PlayerPOVComponent> playerPOVComponentType;

    public static SlayTheTower get() {
        return instance;
    }

    public ComponentType<EntityStore, PlayerPOVComponent> getPlayerPOVComponentType() {
        return this.playerPOVComponentType;
    }

    public SlayTheTower(JavaPluginInit init) {
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
