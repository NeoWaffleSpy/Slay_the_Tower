package com.Team_Berry.Artefacts.Codecs;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.Enums.RarityEnum;
import com.Team_Berry.Artefacts.Codecs.Stats.StatCodec;
import com.Team_Berry.Artefacts.Codecs.StatusEffect.StatusEffectCodec;
import com.Team_Berry.Utils.Codecs.CustomArrayCodec;
import com.Team_Berry.Utils.TooltipInjector.StringFormatter;
import com.Team_Berry.Utils.TooltipInjector.TooltipInjector;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.*;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import com.hypixel.hytale.server.core.asset.type.item.config.AssetIconProperties;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTranslationProperties;

import java.awt.*;
import java.util.*;

public class ArtefactCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, ArtefactCodec>> {
    private static AssetStore<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> ASSET_STORE;
    public static final AssetBuilderCodec<String, ArtefactCodec> CODEC;

    private String artefactName = "Template";
    private AssetExtraInfo.Data data;
    public String icon = null;
    protected AssetIconProperties iconProperties;

    public ArrayList<String> statusStringList = new ArrayList<>();
    public ArrayList<StatusEffectCodec> statusList = new ArrayList<>();
    public ArrayList<String> statStringList = new ArrayList<>();
    public ArrayList<StatCodec> statList = new ArrayList<>();
    private RarityEnum rarity = RarityEnum.DEBUG;
    private ItemTranslationProperties translationProperties = new ItemTranslationProperties("server.artefact." + this.artefactName + ".name", "server.artefact." + this.artefactName + ".description");

    public ArtefactCodec() {}
    public ArtefactCodec(String artefactName) {
        this.artefactName = artefactName;
    }

    @Override
    public String getId() {
        return artefactName;
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

    public static void remove(String s) {
    }

    static {
        CODEC = AssetBuilderCodec.builder(ArtefactCodec.class, ArtefactCodec::new, Codec.STRING,
                        (t, k) -> t.artefactName = k, (t) -> t.artefactName,
                        (asset, data) -> asset.data = data, (asset) -> asset.data)
                .metadata(new UIEditorPreview(UIEditorPreview.PreviewType.ITEM))
                .metadata(new UITypeIcon("Item.png"))
                .metadata(new UIRebuildCaches(false, UIRebuildCaches.ClientCache.ITEM_ICONS))
                .append(new KeyedCodec<>("Icon", Codec.STRING),
                        (artefact, s) -> artefact.icon = s,
                        (artefact) -> artefact.icon)
                .addValidator(CommonAssetValidator.ICON_ITEM)
                .metadata(new UIEditor(new UIEditor.Icon("Icons/ItemsGenerated/{assetId}.png", 64, 64)))
                .metadata(new UIRebuildCaches(UIRebuildCaches.ClientCache.ITEM_ICONS)).add()
                .append(new KeyedCodec<>("IconProperties", AssetIconProperties.CODEC),
                        (item, s) -> item.iconProperties = s,
                        (item) -> item.iconProperties)
                .metadata(UIDisplayMode.HIDDEN).add()
                .append(new KeyedCodec<>("StatusEffect", new CustomArrayCodec<>(Codec.STRING, ArrayList::new)
                                .metadata(new UIEditor(new UIEditor.Dropdown("StatEffectDataSet")))),
                        (artefact, map) -> artefact.statusStringList = getStatusEffectFromStringList(artefact, map),
                        (artefact) -> artefact.statusStringList).add()
                .append(new KeyedCodec<>("Stats", new CustomArrayCodec<>(Codec.STRING, ArrayList::new)
                                .metadata(new UIEditor(new UIEditor.Dropdown("StatDataSet")))),
                        (artefact, map) -> artefact.statStringList = getStatFromStringList(artefact, map),
                        (artefact) -> artefact.statStringList).add()
                .append(new KeyedCodec<>("Rarity", new EnumCodec<>(RarityEnum.class)),
                        (obj, val) -> obj.rarity = val,
                        obj -> obj.rarity).add()
                .append(new KeyedCodec<>("TranslationProperties", ItemTranslationProperties.CODEC),
                        (artefact, s) -> artefact.translationProperties = s,
                        (artefact) -> artefact.translationProperties)
                .documentation("The translation properties for this item asset.").add()
                .build();
    }

    public static ArrayList<String> getStatusEffectFromStringList(ArtefactCodec codec, ArrayList<String> sList) {
        DefaultAssetMap<String, StatusEffectCodec> map = StatusEffectCodec.getAssetMap();
        codec.statusList.clear();
        sList.forEach((s) -> codec.statusList.add(map.getAsset(s)));
        return sList;
    }

    public static ArrayList<String> getStatFromStringList(ArtefactCodec codec, ArrayList<String> sList) {
        DefaultAssetMap<String, StatCodec> map = StatCodec.getAssetMap();
        codec.statList.clear();
        sList.forEach((s) -> codec.statList.add(map.getAsset(s)));
        return sList;
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
        a.getEventRegistry().register(AssetEditorRequestDataSetEvent.class, "StatEffectDataSet", ArtefactCodec::registerStatusEffectRequestDataset);
        a.getEventRegistry().register(AssetEditorRequestDataSetEvent.class, "StatDataSet", ArtefactCodec::registerStatRequestDataset);
    }

    public static void onLoaded(LoadedAssetsEvent<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> event) {
        event.getLoadedAssets().forEach((name, codec) -> codec.updateCodecSetting(name));
    }

    public static void onRemoved(RemovedAssetsEvent<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> event) {
        event.getRemovedAssets().forEach(ArtefactCodec::remove);
    }

    public static void registerStatusEffectRequestDataset(AssetEditorRequestDataSetEvent event) {
        DefaultAssetMap<String, StatusEffectCodec> statusEffect = StatusEffectCodec.getAssetMap();
        String[] s = statusEffect.getAssetMap().values().stream().map(StatusEffectCodec::getId).toArray(String[]::new);
        ArtefactPlugin.LOGGER.atInfo().log("Registering request assets for dataset " + Arrays.toString(s));
        event.setResults(s);
    }

    public static void registerStatRequestDataset(AssetEditorRequestDataSetEvent event) {
        DefaultAssetMap<String, StatCodec> stats = StatCodec.getAssetMap();
        String[] s = stats.getAssetMap().values().stream().map(StatCodec::getId).toArray(String[]::new);
        ArtefactPlugin.LOGGER.atInfo().log("Registering request assets for dataset " + Arrays.toString(s));
        event.setResults(s);
    }
}
