package com.Team_Berry.Artefacts.UI;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.html.TemplateProcessor;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class ShieldHud {
    private final StatEffectComponent statComp;
    private final PlayerRef playerRef;
    private final TemplateProcessor template = new TemplateProcessor();
    private HyUIHud hud;

    public ShieldHud(StatEffectComponent statComp, PlayerRef playerRef) {
        this.statComp = statComp;
        this.playerRef = playerRef;
        buildHudPlayer();
    }

    public void buildHudPlayer() {
        hud = HudBuilder.hudForPlayer(this.playerRef)
                .loadHtml("HUDs/ShieldPreview.html", this.template)
                .enableRuntimeTemplateUpdates(true)
                .show();
        //hud.hide();
    }

    public void displayShield(boolean visible) {
        hud.remove();
        if (visible)
            buildHudPlayer();
    }
}
