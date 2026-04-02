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
    }

    @Override
    protected void setup() {

    }
}
