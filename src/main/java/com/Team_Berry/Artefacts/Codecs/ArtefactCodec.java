package com.Team_Berry.Artefacts.Codecs;

import com.Team_Berry.Artefacts.ArtefactPlugin;
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
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTranslationProperties;

import java.awt.*;
import java.util.*;

public class ArtefactCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, ArtefactCodec>> {
    private static AssetStore<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> ASSET_STORE;
    public static final AssetBuilderCodec<String, ArtefactCodec> CODEC;

    private String artefactName = "Template";
    private AssetExtraInfo.Data data;

    public ArrayList<StatusEffectCodec> statusList = new ArrayList<>();
    public ArrayList<StatCodec> statList = new ArrayList<>();
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

    public static Collection<ArtefactCodec> getAssetMap() {
        return getAssetStore().getAssetMap().getAssetMap().values();
    }

    static {
        CODEC = AssetBuilderCodec.builder(ArtefactCodec.class, ArtefactCodec::new, Codec.STRING,
                        (t, k) -> t.artefactName = k, (t) -> t.artefactName,
                        (asset, data) -> asset.data = data, (asset) -> asset.data)
                .append(new KeyedCodec<>("StatusEffect", new CustomArrayCodec<>(Codec.STRING, ArrayList::new)
                                .metadata(new UIEditor(new UIEditor.Dropdown("StatEffectDataSet")))),
                        (artefact, map) -> artefact.statusList = getStatusEffectFromStringList(map),
                        (artefact) -> getStatusStringList(artefact.statusList)).add()
                .append(new KeyedCodec<>("Stats", new CustomArrayCodec<>(Codec.STRING, ArrayList::new)
                                .metadata(new UIEditor(new UIEditor.Dropdown("StatDataSet")))),
                        (artefact, map) -> artefact.statList = getStatFromStringList(map),
                        (artefact) -> getStatStringList(artefact.statList)).add()
                .append(new KeyedCodec<>("TranslationProperties", ItemTranslationProperties.CODEC),
                        (artefact, s) -> artefact.translationProperties = s,
                        (artefact) -> artefact.translationProperties)
                .documentation("The translation properties for this item asset.").add()
                .build();
    }

    public static ArrayList<StatusEffectCodec> getStatusEffectFromStringList(ArrayList<String> sList) {
        Collection<StatusEffectCodec> col = StatusEffectCodec.getAssetMap();
        ArrayList<StatusEffectCodec> list = new ArrayList<>();
        sList.forEach((s) -> col.forEach((status) -> {
            if (Objects.equals(status.getId(), s))
                list.add(status);
        }));
        return list;
    }

    public static ArrayList<StatCodec> getStatFromStringList(ArrayList<String> sList) {
        Collection<StatCodec> col = StatCodec.getAssetMap();
        ArrayList<StatCodec> list = new ArrayList<>();
        sList.forEach((s) -> col.forEach((status) -> {
            if (Objects.equals(status.getId(), s))
                list.add(status);
        }));
        return list;
    }

    public static ArrayList<String> getStatusStringList(ArrayList<StatusEffectCodec> list) {
        ArrayList<String> val = new ArrayList<>();
        list.forEach((status) -> val.add(new String(status.getId())));
        return val;
    }

    public static ArrayList<String> getStatStringList(ArrayList<StatCodec> list) {
        ArrayList<String> val = new ArrayList<>();
        list.forEach((status) -> val.add(new String(status.getId())));
        return val;
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
        Collection<StatusEffectCodec> statusEffect = StatusEffectCodec.getAssetMap();
        String[] s = statusEffect.stream().map(StatusEffectCodec::getId).toArray(String[]::new);
        ArtefactPlugin.LOGGER.atInfo().log("Registering request assets for dataset " + Arrays.toString(s));
        event.setResults(s);
    }

    public static void registerStatRequestDataset(AssetEditorRequestDataSetEvent event) {
        Collection<StatCodec> stats = StatCodec.getAssetMap();
        String[] s = stats.stream().map(StatCodec::getId).toArray(String[]::new);
        ArtefactPlugin.LOGGER.atInfo().log("Registering request assets for dataset " + Arrays.toString(s));
        event.setResults(s);
    }
}
