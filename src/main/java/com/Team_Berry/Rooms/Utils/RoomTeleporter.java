package com.Team_Berry.Rooms.Utils;

import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.RoomPlugin;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Random;

public class RoomTeleporter {
    private static final Random RANDOM = new Random();

    public static void teleportToRoom(PlayerRef playerRef, RoomCodec room) {
        if (room == null) throw new IllegalArgumentException("Cannot teleport to a null room!");
        if (room.playerSpawns.isEmpty()) return;

        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref.getStore();

        RoomCodec.SpawnPoint spawn = room.playerSpawns.get(RANDOM.nextInt(room.playerSpawns.size()));

        store.getExternalData().getWorld().execute(() -> {
            Vector3d pos = new Vector3d(spawn.pos.x, spawn.pos.y + 1.0, spawn.pos.z);

            Vector3f rot = new Vector3f(
                    (float) Math.toDegrees(spawn.rot.yaw),
                    (float) Math.toDegrees(spawn.rot.pitch),
                    (float) Math.toDegrees(spawn.rot.roll)
            );
            World world = Universe.get().getWorld(room.worldName);
            if (world != null) {
                Teleport teleport = Teleport.createForPlayer(world, pos, rot);

                store.addComponent(ref, Teleport.getComponentType(), teleport);
            } else {
                RoomPlugin.LOGGER.atWarning().log("World doesn't exist!! %s", room.worldName);

            }

        });
    }

    public static void teleportToRoomById(PlayerRef playerRef, String roomId) {
        DefaultAssetMap<String, RoomCodec> assetMap = RoomCodec.getAssetMap();
        RoomCodec room = assetMap.getAsset(roomId);

        if (room == null) {
            RoomPlugin.LOGGER.atWarning().log("Attempted to teleport to non-existent room: %s", roomId);
            playerRef.sendMessage(Message.raw("This room doesn't exist!"));
            return;
        }

        teleportToRoom(playerRef, room);
    }

    public static boolean canTeleportToRoom(RoomCodec room) {
        if (room == null) {
            RoomPlugin.LOGGER.atWarning().log("Validation failed: RoomCodec is null.");
            return false;
        }

        if (room.playerSpawns.isEmpty()) {
            RoomPlugin.LOGGER.atWarning().log("Validation failed: Room %s has no spawn points.", room.worldName);
            return false;
        }

        World world = Universe.get().getWorld(room.worldName);
        if (world == null) {
            RoomPlugin.LOGGER.atWarning().log("Validation failed: World %s does not exist.", room.worldName);
            return false;
        }

        return true;
    }
}
