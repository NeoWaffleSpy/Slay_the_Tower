package com.Team_Berry.WeaponInteraction.Systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class HotbarDurabilityCooldownTickingSystem extends EntityTickingSystem<EntityStore> {


    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }


    @Override
    public void tick(float dt, int i, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        ItemContainer hotbar = player.getInventory().getHotbar();

        double repairPerSecond = 1.0;
        double amountToAdd = repairPerSecond * dt;

        for (short slot = 0; slot <= 3; slot++) {
            ItemStack stack = hotbar.getItemStack(slot);
            if (ItemStack.isEmpty(stack) || stack.isUnbreakable()) continue;

            double current = stack.getDurability();
            double max = stack.getMaxDurability();

            if (current < max) {
                ItemStack improvedStack = stack.withIncreasedDurability(amountToAdd);
                hotbar.setItemStackForSlot(slot, improvedStack);
            }
        }
    }

}
