package com.Team_Berry.Artefacts.Codecs;

import com.Team_Berry.Artefacts.Codecs.StatusEffect.StatusEffectCodec;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ArtefactCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, ArtefactCodec>> {
    private static AssetStore<String, ArtefactCodec, DefaultAssetMap<String, ArtefactCodec>> ASSET_STORE;
    public static final AssetBuilderCodec<String, ArtefactCodec> CODEC;

    private String artefactName = "Template";
    private AssetExtraInfo.Data data;

    public ArrayList<StatusEffectCodec> map = new ArrayList<>();

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

    public static void updateCodecSetting(String name) {
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
                .append(new KeyedCodec<ArrayList<StatusEffectCodec>>("StatusEffect", new ArrayCodec<>(StatusEffectCodec.CODEC, StatusEffectCodec[]::new, ArrayList::new)),
                        (artefact, map) -> artefact.map = map,
                        (artefact) -> artefact.map).add()
                .build();
    }
}
