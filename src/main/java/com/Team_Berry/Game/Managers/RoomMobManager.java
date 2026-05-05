package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Rooms.Codecs.MobGroupCodec;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Utils.RoomNPCSpawner;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class RoomMobManager {
    private final World world;
    private final Set<UUID> pendingCleanup = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public RoomMobManager(World world) {
        this.world = world;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public List<UUID> spawnMobGroup(RoomCodec room, MobGroupCodec group) {
        return RoomNPCSpawner.spawnMobGroup(world.getEntityStore().getStore(), room, group);
    }

    public void cleanRoom(Set<UUID> mobTracker) {
        if (mobTracker.isEmpty()) return;

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Set<UUID> foundMobs = new HashSet<>();

            store.forEachChunk(UUIDComponent.getComponentType(), (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    UUIDComponent uuidComp = (UUIDComponent) chunk.getComponent(i, UUIDComponent.getComponentType());

                    if (uuidComp != null && mobTracker.contains(uuidComp.getUuid())) {
                        commandBuffer.removeEntity(chunk.getReferenceTo(i), RemoveReason.REMOVE);
                        foundMobs.add(uuidComp.getUuid());
                    }
                }
            });

            for (UUID id : mobTracker) {
                if (!foundMobs.contains(id)) {
                    pendingCleanup.add(id);
                }
            }

            log("Room cleaned. " + foundMobs.size() + " active mobs evaporated. " +
                    (mobTracker.size() - foundMobs.size()) + " asleep mobs sent to pending cleanup.");

            mobTracker.clear();
        });
    }

    public void processPendingCleanup() {
        if (pendingCleanup.isEmpty()) return;

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Set<UUID> foundMobs = new HashSet<>();

            store.forEachChunk(UUIDComponent.getComponentType(), (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    UUIDComponent uuidComp = (UUIDComponent) chunk.getComponent(i, UUIDComponent.getComponentType());

                    if (uuidComp != null && pendingCleanup.contains(uuidComp.getUuid())) {
                        commandBuffer.removeEntity(chunk.getReferenceTo(i), RemoveReason.REMOVE);
                        foundMobs.add(uuidComp.getUuid());
                    }
                }
            });

            for (UUID id : foundMobs) {
                pendingCleanup.remove(id);
            }

            if (!foundMobs.isEmpty()) {
                log("Late Cleanup: Successfully  removed " + foundMobs.size() + " sleeping mobs that just loaded in.");
            }
        });
    }

    public Ref<EntityStore> getRefByUUID(Store<EntityStore> store, UUID targetUUID) {
        AtomicReference<Ref<EntityStore>> foundRef = new AtomicReference<>(null);

        store.forEachChunk(UUIDComponent.getComponentType(), (chunk, commandBuffer) -> {
            if (foundRef.get() != null) return;

            for (int i = 0; i < chunk.size(); i++) {
                UUIDComponent uuidComp =
                        (UUIDComponent) chunk.getComponent(i, UUIDComponent.getComponentType());

                if (uuidComp != null && uuidComp.getUuid().equals(targetUUID)) {
                    foundRef.set(chunk.getReferenceTo(i));
                    break;
                }
            }
        });

        return foundRef.get();
    }

    public void applyEffectToMobs(Set<UUID> mobTracker, String effectId) {
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);

            if (effect == null) {
                log("Warning: Cannot apply effect! Asset not found: " + effectId);
                return;
            }

            int appliedCount = 0;
            for (UUID mobId : mobTracker) {
                Ref<EntityStore> ref = getRefByUUID(store, mobId);
                if (ref != null && ref.isValid()) {
                    EffectControllerComponent effectController = store.getComponent(ref, EffectControllerComponent.getComponentType());

                    if (effectController != null) {
                        effectController.addEffect(ref, effect, store);
                        appliedCount++;
                    }
                }
            }
            log("Applied " + effectId + " to " + appliedCount + " mobs.");
        });
    }

    public void validateRemainingMobs(Set<UUID> currentRoomMobs, com.Team_Berry.Game.Data.Quest currentQuest) {
        if (currentRoomMobs.isEmpty()) return;

        Store<EntityStore> store = world.getEntityStore().getStore();
        List<UUID> vanishedIds = new ArrayList<>();

        for (UUID mobId : currentRoomMobs) {
            Ref<EntityStore> ref = getRefByUUID(store, mobId);
            if (ref == null || !ref.isValid()) {
                vanishedIds.add(mobId);
            }
        }

        if (!vanishedIds.isEmpty()) {
            for (UUID id : vanishedIds) {
                currentRoomMobs.remove(id);
                currentQuest.incrementDeadMobs();
                log("Anti-Softlock: Detected and cleared vanished mob UUID: " + id);
            }
        }
    }
}
