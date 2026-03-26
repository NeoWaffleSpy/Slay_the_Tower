package com.Team_Berry.Artefacts.Codecs.StatusEffect;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Codecs.Enums.StatusEffectEnum;
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
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

import java.util.Collection;

public class StatusEffectCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, StatusEffectCodec>> {
    private static AssetStore<String, StatusEffectCodec, DefaultAssetMap<String, StatusEffectCodec>> ASSET_STORE;
    public static final AssetBuilderCodec<String, StatusEffectCodec> CODEC;

    private String effectName = "Template";
    private AssetExtraInfo.Data data;

    private StatusEffectEnum effect = StatusEffectEnum.NONE;
    private TargetType target = TargetType.ENEMY;
    private float value = 1.0f;
    private float duration = 1.0f;

    public StatusEffectCodec() {}
    public StatusEffectCodec(String effectName) {
        this.effectName = effectName;
    }

    @Override
    public String getId() {
        return effectName;
    }

    public static AssetStore<String, StatusEffectCodec, DefaultAssetMap<String, StatusEffectCodec>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(StatusEffectCodec.class);
        }
        return ASSET_STORE;
    }

    public static Collection<StatusEffectCodec> getAssetMap() {
        return getAssetStore().getAssetMap().getAssetMap().values();
    }

    public void updateCodecSetting(String name) {
    }

    public static void remove(String s) {
    }

    static {
        CODEC = AssetBuilderCodec.builder(StatusEffectCodec.class, StatusEffectCodec::new, Codec.STRING,
                        (t, k) -> t.effectName = k, (t) -> t.effectName,
                        (asset, data) -> asset.data = data, (asset) -> asset.data)
                .append(new KeyedCodec<StatusEffectEnum>("StatusEffect", new EnumCodec(StatusEffectEnum.class)),
                        (obj, val) -> obj.effect = val,
                        obj -> obj.effect).add()
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
                .register(HytaleAssetStore.builder(StatusEffectCodec.class, new DefaultAssetMap<>())
                .setPath("StatusEffects")
                .setCodec(StatusEffectCodec.CODEC)
                .setKeyFunction(StatusEffectCodec::getId)
                .setReplaceOnRemove(StatusEffectCodec::new)
                .build());
        a.getEventRegistry().register(LoadedAssetsEvent.class, StatusEffectCodec.class, StatusEffectCodec::onLoaded);
        a.getEventRegistry().register(RemovedAssetsEvent.class, StatusEffectCodec.class, StatusEffectCodec::onRemoved);
    }

    public static void onLoaded(LoadedAssetsEvent<String, StatusEffectCodec, DefaultAssetMap<String, StatusEffectCodec>> event) {
        event.getLoadedAssets().forEach((name, codec) -> codec.updateCodecSetting(name));
    }

    public static void onRemoved(RemovedAssetsEvent<String, StatusEffectCodec, DefaultAssetMap<String, StatusEffectCodec>> event) {
        event.getRemovedAssets().forEach(StatusEffectCodec::remove);
    }
}

