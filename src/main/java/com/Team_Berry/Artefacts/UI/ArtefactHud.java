package com.Team_Berry.Artefacts.UI;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.html.TemplateProcessor;
import com.Team_Berry.Artefacts.Codecs.Enums.RarityEnum;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

public class ArtefactHud {
    private final StatEffectComponent statComp;
    private final PlayerRef playerRef;
    private final TemplateProcessor template = new TemplateProcessor();
    private HyUIHud hud;

    public ArtefactHud(StatEffectComponent statComp, PlayerRef playerRef) {
        this.statComp = statComp;
        this.playerRef = playerRef;
        buildHudPlayer();
    }

    public void buildHudPlayer() {
        List<ArtefactImage> imgs = new ArrayList<>();
        this.template.setVariable("images", imgs);
        hud = HudBuilder.hudForPlayer(this.playerRef)
                .loadHtml("HUDs/ArtefactPreview.html", this.template)
                .enableRuntimeTemplateUpdates(true)
                .show();
    }

    public void refresh() {
        List<ArtefactImage> imgs = new ArrayList<>();
        statComp.artefactList.keySet().forEach(artefact -> {
            if (artefact.rarity == RarityEnum.INVISIBLE || statComp.getAmount(artefact) == 0)
                return;
            imgs.add(new ArtefactImage(artefact.shortIconPath, statComp.getAmount(artefact)));
        });
        this.template.setVariable("images", imgs);


        if (hud != null) {
            hud.removeUnsafe();
        }

        hud = HudBuilder.hudForPlayer(this.playerRef)
                .loadHtml("HUDs/ArtefactPreview.html", this.template)
                .enableRuntimeTemplateUpdates(true)
                .show();
    }

    
    public static class ArtefactImage {
        private final String image;
        private final int count;

        public ArtefactImage(String image, int count) {
            this.image = image;
            this.count = count;
        }

        public String getImage() {
            return image;
        }

        public int getCount() {
            return count;
        }
    }
}