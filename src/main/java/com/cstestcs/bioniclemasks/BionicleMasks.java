package com.cstestcs.bioniclemasks;

import com.cstestcs.bioniclemasks.network.ModNetwork;
import com.cstestcs.bioniclemasks.registry.ModBlockEntities;
import com.cstestcs.bioniclemasks.registry.ModBlocks;
import com.cstestcs.bioniclemasks.registry.ModComponents;
import com.cstestcs.bioniclemasks.registry.ModEffects;
import com.cstestcs.bioniclemasks.registry.ModItems;
import com.cstestcs.bioniclemasks.registry.ModMenus;
import com.cstestcs.bioniclemasks.registry.ModTabs;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * BIONICLE Kanoka disks and Kanohi masks.
 *
 * <p>Gameplay events live in {@code kanoka.KanokaEvents} and {@code mask.MaskEvents}; they register
 * themselves with {@code @EventBusSubscriber}.</p>
 */
@Mod(BionicleMasks.MOD_ID)
public final class BionicleMasks {
    public static final String MOD_ID = "bioniclemasks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BionicleMasks(IEventBus modBus) {
        ModComponents.register(modBus);
        ModEffects.register(modBus);
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModTabs.register(modBus);
        modBus.addListener(ModNetwork::register);
        LOGGER.info("The Great Spirit Mata Nui watches over the forges.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
