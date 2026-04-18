package com.Team_Berry.Artefacts.UI;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Enums.TargetType;
import com.Team_Berry.Artefacts.Codecs.Enums.TriggerType;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ArtefactSelection {
    private final PlayerRef playerRef;
    private final Store<EntityStore> store;
    private final TemplateProcessor template = new TemplateProcessor();
    private final List<ArtefactInfos> artefacts = new ArrayList<>();
    private HyUIPage page;

    public ArtefactSelection(PlayerRef playerRef, Store<EntityStore> store) {
        this.playerRef = playerRef;
        this.store = store;
    }

    public static List<ArtefactCodec> weightedDraw(List<ArtefactCodec> input, int count) {
        List<ArtefactCodec> pool = new ArrayList<>(input);
        List<ArtefactCodec> result = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            float totalWeight = 0;
            for (ArtefactCodec obj : pool) {
                if (obj.rarity.weight > 0) {
                    totalWeight += obj.rarity.weight;
                }
            }
            if (totalWeight <= 0) break;
            float r = random.nextFloat(totalWeight);
            float cumulative = 0;
            ArtefactCodec selected = null;
            for (ArtefactCodec obj : pool) {
                float w = obj.rarity.weight;
                if (w <= 0) continue;
                cumulative += w;
                if (r < cumulative) {
                    selected = obj;
                    break;
                }
            }

            if (selected == null) break;
            result.add(selected);
            pool.remove(selected);
        }
        return result;
    }

    public void buildPage() {
        List<ArtefactCodec> artefactCodecs = weightedDraw(ArtefactCodec.getAssetMap().getAssetMap().values().stream().filter(a -> a.rarity.weight > 0).toList(), 3);
        if (artefactCodecs.size() < 3)
            return;
        artefacts.add(parseInfo(artefactCodecs.get(0), 0));
        artefacts.add(parseInfo(artefactCodecs.get(1), 1));
        artefacts.add(parseInfo(artefactCodecs.get(2), 2));
        this.template.setVariable("artefacts", artefacts);
        page = PageBuilder.pageForPlayer(this.playerRef)
                .loadHtml("Pages/ArtefactSelection.html", this.template)
                .addEventListener("my-button-0", CustomUIEventBindingType.Activating, (_, _) -> this.buttonEvent(0))
                .addEventListener("my-button-1", CustomUIEventBindingType.Activating, (_, _) -> this.buttonEvent(1))
                .addEventListener("my-button-2", CustomUIEventBindingType.Activating, (_, _) -> this.buttonEvent(2))
                .open(store);
    }

    private ArtefactInfos parseInfo(ArtefactCodec artefact, int index) {
        if (artefact == null)
            return null;
        return new ArtefactInfos(
                I18nModule.get().getMessage("en-US", artefact.translationProperties.getName()),
                artefact.shortIconPath,
                I18nModule.get().getMessage("en-US", artefact.translationProperties.getDescription()),
                null,
                parseStats(artefact.getStatArray()),
                artefact,
                artefact.rarity.index,
                index);
    }

    private ArrayList<String> parseStats(ArrayList<StatCodec> stats) {
        ArrayList<String> list = new ArrayList<>();
        stats.forEach((stat) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(stat.type);
            sb.append(": ");
            sb.append(stat.value < 0 ? "" : "+");
            sb.append(stat.calc == StaticModifier.CalculationType.MULTIPLICATIVE ? (stat.value * 100 + "%") : stat.value);
            sb.append(stat.target == TargetType.ENEMY ? " on enemy" : "");
            sb.append(stat.target == TargetType.OTHER ? " on something" : "");
            sb.append(stat.trigger == TriggerType.ON_HIT ? " on hit" : "");
            sb.append(stat.trigger == TriggerType.ON_SKILL_USE ? " on skill hit" : "");
            list.add(sb.toString());
        });
        return list;
    }

    private void buttonEvent(int index) {
        StatEffectComponent statComp = StatEffectComponent.getPlayerStatComp(playerRef);
        if (statComp == null || artefacts.isEmpty())
            return;
        statComp.addArtifact(artefacts.get(index).artefact);

        World world = store.getExternalData().getWorld();
        GameManager manager = GamePlugin.get().getGameManagers().get(world);

        if (manager != null) {
            manager.onPlayerClaimedReward(this.playerRef);
        }

        if (this.page != null) {
            this.page.close();
        }
    }

    private record ArtefactInfos(String name, String icon, String description, ArrayList<String> status,
                                 ArrayList<String> stats, ArtefactCodec artefact, int rarity, int index) {
    }
}
