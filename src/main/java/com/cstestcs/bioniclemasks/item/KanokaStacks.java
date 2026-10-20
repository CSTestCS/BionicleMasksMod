package com.cstestcs.bioniclemasks.item;

import com.cstestcs.bioniclemasks.component.KanokaData;
import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.KanokaPower;
import com.cstestcs.bioniclemasks.lore.Metru;
import com.cstestcs.bioniclemasks.registry.ModComponents;
import com.cstestcs.bioniclemasks.registry.ModItems;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/** Helpers for building Kanoka disk stacks. */
public final class KanokaStacks {
    private KanokaStacks() {
    }

    public static ItemStack create(KanokaPower power, DiskMaterial material) {
        return create(power, new KanokaData(material, material.level(), Metru.TA, List.of()));
    }

    public static ItemStack create(KanokaPower power, KanokaData data) {
        ItemStack stack = new ItemStack(ModItems.KANOKA.get(power).get());
        stack.set(ModComponents.KANOKA.get(), data);
        refresh(stack);
        return stack;
    }

    public static KanokaData data(ItemStack stack) {
        KanokaData data = stack.get(ModComponents.KANOKA.get());
        return data != null ? data : KanokaData.DEFAULT;
    }

    /** Tints the disk with its material's color. */
    public static void refresh(ItemStack stack) {
        KanokaData data = data(stack);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(data.material().id()),
                List.of(data.material().color())));
    }
}
