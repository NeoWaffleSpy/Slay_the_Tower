package com.Team_Berry.Artefacts.Codecs.Stats;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.Enums.TargetType;
import com.Team_Berry.Artefacts.Codecs.Enums.TriggerType;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier.CalculationType;

import java.util.Arrays;

public class StatCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, StatCodec>> {
    private static AssetStore<String, StatCodec, DefaultAssetMap<String, StatCodec>> ASSET_STORE;
    public static final AssetBuilderCodec<String, StatCodec> CODEC;

    private String effectName = "Template";
    private AssetExtraInfo.Data data;

    public EntityStatType type;
    public CalculationType calc = CalculationType.ADDITIVE;
    public TargetType target = TargetType.SELF;
    public TriggerType trigger = TriggerType.PASSIVE;
    public float value = 1.0f;
    public float duration = 1.0f;

    public StatCodec() {}
    public StatCodec(String effectName) {
        this.effectName = effectName;
    }

    @Override
    public String getId() {
        return effectName;
    }

    public static AssetStore<String, StatCodec, DefaultAssetMap<String, StatCodec>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(StatCodec.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, StatCodec> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public void updateCodecSetting(String name) {
    }

    public static void remove(String s) {
    }

    static {
        CODEC = AssetBuilderCodec.builder(StatCodec.class, StatCodec::new, Codec.STRING,
                        (t, k) -> t.effectName = k, (t) -> t.effectName,
                        (asset, data) -> asset.data = data, (asset) -> asset.data)
                .append(new KeyedCodec<>("Stat", Codec.STRING),
                        (artefact, map) -> artefact.type = getStatFromString(map),
                        (artefact) -> getStatString(artefact.type))
                .metadata(new UIEditor(new UIEditor.Dropdown("EntityStatTypeDataSet"))).add()
                .append(new KeyedCodec<>("CalculationType", new EnumCodec<>(CalculationType.class)),
                        (obj, val) -> obj.calc = val,
                        obj -> obj.calc).add()
                .append(new KeyedCodec<>("TargetType", new EnumCodec<>(TargetType.class)),
                        (obj, val) -> obj.target = val,
                        obj -> obj.target).add()
                .append(new KeyedCodec<>("TriggerType", new EnumCodec<>(TriggerType.class)),
                        (obj, val) -> obj.trigger = val,
                        obj -> obj.trigger).add()
                .append(new KeyedCodec<>("Value", Codec.FLOAT),
                        (obj, val) -> obj.value = val,
                        obj -> obj.value).add()
                .append(new KeyedCodec<>("Duration", Codec.FLOAT),
                        (obj, val) -> obj.duration = val,
                        obj -> obj.duration).add()
                .build();
    }

    public static void register() {
        ArtefactPlugin artifact = ArtefactPlugin.get();
        artifact.getAssetRegistry()
                .register(HytaleAssetStore.builder(StatCodec.class, new DefaultAssetMap<>())
                .setPath("StatCodec")
                .setCodec(StatCodec.CODEC)
                .setKeyFunction(StatCodec::getId)
                .setReplaceOnRemove(StatCodec::new)
                .build());
        artifact.getEventRegistry().register(LoadedAssetsEvent.class, StatCodec.class, StatCodec::onLoaded);
        artifact.getEventRegistry().register(RemovedAssetsEvent.class, StatCodec.class, StatCodec::onRemoved);
        artifact.getEventRegistry().register(AssetEditorRequestDataSetEvent.class, "EntityStatTypeDataSet", StatCodec::registerStatRequestDataset);
    }

    public static void registerStatRequestDataset(AssetEditorRequestDataSetEvent event) {
        IndexedLookupTableAssetMap<String, EntityStatType> entityStatType = ArtefactPlugin.getEntityStatTypeAssetStore();
        String[] s = entityStatType.getAssetMap().values().stream().map(EntityStatType::getId).toArray(String[]::new);
        ArtefactPlugin.LOGGER.atInfo().log("Registering request assets for dataset " + Arrays.toString(s));
        event.setResults(s);
    }

    public static void onLoaded(LoadedAssetsEvent<String, StatCodec, DefaultAssetMap<String, StatCodec>> event) {
        event.getLoadedAssets().forEach((name, codec) -> codec.updateCodecSetting(name));
    }

    public static void onRemoved(RemovedAssetsEvent<String, StatCodec, DefaultAssetMap<String, StatCodec>> event) {
        event.getRemovedAssets().forEach(StatCodec::remove);
    }

    public static EntityStatType getStatFromString(String key) {
        return ArtefactPlugin.getEntityStatTypeAssetStore().getAsset(key);
    }

    public static String getStatString(EntityStatType stat) {
        if (stat == null)
            return null;
        return stat.getId();
    }
}
