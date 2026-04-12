package com.Team_Berry.Artefacts.UI;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.html.TemplateProcessor;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
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
        List<ImageClass> imgs = new ArrayList<>();
        this.template.setVariable("images", imgs);
        hud = HudBuilder.hudForPlayer(this.playerRef)
                .loadHtml("HUDs/ArtefactPreview.html", this.template)
                .enableRuntimeTemplateUpdates(true)
                .show();
    }

    public void refresh() {
        List<ImageClass> imgs = new ArrayList<>();
        statComp.artefactList.keySet().stream().toList().forEach(artefact -> {
            if (statComp.getAmount(artefact) == 0)
                return;
            imgs.add(new ImageClass(artefact.shortIconPath, statComp.getAmount(artefact)));
        });
        this.template.setVariable("images", imgs);
        hud.refreshOrRerender(true, true);
    }

    private record ImageClass(String image, int count) {}

    private static final String style = """
            <style>
                .artefactColumn {
                    layout-mode: top;
                    anchor-left: 10;
                    anchor-top: 0;
                    anchor-bottom: 0;
                    anchor-width: 64;
                    anchor-height: 504;
                }
            
                .artefactBox {
                    width: 64px;
                    height: 64px;
                    text-align: right;
                    vertical-align: bottom;
                    anchor-top: 10;
                }
            
                .artefactCount {
                    anchor-left: 60;
                    anchor-top: 20;
                    anchor-right: 0;
                    anchor-bottom: 0;
                    color: #ffffff;
                    font-weight: bold;
                    font-size: 25;
                }
                
                .artefactCountShadow {
                    anchor-left: 62;
                    anchor-top: 22;
                    anchor-right: 0;
                    anchor-bottom: 0;
                    color: #000000;
                    font-weight: bold;
                    font-size: 25;
                }
            </style>
            """;

    private static final String artefactPreview = """
            <div class='artefactColumn'>
                {{#each images}}
                {{@artefactContainer:image={{$image}},count={{$count}}}}
                {{/each}}
            </div>
            """;

    private static final String artefactContainer = style + """
            <div class='artefactBox' style='background-image: url({{$image}})'>
                <p class='artefactCountShadow'>{{$count}}</p>
                <p class='artefactCount'>{{$count}}</p>
            </div>
            """;
}
