package com.Team_Berry;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.registry.Registration;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.ArrayList;
import java.util.List;

public class SlayTheTower extends JavaPlugin {
    public static SlayTheTower instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final List<CommandRegistration> commands = new ArrayList<>();

    public static SlayTheTower get() {
        return instance;
    }

    public SlayTheTower(JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down", this.getName());
        commands.forEach(Registration::unregister);
        commands.clear();
        super.shutdown();
    }
}
