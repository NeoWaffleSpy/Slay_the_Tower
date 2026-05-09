package com.Team_Berry.Artefacts.UI;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Enums.TargetType;
import com.Team_Berry.Artefacts.Codecs.Enums.TriggerType;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Utils.TooltipInjector.TooltipInjector;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTranslationProperties;
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
        this.buildPageWithCount(3);
    }

    public void buildPageWithCount(int count) {
        List<ArtefactCodec> artefactCodecs = weightedDraw(ArtefactCodec.getAssetMap().getAssetMap().values().stream().filter(a -> a.rarity.weight > 0).toList(), count);
        if (artefactCodecs.size() < count)
            return;
        for (int i = 0; i < count; i++)
            artefacts.add(parseInfo(artefactCodecs.get(i), i));
        this.template.setVariable("artefacts", artefacts);
        PageBuilder builder = PageBuilder.pageForPlayer(this.playerRef).loadHtml("Pages/ArtefactSelection.html", this.template).withLifetime(CustomPageLifetime.CantClose);
        artefacts.forEach((s) -> builder.addEventListener("my-button-" + s.index, CustomUIEventBindingType.Activating, (_, _) -> this.buttonEvent(s.index)));
        page = builder.open(store);
    }

    private ArtefactInfos parseInfo(ArtefactCodec artefact, int index) {
        if (artefact == null)
            return null;
        ItemTranslationProperties tl = artefact.translationProperties;
        if (tl == null)
            tl = new ItemTranslationProperties("Template", "Template");
        String name = tl.getName();
        if (tl.getName() != null)
            name = I18nModule.get().getMessage("en-US", tl.getName());
        if (name == null)
            name = artefact.getId();
        String description = tl.getDescription();
        if (tl.getDescription() != null) {
            description = I18nModule.get().getMessage("en-US", tl.getDescription());
            description = TooltipInjector.toHyuiHtml(description, "txt txtDesc");
        }
        if (description == null || description.isEmpty())
            description = "Template";
        return new ArtefactInfos(
                name,
                artefact.shortIconPath,
                description,
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

        ArtefactCodec claimedArtefact = artefacts.get(index).artefact();
        String claimedArtefactId = claimedArtefact.getId();

        statComp.addArtifact(claimedArtefact);

        World world = store.getExternalData().getWorld();
        GameManager manager = GamePlugin.get().getGameManagers().get(world);

        if (manager != null) {
            manager.onPlayerClaimedArtefactReward(this.playerRef, claimedArtefactId);
        }

        if (this.page != null) {
            this.page.close();
        }
    }

    private record ArtefactInfos(String name, String icon, String description, ArrayList<String> status,
                                 ArrayList<String> stats, ArtefactCodec artefact, int rarity, int index) {
    }
}
