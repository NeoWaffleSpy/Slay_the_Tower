package com.Team_Berry.Artefacts;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.List;

public class ArtefactHud {
    private static String artefactPreview = """
            <div style='layout-mode: top'>
                <div id='Groupcc1863b0' style='layout-mode: top; anchor-left: 0; anchor-top: 0; anchor-bottom: 0; anchor-width: 64; anchor-height: 504'>
                    {{#each elem}}
                    <img src='test.png' width='64' height='64'>
                    {{/each}}
                </div>
            </div>
            """;

    private static List<String> list = List.of("elem1", "elem2", "elem3", "elem4", "elem5");

    private static TemplateProcessor template = new TemplateProcessor()
            .setVariable("elem", list);

    public static void buildHudPlayer(PlayerRef playerRef) {
        HudBuilder.hudForPlayer(playerRef)
                .fromTemplate(artefactPreview, template)
                .show();
    }
}
