package com.cstestcs.bioniclemasks.registry;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.forge.KanokaForgeBlock;
import com.cstestcs.bioniclemasks.forge.MaskForgeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BionicleMasks.MOD_ID);

    /** Where Kanohi are forged from melted Kanoka, like Vakama's forge in Ta-Metru. */
    public static final DeferredBlock<MaskForgeBlock> MASK_FORGE = BLOCKS.registerBlock("mask_forge", MaskForgeBlock::new,
            p -> p.mapColor(MapColor.NETHER).strength(3.5F).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(state -> 7));

    /** Where Kanoka disks are pressed from vanilla tool materials. */
    public static final DeferredBlock<KanokaForgeBlock> KANOKA_FORGE = BLOCKS.registerBlock("kanoka_forge", KanokaForgeBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
