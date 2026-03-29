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

        validateHotbarItems(chunk, i, store, hotbarComponent, playerRef);

        boolean durabilityChanged = refillDurability(dt, hotbarComponent);

        syncHotbarSlot(hotbarComponent, playerRef);

        if (durabilityChanged || hotbarComponent.consumeIsDirty()) {
            hotbarComponent.markDirty();
        }
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
        Integer lastSlot = lastSyncedSlot.get(uuid);

        if (lastSlot == null || lastSlot != currentSlot) {
            playerRef.getPacketHandler().writeNoCache(new SetActiveSlot(HOTBAR_SECTION_ID, currentSlot));
            lastSyncedSlot.put(uuid, currentSlot);
            System.out.println("Slot Synced: " + currentSlot);
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

    private void validateHotbarItems(@NonNull ArchetypeChunk<EntityStore> chunk, int index, @NonNull Store<EntityStore> store, InventoryComponent.Hotbar hotbarComp, PlayerRef playerRef) {
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

                    System.out.println("[HotbarSync] Dropped illegal item: " + itemId + " for " + playerRef.getUsername());
                }

                hotbar.setItemStackForSlot(slot, ItemStack.EMPTY);
                hotbarComp.markDirty();
            }
        }
    }
}