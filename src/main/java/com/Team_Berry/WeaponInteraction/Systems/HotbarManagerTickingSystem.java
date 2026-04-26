package com.Team_Berry.WeaponInteraction.Systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HotbarManagerTickingSystem extends EntityTickingSystem<EntityStore> {

    private static final byte HOTBAR_SECTION_ID = -1;
    private static final String[] weaponStringList = new String[]{
            "Daggers_Kweebec",
            "Bow_Kweebec"
    };
    private final ConcurrentHashMap<UUID, Integer> lastSyncedSlot = new ConcurrentHashMap<>();

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(
                PlayerRef.getComponentType(),
                InventoryComponent.Hotbar.getComponentType()
        );
    }

    @Override
    public void tick(float dt, int i, @NonNull ArchetypeChunk<EntityStore> chunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        InventoryComponent.Hotbar hotbarComponent = chunk.getComponent(i, InventoryComponent.Hotbar.getComponentType());
        PlayerRef playerRef = chunk.getComponent(i, PlayerRef.getComponentType());

        if (!isPlayerInActiveGame(store, playerRef)) {
            return;
        }

        validateHotbarItems(chunk, i, store, hotbarComponent, playerRef);

        boolean durabilityChanged = refillDurability(dt, hotbarComponent);

        syncHotbarSlot(hotbarComponent, playerRef);

        if (durabilityChanged || hotbarComponent.consumeIsDirty()) {
            hotbarComponent.markDirty();
        }
    }

    private boolean isPlayerInActiveGame(Store<EntityStore> store, PlayerRef playerRef) {
        World world = store.getExternalData().getWorld();
        if (world == null) return false;

        com.Team_Berry.Game.GameManager manager = com.Team_Berry.Game.GamePlugin.get().getGameManagers().get(world);
        return manager != null && manager.getActiveParticipants().contains(playerRef);
    }


    private boolean refillDurability(float dt, InventoryComponent.Hotbar hotbarComp) {
        ItemContainer inventory = hotbarComp.getInventory();
        double repairPerSecond = 1.0 * dt;
        boolean durabilityChanged = false;

        for (short slot = 0; slot <= 3; slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (ItemStack.isEmpty(stack) || stack.isUnbreakable()) continue;

            if (stack.getDurability() < stack.getMaxDurability()) {
                inventory.setItemStackForSlot(slot, stack.withIncreasedDurability(repairPerSecond));
                durabilityChanged = true;
            }
        }
        return durabilityChanged;
    }

    private void syncHotbarSlot(InventoryComponent.Hotbar hotbarComp, PlayerRef playerRef) {
        int currentSlot = hotbarComp.getActiveSlot();
        UUID uuid = playerRef.getUuid();

        if ((currentSlot >= 0 && currentSlot <= 3) || currentSlot == 8) {
            Integer lastSlot = lastSyncedSlot.get(uuid);

            if (lastSlot == null || lastSlot != currentSlot) {
                playerRef.getPacketHandler().writeNoCache(new SetActiveSlot(HOTBAR_SECTION_ID, currentSlot));
                lastSyncedSlot.put(uuid, currentSlot);
            }
        } else {
            lastSyncedSlot.remove(uuid);
        }
    }

    private boolean tryMoveToOtherInventories(ArchetypeChunk<EntityStore> chunk, int index, ItemStack stack) {
        InventoryComponent.Storage storage = chunk.getComponent(index, InventoryComponent.Storage.getComponentType());
        InventoryComponent.Backpack backpack = chunk.getComponent(index, InventoryComponent.Backpack.getComponentType());

        if (storage != null && storage.getInventory().addItemStack(stack).succeeded()) {
            storage.markDirty();
            return true;
        }

        if (backpack != null && backpack.getInventory().addItemStack(stack).succeeded()) {
            backpack.markDirty();
            return true;
        }

        return false;
    }

    private boolean isWeapon(String itemId) {
        if (itemId == null) return false;
        for (String w : weaponStringList) {
            if (itemId.equals(w)) return true;
        }
        return false;
    }

    // Actively hunts down the weapon and forces it back to slot 8 (the 9th slot)
    private void ensureWeaponInSlot8(ArchetypeChunk<EntityStore> chunk, int index, InventoryComponent.Hotbar hotbarComp, PlayerRef playerRef) {
        ItemContainer hotbar = hotbarComp.getInventory();
        ItemStack stackIn8 = hotbar.getItemStack((short) 8);

        // 1. Check if slot 8 already has the weapon (Happy Path)
        if (stackIn8 != null && !stackIn8.isEmpty() && isWeapon(stackIn8.getItem().getId())) {
            return;
        }

        boolean weaponWasMoved = false;

        // 2. Search the rest of the Hotbar for the weapon
        for (short slot = 0; slot < hotbar.getCapacity(); slot++) {
            if (slot == 8) continue;
            ItemStack stack = hotbar.getItemStack(slot);
            if (stack != null && !stack.isEmpty() && isWeapon(stack.getItem().getId())) {
                hotbar.setItemStackForSlot((short) 8, stack);
                hotbar.setItemStackForSlot(slot, stackIn8 != null ? stackIn8 : ItemStack.EMPTY);
                hotbarComp.markDirty();
                weaponWasMoved = true;
                break;
            }
        }

        // 3. Search the Backpack for the weapon
        if (!weaponWasMoved) {
            InventoryComponent.Backpack backpack = chunk.getComponent(index, InventoryComponent.Backpack.getComponentType());
            if (backpack != null) {
                ItemContainer bpInv = backpack.getInventory();
                for (short slot = 0; slot < bpInv.getCapacity(); slot++) {
                    ItemStack stack = bpInv.getItemStack(slot);
                    if (stack != null && !stack.isEmpty() && isWeapon(stack.getItem().getId())) {
                        hotbar.setItemStackForSlot((short) 8, stack);
                        bpInv.setItemStackForSlot(slot, stackIn8 != null ? stackIn8 : ItemStack.EMPTY);
                        hotbarComp.markDirty();
                        backpack.markDirty();
                        weaponWasMoved = true;
                        break;
                    }
                }
            }
        }

        // 4. Search the Storage for the weapon
        if (!weaponWasMoved) {
            InventoryComponent.Storage storage = chunk.getComponent(index, InventoryComponent.Storage.getComponentType());
            if (storage != null) {
                ItemContainer stInv = storage.getInventory();
                for (short slot = 0; slot < stInv.getCapacity(); slot++) {
                    ItemStack stack = stInv.getItemStack(slot);
                    if (stack != null && !stack.isEmpty() && isWeapon(stack.getItem().getId())) {
                        hotbar.setItemStackForSlot((short) 8, stack);
                        stInv.setItemStackForSlot(slot, stackIn8 != null ? stackIn8 : ItemStack.EMPTY);
                        hotbarComp.markDirty();
                        storage.markDirty();
                        weaponWasMoved = true;
                        break;
                    }
                }
            }
        }

        if (weaponWasMoved && playerRef != null) {
            playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw("Your past training makes you keep the weapon in the last slot for some reason..."));
        }
    }

    private void validateHotbarItems(@NonNull ArchetypeChunk<EntityStore> chunk, int index, @NonNull Store<EntityStore> store, InventoryComponent.Hotbar hotbarComp, PlayerRef playerRef) {

        ensureWeaponInSlot8(chunk, index, hotbarComp, playerRef);

        ItemContainer hotbar = hotbarComp.getInventory();
        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        for (short slot = 0; slot < hotbar.getCapacity(); slot++) {
            ItemStack stack = hotbar.getItemStack(slot);
            if (stack == null || stack.isEmpty()) continue;

            String itemId = stack.getItem().getId();

            if (itemId.startsWith("Hotbar") && slot > 3) {

                boolean moved = tryMoveToOtherInventories(chunk, index, stack);

                if (!moved) {
                    World world = ref.getStore().getExternalData().getWorld();
                    TransformComponent transformComponent = chunk.getComponent(index, TransformComponent.getComponentType());
                    var position = transformComponent.getPosition();

                    Holder<EntityStore>[] itemEntityHolders = ItemComponent.generateItemDrops(
                            store,
                            java.util.List.of(stack),
                            position,
                            new Vector3f()
                    );
                    world.execute(() -> {
                        world.getEntityStore().getStore().addEntities(itemEntityHolders, AddReason.SPAWN);
                    });
                }

                hotbar.setItemStackForSlot(slot, ItemStack.EMPTY);
                hotbarComp.markDirty();
            }
        }
    }
}