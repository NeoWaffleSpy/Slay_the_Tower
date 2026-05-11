package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Utils.RoomTeleporter;
import com.Team_Berry.Utils.Scheduler.KeyedScheduler;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TeleportManager {
    private final World world;
    private final KeyedScheduler scheduler;

    public TeleportManager(World world, KeyedScheduler scheduler) {
        this.world = world;
        this.scheduler = scheduler;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public void teleportParticipantsToLobby(Set<PlayerRef> activeParticipants, RoomCodec lobby) {
        if (lobby == null) return;

        log("Teleporting participants to Lobby asset: " + lobby.worldName);

        world.execute(() -> {
            for (PlayerRef participant : activeParticipants) {
                RoomTeleporter.teleportToRoom(participant, lobby, this.world);
            }
        });

        refreshModelsAfterTeleport(activeParticipants, null);
    }

    public void teleportParticipantsToRoom(List<PlayerRef> playersToTeleport, RoomCodec room, Set<PlayerRef> allParticipants, Set<UUID> npcUUIDs) {
        if (playersToTeleport.isEmpty()) return;

        log("Teleporting participants to Room asset: " + room.worldName);
        RoomTeleporter.teleportGroupToRoom(playersToTeleport, room, this.world);

        refreshModelsAfterTeleport(allParticipants, npcUUIDs);
    }

    public void teleportToSpectatorPosition(PlayerRef playerRef, RoomCodec currentRoom, Set<PlayerRef> allParticipants, Set<UUID> npcUUIDs) {
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

        refreshModelsAfterTeleport(allParticipants, npcUUIDs);
    }

    private void refreshModelsAfterTeleport(Set<PlayerRef> players, Set<UUID> npcUUIDs) {
        String taskKey = "model_refresh_" + UUID.randomUUID().toString();

        scheduler.schedule(taskKey, () -> {
            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();

                if (players != null) {
                    for (PlayerRef p : players) {
                        Ref<EntityStore> ref = p.getReference();
                        if (ref != null && ref.isValid()) {
                            forceRedrawEntity(ref, store);
                        }
                    }
                }

                if (npcUUIDs != null) {
                    for (UUID uuid : npcUUIDs) {
                        Ref<EntityStore> ref = getRefByUUID(store, uuid);
                        if (ref != null && ref.isValid()) {
                            forceRedrawEntity(ref, store);
                        }
                    }
                }
            });
        }, 500, TimeUnit.MILLISECONDS);
    }

    private void forceRedrawEntity(Ref<EntityStore> ref, Store<EntityStore> store) {
        ModelComponent modelComp = (ModelComponent) store.getComponent(ref, ModelComponent.getComponentType());
        if (modelComp != null) {
            store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(modelComp.getModel()));
        }

        PlayerSkinComponent skinComp = (PlayerSkinComponent) store.getComponent(ref, PlayerSkinComponent.getComponentType());
        if (skinComp != null) {
            skinComp.setNetworkOutdated();
        }
    }

    private Ref<EntityStore> getRefByUUID(Store<EntityStore> store, UUID targetUUID) {
        AtomicReference<Ref<EntityStore>> foundRef = new AtomicReference<>(null);

        store.forEachChunk(UUIDComponent.getComponentType(), (chunk, commandBuffer) -> {
            if (foundRef.get() != null) return;

            for (int i = 0; i < chunk.size(); i++) {
                UUIDComponent uuidComp = (UUIDComponent) chunk.getComponent(i, UUIDComponent.getComponentType());
                if (uuidComp != null && uuidComp.getUuid().equals(targetUUID)) {
                    foundRef.set(chunk.getReferenceTo(i));
                    break;
                }
            }
        });

        return foundRef.get();
    }
}