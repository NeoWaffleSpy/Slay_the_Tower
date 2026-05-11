package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.Team_Berry.Rooms.Codecs.MobGroupCodec;
import com.Team_Berry.Rooms.Codecs.RoomCodec;
import com.Team_Berry.Rooms.Utils.RoomNPCSpawner;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class RoomMobManager {
    private static final String MOB_SEARCH_EFFECT = "Mob_Search_Effect";
    private final World world;
    private final Set<UUID> currentRoomMobs = new HashSet<>();
    private final Set<UUID> futureRoomMobs = new HashSet<>();
    private final Set<UUID> pendingCleanup = ConcurrentHashMap.newKeySet();
    private boolean searchEffectApplied = false;

    public RoomMobManager(World world) {
        this.world = world;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public boolean hasCurrentMobs() {
        return !currentRoomMobs.isEmpty();
    }

    public boolean hasFutureMobs() {
        return !futureRoomMobs.isEmpty();
    }

    public void registerCurrentMobs(Collection<UUID> mobs) {
        currentRoomMobs.clear();
        currentRoomMobs.addAll(mobs);
    }

    public void registerFutureMobs(Collection<UUID> mobs) {
        futureRoomMobs.clear();
        futureRoomMobs.addAll(mobs);
    }

    public void shiftMobsToCurrent() {
        currentRoomMobs.clear();
        currentRoomMobs.addAll(futureRoomMobs);
        futureRoomMobs.clear();
    }

    public void resetSearchEffect() {
        this.searchEffectApplied = false;
    }


    public List<UUID> spawnMobGroup(RoomCodec room, MobGroupCodec group) {
        return RoomNPCSpawner.spawnMobGroup(world.getEntityStore().getStore(), room, group);
    }

    public boolean handleMobDeath(UUID deadMobId, com.Team_Berry.Game.Data.Quest currentQuest, com.Team_Berry.Game.Data.Quest futureQuest) {
        if (currentRoomMobs.remove(deadMobId)) {
            if (currentQuest != null) {
                currentQuest.incrementDeadMobs();
                log(String.format("Current Room Mob Death. Progress: %d/%d", currentQuest.getDeadMobs(), currentQuest.getSpawnedMobs()));
                validateRemainingMobs(currentQuest);
            }
            return true;
        } else if (futureRoomMobs.remove(deadMobId)) {
            if (futureQuest != null) {
                futureQuest.incrementDeadMobs();
                log("A Future Room Mob died prematurely! Lowered future quest requirements.");
            }
            return false;
        }
        return false;
    }

    public void checkAndApplySearchEffect(com.Team_Berry.Game.Data.Quest currentQuest, Set<PlayerRef> activeParticipants) {
        if (!searchEffectApplied && currentQuest != null && currentQuest.getMobsLeft() <= (currentQuest.getSpawnedMobs() / 3)) {
            searchEffectApplied = true;
            for (PlayerRef p : activeParticipants) {
                p.sendMessage(Message.raw("The remaining monsters have been revealed!"));
            }
            applyEffectToCurrentMobs(MOB_SEARCH_EFFECT); // Uses its own constant now
        }
    }

    public void cleanCurrentRoom() {
        cleanRoomInternal(currentRoomMobs);
    }

    public void cleanFutureRoom() {
        cleanRoomInternal(futureRoomMobs);
    }

    private void cleanRoomInternal(Set<UUID> mobTracker) {
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

    public void applyEffectToCurrentMobs(String effectId) {
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);

            if (effect == null) {
                log("Warning: Cannot apply effect! Asset not found: " + effectId);
                return;
            }

            int appliedCount = 0;
            for (UUID mobId : currentRoomMobs) {
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

    public void validateRemainingMobs(com.Team_Berry.Game.Data.Quest currentQuest) {
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

    public void makeBossHitboxHard() {
        if (currentRoomMobs.isEmpty()) return;
        UUID bossUuid = currentRoomMobs.iterator().next();

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> bossRef = getRefByUUID(store, bossUuid);

            if (bossRef != null && bossRef.isValid()) {
                HitboxCollisionConfig hardConfig = HitboxCollisionConfig.getAssetMap().getAsset("HardCollision");
                if (hardConfig != null) {
                    if (!store.getArchetype(bossRef).contains(HitboxCollision.getComponentType())) {
                        store.addComponent(bossRef, HitboxCollision.getComponentType(), new HitboxCollision(hardConfig));
                    } else {
                        store.putComponent(bossRef, HitboxCollision.getComponentType(), new HitboxCollision(hardConfig));
                    }
                    log("Successfully made the Boss HARD. :p");
                } else {
                    log("Warning: Could not find hard collision config in the Asset Store.");
                }
            } else {
                log("Warning: Could not find Boss entity reference to apply hard hitbox.");
            }
        });
    }

    public List<MobGroupCodec> findMobGroups(int difficulty, boolean isBoss) {
        DefaultAssetMap<String, MobGroupCodec> map = MobGroupCodec.getAssetMap();
        String tagSearch = isBoss ? "Difficulty=Boss" : "Difficulty=" + difficulty;
        log("Looking for Mob Groups with tag: " + tagSearch);

        int tagIndex = AssetRegistry.getOrCreateTagIndex(tagSearch);
        Set<String> groupKeys = map.getKeysForTag(tagIndex);

        if (groupKeys == null || groupKeys.isEmpty()) return null;

        List<MobGroupCodec> validGroups = new ArrayList<>();
        for (String key : groupKeys) {
            MobGroupCodec group = map.getAsset(key);
            if (group != null) {
                validGroups.add(group);
            }
        }
        return validGroups.isEmpty() ? null : validGroups;
    }

    public Set<UUID> getCurrentMobUUIDs() {
        return new HashSet<>(currentRoomMobs);
    }
}