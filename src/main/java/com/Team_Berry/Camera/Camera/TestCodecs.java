package com.Team_Berry.Camera.Camera;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

import java.util.Collection;

public class TestCodecs implements JsonAssetWithMap<String, DefaultAssetMap<String, TestCodecs>> {
    private static AssetStore<String, TestCodecs, DefaultAssetMap<String, TestCodecs>> ASSET_STORE;
    public static final AssetBuilderCodec<String, TestCodecs> CODEC;

    private String id;
    private String className = "template";
    private AssetExtraInfo.Data data;
    private int value = 1;

    public TestCodecs() {}
    public TestCodecs(String id) { this.id = id; }
    public TestCodecs(String id, String className, AssetExtraInfo.Data data, int value) {
        this.id = id;
        this.className = className;
        this.data = data;
        this.value = value;
    }

    public static AssetStore<String, TestCodecs, DefaultAssetMap<String, TestCodecs>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(TestCodecs.class);
        }
        return ASSET_STORE;
    }

    public static Collection<TestCodecs> getAllLoot() {
        return getAssetStore().getAssetMap().getAssetMap().values();
    }

    @Override
    public String getId() { return id; }
    public String getClassName() { return className; }
    public int getValue() { return value; }

    static {
        CODEC = AssetBuilderCodec.builder(
                        TestCodecs.class,
                        TestCodecs::new,
                        Codec.STRING,
                        (asset, id) ->  asset.id = id,
                        (config) -> config.id,
                        (config, data) -> config.data = data,
                        (config) -> config.data)
                .append(
                        new KeyedCodec<>("ClassName", Codec.STRING),
                        (obj, val) -> obj.className = val,
                        TestCodecs::getClassName)
                .add()
                .append(
                        new KeyedCodec<>("Value", Codec.INTEGER),
                        (obj, val) -> obj.value = val,
                        TestCodecs::getValue)
                .add()
                .build();
    }
}
