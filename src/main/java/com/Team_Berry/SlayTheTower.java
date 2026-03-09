package com.Team_Berry;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class SlayTheTower extends JavaPlugin {
    public static SlayTheTower instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
}
