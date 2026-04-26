package com.Team_Berry.Artefacts.Codecs;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.Enums.RarityEnum;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Codecs.StatusEffect.StatusEffectCodec;
import com.Team_Berry.Utils.Codecs.AssetWrapperCodec;
import com.Team_Berry.Utils.TooltipInjector.StringFormatter;
import com.Team_Berry.Utils.TooltipInjector.TooltipInjector;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTranslationProperties;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ArtefactCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, ArtefactCodec>> {
    public static final AssetBuilderCodec<String, ArtefactCodec> CODEC;
    public static final CommonAssetValidator ICON_ARTEFACT = new CommonAssetValidator("png", "UI/Custom");
    private static AssetStore<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> ASSET_STORE;
    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(ArtefactCodec::getAssetStore));

    static {
        CODEC = AssetBuilderCodec.builder(ArtefactCodec.class, ArtefactCodec::new, Codec.STRING,
                        (t, k) -> t.artefactName = k, (t) -> t.artefactName,
                        (asset, data) -> asset.data = data, (asset) -> asset.data)
                /*.metadata(new UIEditorPreview(UIEditorPreview.PreviewType.ITEM))
                .metadata(new UITypeIcon("Item.png"))
                .metadata(new UIRebuildCaches(false, UIRebuildCaches.ClientCache.ITEM_ICONS))*/
                .append(new KeyedCodec<>("Icon", Codec.STRING),
                        (artefact, s) -> artefact.icon = s,
                        (artefact) -> artefact.icon)
                .addValidator(ICON_ARTEFACT)
                /*.metadata(new UIEditor(new UIEditor.Icon("Icons/ItemsGenerated/{assetId}.png", 64, 64)))
                .metadata(new UIRebuildCaches(UIRebuildCaches.ClientCache.ITEM_ICONS))*/.add()
                .append(new KeyedCodec<>("StatusEffect", new ArrayCodec<>(new AssetWrapperCodec<>(StatusEffectCodec.class, StatusEffectCodec.CODEC), String[]::new)),
                        (artefact, map) -> artefact.statusList = map,
                        (artefact) -> artefact.statusList)
                .addValidatorLate(() -> StatusEffectCodec.VALIDATOR_CACHE.getArrayValidator().late()).add()
                .append(new KeyedCodec<>("Stats", new ArrayCodec<>(new AssetWrapperCodec<>(StatCodec.class, StatCodec.CODEC), String[]::new)),
                        (artefact, map) -> artefact.statList = map,
                        (artefact) -> artefact.statList)
                .addValidatorLate(() -> StatCodec.VALIDATOR_CACHE.getArrayValidator().late()).add()
                .append(new KeyedCodec<>("Rarity", new EnumCodec<>(RarityEnum.class)),
                        (obj, val) -> obj.rarity = val,
                        obj -> obj.rarity).add()
                .append(new KeyedCodec<>("TranslationProperties", ItemTranslationProperties.CODEC),
                        (artefact, s) -> artefact.translationProperties = s,
                        (artefact) -> artefact.translationProperties)
                .documentation("The translation properties for this item asset.").add()
                .afterDecode(ArtefactCodec::process)
                .build();
    }

    public String artefactName = "Template";
    public String icon = null;
    public String shortIconPath = null;
    public String[] statusList;
    public String[] statList;
    public RarityEnum rarity = RarityEnum.DEBUG;
    public ItemTranslationProperties translationProperties = new ItemTranslationProperties("server.artefact." + this.artefactName + ".name", "server.artefact." + this.artefactName + ".description");
    private AssetExtraInfo.Data data;

    public ArtefactCodec() {
    }

    public ArtefactCodec(String artefactName) {
        this.artefactName = artefactName;
    }

    public static AssetStore<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(ArtefactCodec.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, ArtefactCodec> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public static void remove(String s) {
    }

    public static void register() {
        ArtefactPlugin a = ArtefactPlugin.get();
        a.getAssetRegistry()
                .register(HytaleAssetStore.builder(ArtefactCodec.class, new DefaultAssetMap<>())
                        .setPath("Artefacts")
                        .setCodec(ArtefactCodec.CODEC)
                        .setKeyFunction(ArtefactCodec::getId)
                        .setReplaceOnRemove(ArtefactCodec::new)
                        .build());
        a.getEventRegistry().register(LoadedAssetsEvent.class, ArtefactCodec.class, ArtefactCodec::onLoaded);
        a.getEventRegistry().register(RemovedAssetsEvent.class, ArtefactCodec.class, ArtefactCodec::onRemoved);
    }

    public static void onLoaded(LoadedAssetsEvent<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> event) {
        event.getLoadedAssets().forEach((name, codec) -> codec.updateCodecSetting(name));
    }

    public static void onRemoved(RemovedAssetsEvent<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> event) {
        event.getRemovedAssets().forEach(ArtefactCodec::remove);
    }

    @Override
    public String getId() {
        return artefactName;
    }

    public void updateTranslation() {
        if (translationProperties == null
                || translationProperties.getName() == null
                || translationProperties.getDescription() == null)
            return;
        TooltipInjector.setItemTranslation(translationProperties.getName(),
                new StringFormatter().clear().color(Color.RED).append(artefactName));
        StringFormatter sf = new StringFormatter().clear().color(Color.GREEN)
                .append(TooltipInjector.getItemTranslation(translationProperties.getDescription()));
        TooltipInjector.setItemTranslation(translationProperties.getName(), sf);
    }

    public void updateCodecSetting(String name) {
        updateTranslation();
    }

    public ArrayList<String> statusStringArray() {
        return new ArrayList<>(List.of(statusList));
    }

    public ArrayList<String> statStringArray() {
        if (this.statList == null) {
            return new ArrayList<>(); // Return an empty ArrayList safely
        }
        return new ArrayList<>(List.of(this.statList));
    }

    private void process(ExtraInfo u) {
        if (this.icon == null)
            return;
        this.shortIconPath = this.icon.replace("UI/Custom/", "");
    }

    public ArrayList<StatusEffectCodec> getStatusEffectArray() {
        DefaultAssetMap<String, StatusEffectCodec> map = StatusEffectCodec.getAssetMap();
        ArrayList<StatusEffectCodec> list = new ArrayList<>();
        statusStringArray().forEach((s) -> list.add(map.getAsset(s)));
        return list;
    }

    public ArrayList<StatCodec> getStatArray() {
        DefaultAssetMap<String, StatCodec> map = StatCodec.getAssetMap();
        ArrayList<StatCodec> list = new ArrayList<>();
        statStringArray().forEach((s) -> list.add(map.getAsset(s)));
        return list;
    }
}
