package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Rooms.Codecs.MobGroupCodec;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Utils.RoomTeleporter;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RoomManager {
    private final World world;
    private final RoomCodec lobbyRoom;
    private final RoomCodec postgameRoom;
    private final RoomCodec prisonSpawnRoom;

    public RoomManager(World world) {
        this.world = world;
        this.lobbyRoom = findLobbyRoom();
        this.postgameRoom = findPostgameRoom();
        this.prisonSpawnRoom = findPrisonSpawnRoom();
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public RoomCodec getLobbyRoom() {
        return lobbyRoom;
    }

    public RoomCodec getPostgameRoom() {
        return postgameRoom;
    }

    public RoomCodec getPrisonSpawnRoom() {
        return prisonSpawnRoom;
    }

    public List<RoomCodec> findValidRooms() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Room");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) return Collections.emptyList();

        List<RoomCodec> validRooms = new ArrayList<>();
        for (String key : roomKeys) {
            RoomCodec room = roomMap.getAsset(key);
            if (RoomTeleporter.canTeleportToRoom(room)) {
                validRooms.add(room);
            }
        }
        return validRooms;
    }

    public List<MobGroupCodec> findValidMobGroups(int difficulty) {
        DefaultAssetMap<String, MobGroupCodec> map = MobGroupCodec.getAssetMap();
        String tagSearch = "Difficulty=" + difficulty;
        log("Looking for Mob Groups with tag: " + tagSearch);
        int tagIndex = AssetRegistry.getOrCreateTagIndex(tagSearch);
        Set<String> groupKeys = map.getKeysForTag(tagIndex);

        if (groupKeys == null || groupKeys.isEmpty()) return null;

        List<MobGroupCodec> validGroups = new ArrayList<>();
        for (String key : groupKeys) {
            MobGroupCodec group = map.getAsset(key);
            if (group != null) {
                validGroups.add(group);
                log("Pulled group: " + key);
            }
        }
        return validGroups.isEmpty() ? null : validGroups;
    }

    private RoomCodec findLobbyRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Lobby");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) return null;

        List<String> keyList = new ArrayList<>(roomKeys);
        String randomKey = keyList.get(ThreadLocalRandom.current().nextInt(keyList.size()));

        return roomMap.getAsset(randomKey);
    }

    private RoomCodec findPostgameRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Postgame");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) {
            log("Warning: No Postgame room found with tag 'Category=Postgame'.");
            return null;
        }

        List<String> keyList = new ArrayList<>(roomKeys);
        String randomKey = keyList.get(ThreadLocalRandom.current().nextInt(keyList.size()));

        return roomMap.getAsset(randomKey);
    }

    private RoomCodec findPrisonSpawnRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Prisonspawn");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) {
            log("Warning: No Prisonspawn room found with tag 'Category=Prisonspawn'.");
            return null;
        }

        List<String> keyList = new ArrayList<>(roomKeys);
        String randomKey = keyList.get(ThreadLocalRandom.current().nextInt(keyList.size()));

        return roomMap.getAsset(randomKey);
    }

    public RoomCodec findBossRoom() {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=Boss");
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) {
            log("SEVERE: No Boss room found with tag 'Category=Boss'.");
            return null;
        }

        List<String> keyList = new ArrayList<>(roomKeys);
        return roomMap.getAsset(keyList.get(ThreadLocalRandom.current().nextInt(keyList.size())));
    }

    public List<MobGroupCodec> findBossMobGroups() {
        DefaultAssetMap<String, MobGroupCodec> map = MobGroupCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Difficulty=Boss");
        Set<String> groupKeys = map.getKeysForTag(tagIndex);

        if (groupKeys == null || groupKeys.isEmpty()) {
            log("SEVERE: No Boss mob groups found with tag 'Difficulty=Boss'.");
            return null;
        }

        List<MobGroupCodec> validGroups = new ArrayList<>();
        for (String key : groupKeys) {
            validGroups.add(map.getAsset(key));
        }
        return validGroups;
    }
}
