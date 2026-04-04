package com.Team_Berry.Rooms;

import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class RoomPlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static RoomPlugin instance;

    public RoomPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static RoomPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        RoomCodec.register();
        LOGGER.atInfo().log("Room System Initialized");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down", this.getName());
        super.shutdown();
    }
}