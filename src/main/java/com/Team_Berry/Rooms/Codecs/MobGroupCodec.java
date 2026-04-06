package com.Team_Berry.Rooms.Codecs;

import com.Team_Berry.Rooms.RoomPlugin;
import com.Team_Berry.Utils.Codecs.CustomArrayCodec;
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
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

import java.util.ArrayList;

public class MobGroupCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, MobGroupCodec>> {
    public static final AssetBuilderCodec<String, MobGroupCodec> CODEC;
    private static AssetStore<String, MobGroupCodec, DefaultAssetMap<String, MobGroupCodec>> ASSET_STORE;

    static {
        CODEC = AssetBuilderCodec.builder(MobGroupCodec.class, MobGroupCodec::new, Codec.STRING,
                        (t, k) -> t.id = k, (t) -> t.id,
                        (asset, data) -> asset.data = data, (asset) -> asset.data)
                .append(new KeyedCodec<>("Difficulty", Codec.INTEGER, true),
                        (obj, val) -> obj.difficulty = val,
                        obj -> obj.difficulty)
                .documentation("The difficulty tier of this mob group (e.g., Easy, Medium, Hard).").add()
                .append(new KeyedCodec<>("Mobs", new CustomArrayCodec<>(MobEntry.CODEC, ArrayList::new)),
                        (obj, val) -> obj.mobs = val,
                        obj -> obj.mobs)
                .documentation("List of NPC roles and their quantities in this group.").add()
                .build();
    }

    public int difficulty = 1;
    public ArrayList<MobEntry> mobs = new ArrayList<>();
    private String id = "NewMobGroup";
    private AssetExtraInfo.Data data;

    public MobGroupCodec() {
    }

    public MobGroupCodec(String id) {
        this.id = id;
    }

    public static AssetStore<String, MobGroupCodec, DefaultAssetMap<String, MobGroupCodec>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(MobGroupCodec.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, MobGroupCodec> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public static void register() {
        RoomPlugin plugin = RoomPlugin.get();
        plugin.getAssetRegistry()
                .register(HytaleAssetStore.builder(MobGroupCodec.class, new DefaultAssetMap<>())
                        .setPath("MobGroups")
                        .setCodec(MobGroupCodec.CODEC)
                        .setKeyFunction(MobGroupCodec::getId)
                        .setReplaceOnRemove(MobGroupCodec::new)
                        .build());

        plugin.getEventRegistry().register(LoadedAssetsEvent.class, MobGroupCodec.class, MobGroupCodec::onLoaded);
        plugin.getEventRegistry().register(RemovedAssetsEvent.class, MobGroupCodec.class, MobGroupCodec::onRemoved);
    }

    public static void onLoaded(LoadedAssetsEvent<String, MobGroupCodec, DefaultAssetMap<String, MobGroupCodec>> event) {
    }

    public static void onRemoved(RemovedAssetsEvent<String, MobGroupCodec, DefaultAssetMap<String, MobGroupCodec>> event) {
    }

    @Override
    public String getId() {
        return this.id;
    }

    public static class MobEntry {
        public static final BuilderCodec<MobEntry> CODEC = BuilderCodec.builder(MobEntry.class, MobEntry::new)
                .append(new KeyedCodec<>("NPCRole", Codec.STRING, true),
                        (obj, val) -> obj.npcRoleId = val,
                        obj -> obj.npcRoleId).add()
                .append(new KeyedCodec<>("Quantity", Codec.INTEGER),
                        (obj, val) -> obj.quantity = (val != null) ? val : 1,
                        obj -> obj.quantity).add()
                .build();

        public String npcRoleId = "Zombie_Kweebec";
        public int quantity = 1;
    }
}