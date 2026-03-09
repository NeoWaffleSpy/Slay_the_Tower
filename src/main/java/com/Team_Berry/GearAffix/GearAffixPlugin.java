package com.Team_Berry.GearAffix;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class GearAffixPlugin extends JavaPlugin {
    private static GearAffixPlugin instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static GearAffixPlugin get() {
        return instance;
    }

    public GearAffixPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {

    }
}
