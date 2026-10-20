package com.cstestcs.bioniclemasks.registry;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.forge.KanokaForgeMenu;
import com.cstestcs.bioniclemasks.forge.MaskForgeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BionicleMasks.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MaskForgeMenu>> MASK_FORGE =
            MENUS.register("mask_forge", () -> IMenuTypeExtension.create(MaskForgeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<KanokaForgeMenu>> KANOKA_FORGE =
            MENUS.register("kanoka_forge", () -> IMenuTypeExtension.create(KanokaForgeMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
