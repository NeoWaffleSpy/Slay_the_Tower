package com.Team_Berry.Rooms.Utils;

import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Rooms.Codecs.MobGroupCodec;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.RoomPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.spawning.ISpawnableWithModel;
import com.hypixel.hytale.server.spawning.SpawningContext;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RoomNPCSpawner {


    public static @Nullable Ref<EntityStore> spawnInRoom(Store<EntityStore> store, RoomCodec room, String roleId) {
        if (room == null || room.npcSpawnAreas.isEmpty()) return null;

        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = npcPlugin.getBuilderManager().getIndex(roleId);

        if (roleIndex < 0) {
            RoomPlugin.LOGGER.atWarning().log("NPC Role ID not found in BuilderManager: " + roleId);
            return null;
        }

        RoomCodec.NPCSpawnArea area = room.npcSpawnAreas.get(ThreadLocalRandom.current().nextInt(room.npcSpawnAreas.size()));

        Vector3d spawnPos = new Vector3d(
                safeLerp(area.minPos.x, area.maxPos.x),
                safeLerp(area.minPos.y + 1.0, area.maxPos.y + 1.0),
                safeLerp(area.minPos.z, area.maxPos.z)
        );

        return executeInternalSpawn(store, roleIndex, spawnPos);
    }


    static @Nullable Ref<EntityStore> executeInternalSpawn(Store<EntityStore> store, int roleIndex, Vector3d position) {
        NPCPlugin npcPlugin = NPCPlugin.get();

        Builder<Role> roleBuilder = npcPlugin.tryGetCachedValidRole(roleIndex);

        if (!(roleBuilder instanceof ISpawnableWithModel spawnable) || !roleBuilder.isSpawnable()) {
            RoomPlugin.LOGGER.atWarning().log("NPC at index " + roleIndex + " is not a spawnable role.");
            return null;
        }

        SpawningContext spawningContext = new SpawningContext();
        if (!spawningContext.setSpawnable(spawnable)) return null;

        Model model = spawningContext.getModel();

        Vector3f rotation = new Vector3f(0, (float) (ThreadLocalRandom.current().nextDouble() * Math.PI * 2), 0);

        Pair<Ref<EntityStore>, NPCEntity> npcEntityPair = npcPlugin.spawnEntity(store, roleIndex, position, rotation, model, null);
        Ref<EntityStore> ref = npcEntityPair.left();
        store.getExternalData().getWorld().execute(() -> {
            store.addComponent(ref, GamePlugin.getQuestNPCComponentType());
        });
        return ref;
    }


    public static @Nullable Ref<EntityStore> spawnInRoomById(Store<EntityStore> store, String roomId, String roleId) {
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

    public static List<Ref<EntityStore>> spawnMobGroup(Store<EntityStore> store, RoomCodec room, MobGroupCodec group) {
        List<Ref<EntityStore>> spawnedRefs = new ArrayList<>();

        for (MobGroupCodec.MobEntry entry : group.mobs) {
            for (int i = 0; i < entry.quantity; i++) {
                Ref<EntityStore> ref = spawnInRoom(store, room, entry.npcRoleId);

                // Only add if the spawn was successful (not null)
                if (ref != null) {
                    spawnedRefs.add(ref);
                }
            }
        }
        return spawnedRefs;
    }

    public static List<Ref<EntityStore>> spawnMobGroupById(Store<EntityStore> store, String roomId, String mobGroupId) {
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