package com.Team_Berry.Game.Systems;

import com.Team_Berry.Game.GameManager;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.inventory.DropItemStack;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class DropItemPacketHandler implements PlayerPacketFilter {

    private static final String[] weaponStringList = new String[]{
            "Daggers_Kweebec",
            "Bow_Kweebec"
    };

    @Override
    public boolean test(@NonNull PlayerRef playerRef, @NonNull Packet packet) {
        if (!(packet instanceof DropItemStack dropPacket)) {
            return false;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return false;

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        GameManager manager = GamePlugin.get().getGameManager(world);
        if (manager == null || !manager.getActiveParticipants().contains(playerRef)) {
            return false;
        }

        int sectionId = dropPacket.inventorySectionId;
        int slotId = dropPacket.slotId;
        int dropQuantity = dropPacket.quantity;

        world.execute(() -> {

            InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
            InventoryComponent.Backpack backpack = store.getComponent(ref, InventoryComponent.Backpack.getComponentType());
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());

            boolean isHotbar = (sectionId == -1);
            ItemContainer container = isHotbar && hotbar != null ? hotbar.getInventory() : (backpack != null ? backpack.getInventory() : null);

            if (container == null) return;

            ItemStack targetItem = container.getItemStack((short) slotId);

            if (targetItem != null && !targetItem.isEmpty()) {
                String itemId = targetItem.getItem().getId();

                if (itemId != null && (itemId.startsWith("Hotbar") || isWeapon(itemId))) {

                    playerRef.sendMessage(Message.raw("You cannot allow yourself to do that!"));
                    if (isHotbar) hotbar.markDirty();
                    else backpack.markDirty();

                } else {

                    container.setItemStackForSlot((short) slotId, ItemStack.EMPTY);
                    if (isHotbar) hotbar.markDirty();
                    else backpack.markDirty();

                    if (transform != null) {
                        Holder<EntityStore>[] itemDrops = ItemComponent.generateItemDrops(
                                store,
                                List.of(targetItem),
                                transform.getPosition(),
                                new Vector3f()
                        );
                        world.getEntityStore().getStore().addEntities(itemDrops, AddReason.SPAWN);
                    }
                }
            }
        });


        return true;
    }

    private boolean isWeapon(String itemId) {
        for (String w : weaponStringList) {
            if (itemId.equals(w)) return true;
        }
        return false;
    }
}