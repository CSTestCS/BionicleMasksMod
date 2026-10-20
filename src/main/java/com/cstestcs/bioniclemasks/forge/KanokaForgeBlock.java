package com.cstestcs.bioniclemasks.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Kanoka Disk Forge: presses disks from a tool material, a power catalyst and optional additives. */
public class KanokaForgeBlock extends ForgeBlock {
    public KanokaForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KanokaForgeBlockEntity(pos, state);
    }
}
