package com.Team_Berry.Utils;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class UtilsPlugin extends JavaPlugin {
    public static UtilsPlugin instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static UtilsPlugin get() {
        return instance;
    }

    public UtilsPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {

    }
}
