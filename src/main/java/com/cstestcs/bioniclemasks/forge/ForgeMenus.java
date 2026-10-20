package com.cstestcs.bioniclemasks.forge;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared bits of the two forge menus. */
final class ForgeMenus {
    /** Both forge screens are 176x184; the player inventory starts here. */
    static final int INVENTORY_Y = 102;

    private ForgeMenus() {
    }

    static void addPlayerSlots(java.util.function.Consumer<Slot> addSlot, Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot.accept(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot.accept(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    /** Uses one item from a slot, leaving its container item (bucket, bottle) behind or giving it to the player. */
    static void consumeOne(Container container, int slot, Player player) {
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty()) {
            return;
        }
        var remainderTemplate = stack.getItem().getCraftingRemainder();
        ItemStack remainder = remainderTemplate == null ? ItemStack.EMPTY : remainderTemplate.create();
        stack.shrink(1);
        if (stack.isEmpty()) {
            container.setItem(slot, remainder);
        } else if (!remainder.isEmpty() && !player.getInventory().add(remainder)) {
            player.drop(remainder, false);
        }
        container.setChanged();
    }

    /** A slot that only accepts what the predicate allows. */
    static Slot filtered(Container container, int index, int x, int y, java.util.function.Predicate<ItemStack> allowed) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return allowed.test(stack);
            }
        };
    }

    /** A take-only slot. */
    static Slot output(Container container, int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }
}
