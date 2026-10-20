package com.cstestcs.bioniclemasks.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A block entity holding a simple inventory that is saved and dropped when the block is broken. */
public abstract class ForgeBlockEntity extends BlockEntity implements MenuProvider {
    public final SimpleContainer inventory;

    protected ForgeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.inventory = new SimpleContainer(size) {
            @Override
            public void setChanged() {
                super.setChanged();
                ForgeBlockEntity.this.setChanged();
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, inventory.getItems());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.clearContent();
        ContainerHelper.loadAllItems(input, inventory.getItems());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            Containers.dropContents(level, pos, inventory);
        }
        super.preRemoveSideEffects(pos, state);
    }
}
