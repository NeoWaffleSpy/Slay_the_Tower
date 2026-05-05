package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Utils.RoomTeleporter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Set;

public class TeleportManager {
    private final World world;

    public TeleportManager(World world) {
        this.world = world;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public void teleportParticipantsToLobby(Set<PlayerRef> activeParticipants, RoomCodec lobby) {
        if (lobby != null) {
            log("Teleporting participants to Lobby asset: " + lobby.worldName);
            world.execute(() -> {
                for (PlayerRef participant : activeParticipants) {
                    RoomTeleporter.teleportToRoom(participant, lobby, this.world);
                }
            });
        }
    }

    public void teleportParticipantsToRoom(List<PlayerRef> playersToTeleport, RoomCodec room) {
        log("Teleporting participants to Room asset: " + room.worldName);

        if (!playersToTeleport.isEmpty()) {
            RoomTeleporter.teleportGroupToRoom(playersToTeleport, room, this.world);
        }
    }

    public void teleportToSpectatorPosition(PlayerRef playerRef, RoomCodec currentRoom) {
        if (currentRoom == null || currentRoom.spectatePosition == null) {
            log("Warning: No spectate position defined for the current room.");
            return;
        }

        Position pos = currentRoom.spectatePosition;
        Transform spectateTransform = new Transform(new Vector3d(pos.x, pos.y, pos.z));

        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                Teleport teleportComponent = Teleport.createForPlayer(world, spectateTransform);
                store.addComponent(ref, Teleport.getComponentType(), teleportComponent);
                log("Teleported spectator " + playerRef.getUsername() + " to the spectate position.");
            }
        });
    }
}
