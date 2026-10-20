package com.cstestcs.bioniclemasks.item;

import com.cstestcs.bioniclemasks.component.KanohiData;
import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.KanohiForm;
import com.cstestcs.bioniclemasks.lore.KanohiPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A Kanohi mask. Wear it on your head (right-click it, or put it in the helmet slot) and press the
 * "Use mask power" key (V by default). Its powers are handled in {@code mask.MaskPowers}.
 */
public class KanohiItem extends Item {
    private final KanohiPower power;

    public KanohiItem(Properties properties, KanohiPower power) {
        super(properties);
        this.power = power;
    }

    public KanohiPower power() {
        return power;
    }

    /** Gold masks (and Golden Kanohi) keep piglins calm, like gold armor. */
    @Override
    public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
        KanohiData data = KanohiStacks.data(stack);
        return data.material() == DiskMaterial.GOLD || data.form() == KanohiForm.GOLDEN;
    }
}
