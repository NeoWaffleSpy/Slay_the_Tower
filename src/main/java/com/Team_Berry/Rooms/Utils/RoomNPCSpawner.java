package com.Team_Berry.Rooms.Utils;

import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.RoomPlugin;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.spawning.ISpawnableWithModel;
import com.hypixel.hytale.server.spawning.SpawningContext;

import java.util.concurrent.ThreadLocalRandom;

public class RoomNPCSpawner {


    public static void spawnInRoom(Store<EntityStore> store, RoomCodec room, String roleId) {
        if (room == null || room.npcSpawnAreas.isEmpty()) return;

        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = npcPlugin.getBuilderManager().getIndex(roleId);

        if (roleIndex < 0) {
            RoomPlugin.LOGGER.atWarning().log("NPC Role ID not found in BuilderManager: " + roleId);
            return;
        }

        RoomCodec.NPCSpawnArea area = room.npcSpawnAreas.get(ThreadLocalRandom.current().nextInt(room.npcSpawnAreas.size()));

        Vector3d spawnPos = new Vector3d(
                safeLerp(area.minPos.x, area.maxPos.x),
                safeLerp(area.minPos.y + 1.0, area.maxPos.y + 1.0),
                safeLerp(area.minPos.z, area.maxPos.z)
        );

        executeInternalSpawn(store, roleIndex, spawnPos);
    }

    private static void executeInternalSpawn(Store<EntityStore> store, int roleIndex, Vector3d position) {
        NPCPlugin npcPlugin = NPCPlugin.get();

        Builder<Role> roleBuilder = npcPlugin.tryGetCachedValidRole(roleIndex);

        if (roleBuilder == null || !(roleBuilder instanceof ISpawnableWithModel spawnable) || !roleBuilder.isSpawnable()) {
            RoomPlugin.LOGGER.atWarning().log("NPC at index " + roleIndex + " is not a spawnable role.");
            return;
        }

        SpawningContext spawningContext = new SpawningContext();
        if (!spawningContext.setSpawnable(spawnable)) return;

        Model model = spawningContext.getModel();

        Vector3f rotation = new Vector3f(0, (float) (ThreadLocalRandom.current().nextDouble() * Math.PI * 2), 0);

        npcPlugin.spawnEntity(store, roleIndex, position, rotation, model, null);
    }


    public static void spawnInRoomById(Store<EntityStore> store, String roomId, String roleId) {
        RoomCodec room = RoomCodec.getAssetMap().getAsset(roomId);
        if (room != null) {
            spawnInRoom(store, room, roleId);
        } else {
            RoomPlugin.LOGGER.atWarning().log("Cannot spawn NPC: Room ID '" + roomId + "' not found.");
        }
    }

    private static double safeLerp(double v1, double v2) {
        double min = Math.min(v1, v2);
        double max = Math.max(v1, v2);
        if (min == max) return min;
        return min + (max - min) * ThreadLocalRandom.current().nextDouble();
    }
}