package com.Team_Berry.Artefacts.Codecs.Stats;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.Enums.TargetType;
import com.Team_Berry.Artefacts.Codecs.Enums.TriggerType;
import com.Team_Berry.Utils.Codecs.AssetWrapperCodec;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDisplayMode;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier.CalculationType;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class StatCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, StatCodec>> {
    private static AssetStore<String, StatCodec, DefaultAssetMap<String, StatCodec>> ASSET_STORE;
    public static final AssetBuilderCodec<String, StatCodec> CODEC;
    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(StatCodec::getAssetStore));

    private String effectName = "Template";
    private AssetExtraInfo.Data data;

    public String type = "Health";
    public CalculationType calc = CalculationType.ADDITIVE;
    public TargetType target = TargetType.SELF;
    public TriggerType trigger = TriggerType.PASSIVE;
    public float value = 1.0f;
    public float duration = 1.0f;
    public float probability = 1.0f;

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

    public EntityStatType getType() {
        return getStatFromString(this.type);
    }

    public void updateCodecSetting(String name) {
    }

    public static void remove(String s) {
    }

    static {
        CODEC = AssetBuilderCodec.builder(StatCodec.class, StatCodec::new, Codec.STRING,
                        (t, k) -> t.effectName = k, (t) -> t.effectName,
                        (asset, data) -> asset.data = data, (asset) -> asset.data)
                .append(new KeyedCodec<>("Stat", new AssetWrapperCodec<>(EntityStatType.class, EntityStatType.CODEC)),
                        (artefact, s) -> artefact.type = s,
                        (artefact) -> artefact.type)
                .addValidator(EntityStatType.VALIDATOR_CACHE.getValidator()).add()
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
                .append(new KeyedCodec<>("Probability", Codec.FLOAT),
                        (obj, val) -> obj.probability = val,
                        obj -> obj.probability)
                .addValidator(Validators.min(0f)).addValidator(Validators.max(1f)).add()
                .build();
    }

    public static void register() {
        ArtefactPlugin artifact = ArtefactPlugin.get();
        artifact.getAssetRegistry()
                .register(HytaleAssetStore.builder(StatCodec.class, new DefaultAssetMap<>())
                .loadsAfter(EntityStatType.class)
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

    @Nonnull
    public String toString() {
        return "StatCodec{effectName='" + effectName + "', type='" + type + "', calculations='" + calc + "', target='" + target + "', trigger='" + trigger + "', value='" + value + "', duration='" + duration + "'}";
    }
}
