package com.cstestcs.bioniclemasks.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Kanohi Mask Forge: melts Kanoka down into masks, and infuses, dyes and upgrades finished masks. */
public class MaskForgeBlock extends ForgeBlock {
    public MaskForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaskForgeBlockEntity(pos, state);
    }
}
