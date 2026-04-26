package com.Team_Berry.Game.Utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record PlayerInventory(
        ItemContainer armor,
        ItemContainer backpack,
        ItemContainer tool,
        ItemContainer hotbar,
        ItemContainer utility,
        ItemContainer storage
) {
    public static PlayerInventory fromPlayer(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        return new PlayerInventory(
                store.getComponent(ref, InventoryComponent.Armor.getComponentType()).getInventory().clone(),
                store.getComponent(ref, InventoryComponent.Backpack.getComponentType()).getInventory().clone(),
                store.getComponent(ref, InventoryComponent.Tool.getComponentType()).getInventory().clone(),
                store.getComponent(ref, InventoryComponent.Hotbar.getComponentType()).getInventory().clone(),
                store.getComponent(ref, InventoryComponent.Utility.getComponentType()).getInventory().clone(),
                store.getComponent(ref, InventoryComponent.Storage.getComponentType()).getInventory().clone()
        );
    }

    public static void clearPlayerInventory(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        clearInventory(store.getComponent(ref, InventoryComponent.Armor.getComponentType()).getInventory());
        clearInventory(store.getComponent(ref, InventoryComponent.Backpack.getComponentType()).getInventory());
        clearInventory(store.getComponent(ref, InventoryComponent.Tool.getComponentType()).getInventory());
        clearInventory(store.getComponent(ref, InventoryComponent.Hotbar.getComponentType()).getInventory());
        clearInventory(store.getComponent(ref, InventoryComponent.Utility.getComponentType()).getInventory());
        clearInventory(store.getComponent(ref, InventoryComponent.Storage.getComponentType()).getInventory());

        store.getComponent(ref, InventoryComponent.Armor.getComponentType()).markDirty();
        store.getComponent(ref, InventoryComponent.Backpack.getComponentType()).markDirty();
        store.getComponent(ref, InventoryComponent.Tool.getComponentType()).markDirty();
        store.getComponent(ref, InventoryComponent.Hotbar.getComponentType()).markDirty();
        store.getComponent(ref, InventoryComponent.Utility.getComponentType()).markDirty();
        store.getComponent(ref, InventoryComponent.Storage.getComponentType()).markDirty();
    }

    private static void applyInventory(ItemContainer from, ItemContainer to) {
        if (from == null || to == null) return;
        var size = Math.min(from.getCapacity(), to.getCapacity());
        for (short i = 0; i < size; i++) {
            to.setItemStackForSlot(i, from.getItemStack(i));
        }
    }

    private static void clearInventory(ItemContainer container) {
        if (container == null) return;
        for (short i = 0; i < container.getCapacity(); i++) {
            container.setItemStackForSlot(i, ItemStack.EMPTY);
        }
    }

    public void applyToPlayer(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        InventoryComponent.Armor armorComp = store.getComponent(ref, InventoryComponent.Armor.getComponentType());
        applyInventory(this.armor, armorComp.getInventory());
        armorComp.markDirty();

        InventoryComponent.Backpack backpackComp = store.getComponent(ref, InventoryComponent.Backpack.getComponentType());
        applyInventory(this.backpack, backpackComp.getInventory());
        backpackComp.markDirty();

        InventoryComponent.Tool toolComp = store.getComponent(ref, InventoryComponent.Tool.getComponentType());
        applyInventory(this.tool, toolComp.getInventory());
        toolComp.markDirty();

        InventoryComponent.Hotbar hotbarComp = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        applyInventory(this.hotbar, hotbarComp.getInventory());
        hotbarComp.markDirty();

        InventoryComponent.Utility utilityComp = store.getComponent(ref, InventoryComponent.Utility.getComponentType());
        applyInventory(this.utility, utilityComp.getInventory());
        utilityComp.markDirty();

        InventoryComponent.Storage storageComp = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        applyInventory(this.storage, storageComp.getInventory());
        storageComp.markDirty();
    }
}