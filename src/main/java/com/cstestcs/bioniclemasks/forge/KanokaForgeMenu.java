package com.cstestcs.bioniclemasks.forge;

import com.cstestcs.bioniclemasks.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Kanoka Disk Forge: material, power catalyst and two optional additives in; one disk out.
 * The result updates as you change the inputs, like a crafting table.
 */
public class KanokaForgeMenu extends AbstractContainerMenu {
    public static final int MATERIAL = 0;
    public static final int CATALYST = 1;
    public static final int ADDITIVE_A = 2;
    public static final int ADDITIVE_B = 3;
    public static final int INPUT_COUNT = 4;
    public static final int RESULT = 4;
    private static final int PLAYER_START = 5;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final KanokaForgeBlockEntity forge;
    private final Container inputs;
    private final SimpleContainer result = new SimpleContainer(1);

    public KanokaForgeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (KanokaForgeBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public KanokaForgeMenu(int id, Inventory inventory, KanokaForgeBlockEntity forge) {
        super(ModMenus.KANOKA_FORGE.get(), id);
        this.forge = forge;
        this.inputs = forge.inventory;
        addSlot(ForgeMenus.filtered(inputs, MATERIAL, 26, 28, s -> DiskPressing.material(s) != null));
        addSlot(ForgeMenus.filtered(inputs, CATALYST, 26, 52, s -> DiskPressing.POWER_CATALYSTS.containsKey(s.getItem())));
        addSlot(ForgeMenus.filtered(inputs, ADDITIVE_A, 56, 28, DiskPressing::isAdditive));
        addSlot(ForgeMenus.filtered(inputs, ADDITIVE_B, 56, 52, DiskPressing::isAdditive));
        addSlot(new Slot(result, 0, 124, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                press(player);
                super.onTake(player, stack);
            }
        });
        ForgeMenus.addPlayerSlots(this::addSlot, inventory);
    }

    private ItemStack computeResult() {
        Level level = forge.getLevel();
        if (level == null) {
            return ItemStack.EMPTY;
        }
        return DiskPressing.result(inputs.getItem(MATERIAL), inputs.getItem(CATALYST), inputs.getItem(ADDITIVE_A),
                inputs.getItem(ADDITIVE_B), level, forge.getBlockPos());
    }

    /** Uses up the inputs of the disk that was just taken. */
    private void press(Player player) {
        ForgeMenus.consumeOne(inputs, MATERIAL, player);
        ForgeMenus.consumeOne(inputs, CATALYST, player);
        for (int slot : new int[]{ADDITIVE_A, ADDITIVE_B}) {
            if (DiskPressing.isAdditive(inputs.getItem(slot))) {
                ForgeMenus.consumeOne(inputs, slot, player);
            }
        }
        var pos = forge.getBlockPos();
        player.level().playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    @Override
    public void broadcastChanges() {
        Level level = forge.getLevel();
        if (level != null && !level.isClientSide()) {
            ItemStack next = computeResult();
            if (!ItemStack.matches(next, result.getItem(0))) {
                result.setItem(0, next);
            }
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(forge.getLevel(), forge.getBlockPos()), player, forge.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == RESULT) {
            return ItemStack.EMPTY; // take finished disks by clicking
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < INPUT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (DiskPressing.material(stack) != null) {
            if (!moveItemStackTo(stack, MATERIAL, MATERIAL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (DiskPressing.POWER_CATALYSTS.containsKey(stack.getItem())) {
            if (!moveItemStackTo(stack, CATALYST, CATALYST + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (DiskPressing.isAdditive(stack)) {
            if (!moveItemStackTo(stack, ADDITIVE_A, ADDITIVE_B + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}
