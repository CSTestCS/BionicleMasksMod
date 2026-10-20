package com.cstestcs.bioniclemasks.forge;

import com.cstestcs.bioniclemasks.item.KanohiItem;
import com.cstestcs.bioniclemasks.item.KanokaItem;
import com.cstestcs.bioniclemasks.mask.MaskPowers;
import com.cstestcs.bioniclemasks.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Kanohi Mask Forge: six disk slots, a catalyst slot, a fuel slot and the output.
 * The "Forge" button (menu button 0) does the work.
 */
public class MaskForgeMenu extends AbstractContainerMenu {
    public static final int DISKS = 6;
    public static final int CATALYST = 6;
    public static final int FUEL = 7;
    public static final int OUTPUT = 8;
    public static final int SLOT_COUNT = 9;
    private static final int PLAYER_START = SLOT_COUNT;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final MaskForgeBlockEntity forge;
    private final Container container;

    public MaskForgeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, (MaskForgeBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public MaskForgeMenu(int id, Inventory inventory, MaskForgeBlockEntity forge) {
        super(ModMenus.MASK_FORGE.get(), id);
        this.forge = forge;
        this.container = forge.inventory;
        for (int i = 0; i < DISKS; i++) {
            addSlot(ForgeMenus.filtered(container, i, 44 + (i % 3) * 18, 20 + (i / 3) * 18,
                    s -> s.getItem() instanceof KanokaItem || s.getItem() instanceof KanohiItem));
        }
        addSlot(new Slot(container, CATALYST, 62, 64));
        addSlot(ForgeMenus.filtered(container, FUEL, 14, 64, MaskForgeLogic::isFuel));
        addSlot(ForgeMenus.output(container, OUTPUT, 134, 38));
        ForgeMenus.addPlayerSlots(this::addSlot, inventory);
    }

    public List<ItemStack> disks() {
        List<ItemStack> disks = new ArrayList<>();
        for (int i = 0; i < DISKS; i++) {
            disks.add(slots.get(i).getItem());
        }
        return disks;
    }

    public ItemStack catalyst() {
        return slots.get(CATALYST).getItem();
    }

    /** What the forge would make right now, for the screen's preview line. */
    public MaskForgeLogic.Plan preview() {
        return MaskForgeLogic.plan(disks(), catalyst(), null);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != 0 || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        forge(serverPlayer);
        return true;
    }

    private void forge(ServerPlayer player) {
        if (!container.getItem(OUTPUT).isEmpty()) {
            MaskPowers.tell(player, "Take the finished piece out of the forge first.", 0xFFAA55);
            return;
        }
        if (!MaskForgeLogic.isFuel(container.getItem(FUEL))) {
            MaskPowers.tell(player, "The forge is cold. Add fuel: coal, charcoal, blaze rods or lava.", 0xFFAA55);
            return;
        }
        Random random = new Random(player.getRandom().nextLong());
        MaskForgeLogic.Plan plan = MaskForgeLogic.plan(disks(), catalyst(), random);
        if (plan.output().isEmpty()) {
            MaskPowers.tell(player, plan.message(), plan.color());
            return;
        }
        if (plan.consumeDisks()) {
            for (int i = 0; i < DISKS; i++) {
                ForgeMenus.consumeOne(container, i, player);
            }
        }
        if (plan.consumeCatalyst()) {
            ForgeMenus.consumeOne(container, CATALYST, player);
        }
        ForgeMenus.consumeOne(container, FUEL, player);
        container.setItem(OUTPUT, plan.output());
        container.setChanged();
        MaskPowers.tell(player, plan.message(), plan.color());
        var pos = forge.getBlockPos();
        player.level().playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
        player.level().playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(forge.getLevel(), forge.getBlockPos()), player, forge.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof KanokaItem || stack.getItem() instanceof KanohiItem) {
            if (!moveItemStackTo(stack, 0, DISKS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MaskForgeLogic.isFuel(stack)) {
            if (!moveItemStackTo(stack, FUEL, FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, CATALYST, CATALYST + 1, false)) {
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
