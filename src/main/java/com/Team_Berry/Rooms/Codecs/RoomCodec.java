package com.Team_Berry.Rooms.Codecs;

import com.Team_Berry.Rooms.RoomPlugin;
import com.Team_Berry.Utils.Codecs.CodecUtils;
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
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

public class RoomCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, RoomCodec>> {
    public static final AssetBuilderCodec<String, RoomCodec> CODEC;
    private static AssetStore<String, RoomCodec, DefaultAssetMap<String, RoomCodec>> ASSET_STORE;

    static {
        CODEC = AssetBuilderCodec.builder(RoomCodec.class, RoomCodec::new, Codec.STRING,
                        (t, k) -> t.roomName = k, (t) -> t.roomName,
                        (asset, data) -> asset.data = data, (asset) -> asset.data)
                .append(new KeyedCodec<>("WorldName", Codec.STRING, true),
                        (obj, val) -> obj.worldName = val,
                        obj -> obj.worldName)
                .documentation("The World where this room is located.").add()
                .append(new KeyedCodec<>("PlayerSpawns", new ArrayCodec<>(SpawnPoint.CODEC, SpawnPoint[]::new)),
                        (obj, val) -> obj.playerSpawns = val,
                        obj -> obj.playerSpawns)
                .documentation("List of possible locations for players to teleport into.").add()
                .append(new KeyedCodec<>("NPCSpawnAreas", new ArrayCodec<>(NPCSpawnArea.CODEC, NPCSpawnArea[]::new)),
                        (obj, val) -> obj.npcSpawnAreas = val,
                        obj -> obj.npcSpawnAreas)
                .documentation("Defined area for NPC spawning.").add()
                .build();
    }

    public SpawnPoint[] playerSpawns;
    public NPCSpawnArea[] npcSpawnAreas;
    public String worldName = "default";
    private String roomName = "NewRoom";
    private AssetExtraInfo.Data data;

    public RoomCodec() {
    }

    public RoomCodec(String roomName) {
        this.roomName = roomName;
    }

    public static AssetStore<String, RoomCodec, DefaultAssetMap<String, RoomCodec>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(RoomCodec.class);
        }
        return ASSET_STORE;
    }

    public static DefaultAssetMap<String, RoomCodec> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public static void register() {
        RoomPlugin plugin = RoomPlugin.get();
        plugin.getAssetRegistry()
                .register(HytaleAssetStore.builder(RoomCodec.class, new DefaultAssetMap<>())
                        .setPath("Rooms")
                        .setCodec(RoomCodec.CODEC)
                        .setKeyFunction(RoomCodec::getId)
                        .setReplaceOnRemove(RoomCodec::new)
                        .build());

        plugin.getEventRegistry().register(LoadedAssetsEvent.class, RoomCodec.class, RoomCodec::onLoaded);
        plugin.getEventRegistry().register(RemovedAssetsEvent.class, RoomCodec.class, RoomCodec::onRemoved);
    }

    public static void onLoaded(LoadedAssetsEvent<String, RoomCodec, DefaultAssetMap<String, RoomCodec>> event) {
    }

    public static void onRemoved(RemovedAssetsEvent<String, RoomCodec, DefaultAssetMap<String, RoomCodec>> event) {
    }

    @Override
    public String getId() {
        return roomName;
    }

    public Vector3d getFirstSpawnPosition() {
        if (playerSpawns != null && playerSpawns.length > 0 && playerSpawns[0] != null) {
            return new Vector3d(playerSpawns[0].pos.x, playerSpawns[0].pos.y + 1, playerSpawns[0].pos.z);
        }
        return null;
    }

    public static class SpawnPoint {
        public static final BuilderCodec<SpawnPoint> CODEC = BuilderCodec.builder(SpawnPoint.class, SpawnPoint::new)
                .append(new KeyedCodec<>("Position", CodecUtils.POS_CODEC, true), (obj, val) -> obj.pos = val, obj -> obj.pos).add()
                .append(new KeyedCodec<>("Rotation", CodecUtils.DIR_CODEC, true), (obj, val) -> obj.rot = val, obj -> obj.rot).add()
                .build();
        public Position pos = new Position(0, 0, 0);
        public Direction rot = new Direction(0, 0, 0);
    }

    public static class NPCSpawnArea {
        public static final BuilderCodec<NPCSpawnArea> CODEC = BuilderCodec.builder(NPCSpawnArea.class, NPCSpawnArea::new)
                .append(new KeyedCodec<>("GroupName", Codec.STRING, true), (obj, val) -> obj.groupName = val, obj -> obj.groupName).add()
                .append(new KeyedCodec<>("Min", CodecUtils.POS_CODEC, true), (obj, val) -> obj.minPos = val, obj -> obj.minPos).add()
                .append(new KeyedCodec<>("Max", CodecUtils.POS_CODEC, true), (obj, val) -> obj.maxPos = val, obj -> obj.maxPos).add()
                .build();
        public Position minPos = new Position(0, 0, 0);
        public Position maxPos = new Position(0, 0, 0);
        public String groupName = "Default";
    }
}