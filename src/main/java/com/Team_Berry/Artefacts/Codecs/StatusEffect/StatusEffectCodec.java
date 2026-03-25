package com.Team_Berry.Artefacts.Codecs.StatusEffect;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

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

    public static void updateCodecSetting(String name) {
    }

    public static void remove(String s) {
    }

    public static Collection<StatusEffectCodec> getAssetMap() {
        return getAssetStore().getAssetMap().getAssetMap().values();
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
}

