package com.Team_Berry.Utils;

import com.Team_Berry.Utils.TooltipInjector.TooltipInjector;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.i18n.event.MessagesUpdated;
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
    }

    @Override
    protected void setup() {
        this.getEventRegistry().registerGlobal(MessagesUpdated.class, (event) -> TooltipInjector.injectTooltips());
    }
}
