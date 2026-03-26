package com.Team_Berry.Artefacts.Codecs.Stats;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Enums.StatEnum;
import com.Team_Berry.Artefacts.Codecs.Enums.TargetType;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.protocol.CalculationType;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

import java.util.Collection;

public class StatCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, StatCodec>> {
    private static AssetStore<String, StatCodec, DefaultAssetMap<String, StatCodec>> ASSET_STORE;
    public static final AssetBuilderCodec<String, StatCodec> CODEC;

    private String effectName = "Template";
    private AssetExtraInfo.Data data;

    private StatEnum stat = StatEnum.NONE;
    private CalculationType calc = CalculationType.Additive;
    private TargetType target = TargetType.ENEMY;
    private float value = 1.0f;
    private float duration = 1.0f;

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

    public static Collection<StatCodec> getAssetMap() {
        return getAssetStore().getAssetMap().getAssetMap().values();
    }

    public void updateCodecSetting(String name) {
    }

    public static void remove(String s) {
    }

    static {
        CODEC = AssetBuilderCodec.builder(StatCodec.class, StatCodec::new, Codec.STRING,
                        (t, k) -> t.effectName = k, (t) -> t.effectName,
                        (asset, data) -> asset.data = data, (asset) -> asset.data)
                .append(new KeyedCodec<StatEnum>("StatusEffect", new EnumCodec(StatEnum.class)),
                        (obj, val) -> obj.stat = val,
                        obj -> obj.stat).add()
                .append(new KeyedCodec<CalculationType>("CalculationType", new EnumCodec(CalculationType.class)),
                        (obj, val) -> obj.calc = val,
                        obj -> obj.calc).add()
                .append(new KeyedCodec<TargetType>("TargetType", new EnumCodec(TargetType.class)),
                        (obj, val) -> obj.target = val,
                        obj -> obj.target).add()
                .append(new KeyedCodec<>("Value", Codec.FLOAT),
                        (obj, val) -> obj.value = val,
                        obj -> obj.value).add()
                .append(new KeyedCodec<>("Duration", Codec.FLOAT),
                        (obj, val) -> obj.duration = val,
                        obj -> obj.duration).add()
                .build();
    }

    public static void register() {
        ArtefactPlugin a = ArtefactPlugin.get();
        a.getAssetRegistry()
                .register(HytaleAssetStore.builder(StatCodec.class, new DefaultAssetMap<>())
                .setPath("StatCodec")
                .setCodec(StatCodec.CODEC)
                .setKeyFunction(StatCodec::getId)
                .setReplaceOnRemove(StatCodec::new)
                .build());
        a.getEventRegistry().register(LoadedAssetsEvent.class, StatCodec.class, StatCodec::onLoaded);
        a.getEventRegistry().register(RemovedAssetsEvent.class, StatCodec.class, StatCodec::onRemoved);
    }

    public static void onLoaded(LoadedAssetsEvent<String, StatCodec, DefaultAssetMap<String, StatCodec>> event) {
        event.getLoadedAssets().forEach((name, codec) -> codec.updateCodecSetting(name));
    }

    public static void onRemoved(RemovedAssetsEvent<String, StatCodec, DefaultAssetMap<String, StatCodec>> event) {
        event.getRemovedAssets().forEach(StatCodec::remove);
    }
}
