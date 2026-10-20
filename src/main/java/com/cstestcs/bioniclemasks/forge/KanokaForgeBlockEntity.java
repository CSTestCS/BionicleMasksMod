package com.cstestcs.bioniclemasks.forge;

import com.cstestcs.bioniclemasks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public class KanokaForgeBlockEntity extends ForgeBlockEntity {
    public KanokaForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KANOKA_FORGE.get(), pos, state, KanokaForgeMenu.INPUT_COUNT);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new KanokaForgeMenu(id, playerInventory, this);
    }
}
