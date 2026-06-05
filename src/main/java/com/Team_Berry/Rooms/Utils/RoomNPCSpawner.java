package com.Team_Berry.Rooms.Utils;

import com.Team_Berry.Rooms.Codecs.MobGroupCodec;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.RoomPlugin;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.spawning.ISpawnableWithModel;
import com.hypixel.hytale.server.spawning.SpawningContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RoomNPCSpawner {

    public static @Nullable UUID spawnInRoom(Store<EntityStore> store, RoomCodec room, String roleId) {
        if (room == null || room.npcSpawnAreas.length == 0) return null;

        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = npcPlugin.getBuilderManager().getIndex(roleId);

        if (roleIndex < 0) {
            RoomPlugin.LOGGER.atWarning().log("NPC Role ID not found in BuilderManager: " + roleId);
            return null;
        }

        RoomCodec.NPCSpawnArea area = room.npcSpawnAreas[ThreadLocalRandom.current().nextInt(room.npcSpawnAreas.length)];

        Vector3d spawnPos = new Vector3d(
                safeLerp(area.minPos.x, area.maxPos.x),
                safeLerp(area.minPos.y + 1.0, area.maxPos.y + 1.0),
                safeLerp(area.minPos.z, area.maxPos.z)
        );

        return executeInternalSpawn(store, roleIndex, spawnPos, room);
    }

    static @Nullable UUID executeInternalSpawn(Store<EntityStore> store, int roleIndex, Vector3d position, RoomCodec room) {
        NPCPlugin npcPlugin = NPCPlugin.get();

        Builder<Role> roleBuilder = npcPlugin.tryGetCachedValidRole(roleIndex);

        if (!(roleBuilder instanceof ISpawnableWithModel spawnable) || !roleBuilder.isSpawnable()) {
            RoomPlugin.LOGGER.atWarning().log("NPC at index " + roleIndex + " is not a spawnable role.");
            return null;
        }

        SpawningContext spawningContext = new SpawningContext();
        if (!spawningContext.setSpawnable(spawnable)) return null;

        Model model = spawningContext.getModel();
        Rotation3f rotation = new Rotation3f(0, (float) (ThreadLocalRandom.current().nextDouble() * Math.PI * 2), 0);

        // We use a 1-size array to safely extract the UUID from inside the lambda
        UUID[] caughtUuid = new UUID[1];

        npcPlugin.spawnEntity(
                store, roleIndex, position, rotation, model,
                (npcComponent, holder, entityStore) -> {
                    // Snatch the UUID directly from the blueprint before it hits the Store queue!
                    com.hypixel.hytale.server.core.entity.UUIDComponent uuidComp =
                            (com.hypixel.hytale.server.core.entity.UUIDComponent) holder.getComponent(com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());

                    if (uuidComp != null) {
                        caughtUuid[0] = uuidComp.getUuid();
                    }
                },
                null
        );

        return caughtUuid[0];
    }

    public static @Nullable UUID spawnInRoomById(Store<EntityStore> store, String roomId, String roleId) {
        RoomCodec room = RoomCodec.getAssetMap().getAsset(roomId);
        if (room != null) {
            return spawnInRoom(store, room, roleId);
        } else {
            RoomPlugin.LOGGER.atWarning().log("Cannot spawn NPC: Room ID '" + roomId + "' not found.");
            return null;
        }
    }

    private static double safeLerp(double v1, double v2) {
        double min = Math.min(v1, v2);
        double max = Math.max(v1, v2);
        if (min == max) return min;
        return min + (max - min) * ThreadLocalRandom.current().nextDouble();
    }

    public static List<UUID> spawnMobGroup(Store<EntityStore> store, RoomCodec room, MobGroupCodec group) {
        List<UUID> spawnedUUIDs = new ArrayList<>();

        for (MobGroupCodec.MobEntry entry : group.mobs) {
            for (int i = 0; i < entry.quantity; i++) {
                UUID mobId = spawnInRoom(store, room, entry.npcRoleId);

                if (mobId != null) {
                    spawnedUUIDs.add(mobId);
                }
            }
        }
        return spawnedUUIDs;
    }

    public static List<UUID> spawnMobGroupById(Store<EntityStore> store, String roomId, String mobGroupId) {
        RoomCodec room = RoomCodec.getAssetMap().getAsset(roomId);
        if (room == null) {
            RoomPlugin.LOGGER.atWarning().log("Cannot spawn MobGroup: Room '" + roomId + "' not found.");
            return new ArrayList<>(); // Return empty list instead of null to avoid crashes elsewhere
        }

        MobGroupCodec group = MobGroupCodec.getAssetMap().getAsset(mobGroupId);
        if (group == null) {
            RoomPlugin.LOGGER.atWarning().log("Cannot spawn MobGroup: Group '" + mobGroupId + "' not found.");
            return new ArrayList<>();
        }

        return spawnMobGroup(store, room, group);
    }
}