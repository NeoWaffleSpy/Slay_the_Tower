package com.Team_Berry.Artefacts.UI;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Utils.TooltipInjector.TooltipInjector;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
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

public class SkillSelection {
    private static final String[] skillStringList = new String[]{
            "Hotbar_Second_Wind",
            "Hotbar_Burst",
            "Hotbar_Dagger_Throw"
    };
    private final PlayerRef playerRef;
    private final Store<EntityStore> store;
    private final TemplateProcessor template = new TemplateProcessor();
    private final List<SkillInfos> skills = new ArrayList<>();
    private HyUIPage page;

    public SkillSelection(PlayerRef playerRef, Store<EntityStore> store) {
        this.playerRef = playerRef;
        this.store = store;
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
        PageBuilder builder = PageBuilder.pageForPlayer(this.playerRef).loadHtml("Pages/SkillSelection.html", this.template).withLifetime(CustomPageLifetime.CantClose);
        ;
        skills.forEach((s) -> builder.addEventListener("my-button-" + s.index, CustomUIEventBindingType.Activating, (_, _) -> this.buttonEvent(s.index)));
        page = builder.open(store);
    }

    public void buildPageWithList(List<Item> items) {
        for (int i = 0; i < items.size(); i++)
            skills.add(parseInfo(items.get(i), i));
        this.template.setVariable("Skills", skills);
        PageBuilder builder = PageBuilder.pageForPlayer(this.playerRef).loadHtml("Pages/SkillSelection.html", this.template).withLifetime(CustomPageLifetime.CantClose);
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
        if (tl.getDescription() != null) {
            description = I18nModule.get().getMessage("en-US", tl.getDescription());
            description = TooltipInjector.toHyuiHtml(description, "txt txtDesc");
        }
        if (description == null || description.isEmpty())
            description = "Template";
        String icon = item.getIcon().replace("Icons/ItemsGenerated/", "SkillIcons/");
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

        String claimedSkillId = skills.get(index).item().getId();

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
            manager.onPlayerClaimedSkillReward(this.playerRef, claimedSkillId);
        }

        if (this.page != null) {
            this.page.close();
        }
    }

    private record SkillInfos(String name, String icon, String description, Item item, int index) {
    }
}
