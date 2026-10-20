package com.cstestcs.bioniclemasks.registry;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.forge.KanokaForgeBlockEntity;
import com.cstestcs.bioniclemasks.forge.MaskForgeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BionicleMasks.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaskForgeBlockEntity>> MASK_FORGE =
            TYPES.register("mask_forge", () -> new BlockEntityType<>(MaskForgeBlockEntity::new, ModBlocks.MASK_FORGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KanokaForgeBlockEntity>> KANOKA_FORGE =
            TYPES.register("kanoka_forge", () -> new BlockEntityType<>(KanokaForgeBlockEntity::new, ModBlocks.KANOKA_FORGE.get()));

    private ModBlockEntities() {
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
