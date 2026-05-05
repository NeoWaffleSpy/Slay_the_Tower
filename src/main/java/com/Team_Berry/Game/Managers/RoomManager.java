package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Game.Data.Quest;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Utils.RoomTeleporter;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.universe.world.World;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RoomManager {
    private final World world;
    private final RoomCodec lobbyRoom;
    private final RoomCodec postgameRoom;
    private final RoomCodec prisonSpawnRoom;

    private Pair<RoomCodec, Quest> currentRoom;
    private Pair<RoomCodec, Quest> futureRoom;

    public RoomManager(World world) {
        this.world = world;
        this.lobbyRoom = findRoomByCategory("Lobby");
        this.postgameRoom = findRoomByCategory("Postgame");
        this.prisonSpawnRoom = findRoomByCategory("Prisonspawn");
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public Pair<RoomCodec, Quest> getCurrentRoom() { return currentRoom; }
    public Pair<RoomCodec, Quest> getFutureRoom() { return futureRoom; }
    public void setCurrentRoom(Pair<RoomCodec, Quest> room) { this.currentRoom = room; }
    public void setFutureRoom(Pair<RoomCodec, Quest> room) { this.futureRoom = room; }

    public void shiftRooms() {
        this.currentRoom = this.futureRoom;
        this.futureRoom = null;
    }

    public RoomCodec getLobbyRoom() { return lobbyRoom; }
    public RoomCodec getPostgameRoom() { return postgameRoom; }
    public RoomCodec getPrisonSpawnRoom() { return prisonSpawnRoom; }

    public RoomCodec findRoomByCategory(String category) {
        DefaultAssetMap<String, RoomCodec> roomMap = RoomCodec.getAssetMap();
        int tagIndex = AssetRegistry.getOrCreateTagIndex("Category=" + category);
        Set<String> roomKeys = roomMap.getKeysForTag(tagIndex);

        if (roomKeys == null || roomKeys.isEmpty()) {
            log("SEVERE: No room found with tag 'Category=" + category + "'.");
            return null;
        }

        List<RoomCodec> validRooms = new ArrayList<>();
        for (String key : roomKeys) {
            RoomCodec room = roomMap.getAsset(key);

            // If it's a standard room, we must check if we can teleport to it
            if (category.equals("Room")) {
                if (RoomTeleporter.canTeleportToRoom(room)) {
                    validRooms.add(room);
                }
            } else {
                validRooms.add(room);
            }
        }

        if (validRooms.isEmpty()) return null;
        return validRooms.get(ThreadLocalRandom.current().nextInt(validRooms.size()));
    }
}