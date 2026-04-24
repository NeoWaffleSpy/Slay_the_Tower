package com.Team_Berry.Artefacts.UI;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.Data.StatEffectComponent;
import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTranslationProperties;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkillSelection {
    private final PlayerRef playerRef;
    private final Store<EntityStore> store;
    private final TemplateProcessor template = new TemplateProcessor();
    private final List<SkillInfos> skills = new ArrayList<>();
    private HyUIPage page;

    public SkillSelection(PlayerRef playerRef, Store<EntityStore> store) {
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

    private ArrayList<Item> getSkillsFromItemList() {
        ArrayList<Item> items = new ArrayList<>();
        DefaultAssetMap<String, Item> itemMap = Item.getAssetMap();
        for (String skill : skillStringList) {
           items.add(itemMap.getAsset(skill));
        }
        return items;
    }

    public void buildPage() {
        List<Item> skillsCodecs = getSkillsFromItemList();
        if (skillsCodecs.size() < 3)
            return;
        for (int i = 0; i < 3; i++)
            skills.add(parseInfo(skillsCodecs.get(i), i));
        this.template.setVariable("Skills", skills);
        PageBuilder builder = PageBuilder.pageForPlayer(this.playerRef).loadHtml("Pages/SkillSelection.html", this.template);
        skills.forEach((s) -> builder.addEventListener("my-button-" + s.index, CustomUIEventBindingType.Activating, (_, _) -> this.buttonEvent(s.index)));
        page = builder.open(store);
    }

    public void buildPageWithList(List<Item> items) {
        for (int i = 0; i < items.size(); i++)
            skills.add(parseInfo(items.get(i), i));
        this.template.setVariable("Skills", skills);
        PageBuilder builder = PageBuilder.pageForPlayer(this.playerRef).loadHtml("Pages/SkillSelection.html", this.template);
        skills.forEach((s) -> builder.addEventListener("my-button-" + s.index, CustomUIEventBindingType.Activating, (_, _) -> this.buttonEvent(s.index)));
        page = builder.open(store);
    }

    private SkillInfos parseInfo(Item item, int index) {
        if (item == null)
            return null;
        ItemTranslationProperties tl = item.getTranslationProperties();
        if (tl == null)
            tl = new ItemTranslationProperties("Template", "Template");
        String name = tl.getName();
        if (tl.getName() != null)
            name = I18nModule.get().getMessage("en-US", tl.getName());
        if (name == null)
            name = item.getId();
        String description = tl.getDescription();
        if (tl.getDescription() != null)
            description = I18nModule.get().getMessage("en-US", tl.getDescription());
        if (description == null)
            description = "Template";
        String icon = item.getIcon().replace("icons/ItemsGenerated/", "");
        return new SkillInfos(
                name,
                icon,
                description,
                item,
                index);
    }

    private void buttonEvent(int index) {
        StatEffectComponent statComp = StatEffectComponent.getPlayerStatComp(playerRef);
        if (statComp == null || skills.isEmpty())
            return;

        Ref<EntityStore> ref = playerRef.getReference();
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        ItemStack stack = new ItemStack(skills.get(index).item.getId(), 1, null);
        ItemStackTransaction transaction = playerComponent.giveItem(stack, ref, store);
        ItemStack remainder = transaction.getRemainder();
        Message itemNameMessage = Message.translation(skills.get(index).item.getTranslationKey());
        if (remainder != null && !remainder.isEmpty()) {
            ArtefactPlugin.LOGGER.atSevere().log(Message.translation("server.commands.give.insufficientInvSpace").param("quantity", 1).param("item", itemNameMessage).toString());
        } else {
            ArtefactPlugin.LOGGER.atSevere().log(Message.translation("server.commands.give.received").param("quantity", 1).param("item", itemNameMessage).toString());
        }

        World world = store.getExternalData().getWorld();
        GameManager manager = GamePlugin.get().getGameManagers().get(world);

        if (manager != null) {
            //manager.onPlayerClaimedReward(this.playerRef);
        }

        if (this.page != null) {
            this.page.close();
        }
    }

    private record SkillInfos(String name, String icon, String description, Item item, int index) {}
    private static final String[] skillStringList = new String[] {
            "Hotbar_Second_Wind",
            "Hotbar_Shadow_Dash",
            "Hotbar_Slow_Bomb"
    };
}
