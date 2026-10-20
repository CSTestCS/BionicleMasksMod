package com.cstestcs.bioniclemasks.registry;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.item.KanohiStacks;
import com.cstestcs.bioniclemasks.item.KanokaStacks;
import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.KanohiForm;
import com.cstestcs.bioniclemasks.lore.KanohiPower;
import com.cstestcs.bioniclemasks.lore.KanokaPower;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Two creative tabs: every Kanoka (each material) and every Kanohi (each form). */
public final class ModTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BionicleMasks.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KANOKA = TABS.register("kanoka", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bioniclemasks.kanoka"))
            .icon(() -> KanokaStacks.create(KanokaPower.TELEPORT, DiskMaterial.IRON))
            .displayItems((params, output) -> {
                output.accept(new ItemStack(ModItems.KANOKA_FORGE.get()));
                output.accept(new ItemStack(ModItems.MASK_FORGE.get()));
                for (KanokaPower power : KanokaPower.values()) {
                    for (DiskMaterial material : DiskMaterial.values()) {
                        output.accept(KanokaStacks.create(power, material));
                    }
                }
                ModItems.GREAT_DISKS.values().forEach(item -> output.accept(new ItemStack(item.get())));
                output.accept(new ItemStack(ModItems.DISK_OF_TIME.get()));
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KANOHI = TABS.register("kanohi", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bioniclemasks.kanohi"))
            .icon(() -> KanohiStacks.create(KanohiPower.HAU, KanohiForm.GREAT, DiskMaterial.GOLD))
            .displayItems((params, output) -> {
                for (KanohiPower power : KanohiPower.values()) {
                    for (KanohiForm form : power.forms()) {
                        DiskMaterial material = switch (form) {
                            case POWERLESS -> DiskMaterial.STONE;
                            case NOBLE -> DiskMaterial.IRON;
                            case GREAT -> DiskMaterial.DIAMOND;
                            case NUVA, LEGENDARY -> DiskMaterial.NETHERITE;
                            case GOLDEN -> DiskMaterial.GOLD;
                        };
                        output.accept(KanohiStacks.create(power, form, material));
                    }
                }
            })
            .build());

    private ModTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
