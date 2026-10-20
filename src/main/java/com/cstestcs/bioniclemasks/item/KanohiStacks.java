package com.cstestcs.bioniclemasks.item;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.component.KanohiData;
import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.DiskTrait;
import com.cstestcs.bioniclemasks.lore.Infusion;
import com.cstestcs.bioniclemasks.lore.KanohiForm;
import com.cstestcs.bioniclemasks.lore.KanohiPower;
import com.cstestcs.bioniclemasks.lore.Metru;
import com.cstestcs.bioniclemasks.registry.ModComponents;
import com.cstestcs.bioniclemasks.registry.ModItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Helpers for building Kanohi stacks and keeping their name, color, armor and model in sync with their data. */
public final class KanohiStacks {
    /** Golden Kanohi are always golden unless dyed. */
    public static final int GOLDEN_COLOR = 0xFFC93C;

    private KanohiStacks() {
    }

    public static ItemStack create(KanohiPower power, KanohiForm form, DiskMaterial material) {
        return create(power, new KanohiData(form, material, Optional.empty(), List.of(), Metru.TA, 0));
    }

    public static ItemStack create(KanohiPower power, KanohiData data) {
        ItemStack stack = new ItemStack(ModItems.KANOHI.get(power).get());
        stack.set(ModComponents.KANOHI.get(), data);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(data.form() == KanohiForm.GOLDEN ? GOLDEN_COLOR : data.material().color()));
        refresh(stack);
        return stack;
    }

    public static KanohiData data(ItemStack stack) {
        KanohiData data = stack.get(ModComponents.KANOHI.get());
        return data != null ? data : KanohiData.DEFAULT;
    }

    public static KanohiPower power(ItemStack stack) {
        return stack.getItem() instanceof KanohiItem item ? item.power() : null;
    }

    public static void setData(ItemStack stack, KanohiData data) {
        stack.set(ModComponents.KANOHI.get(), data);
        refresh(stack);
    }

    /** Re-derives the name, armor, durability and element overlay from the mask's data. */
    public static void refresh(ItemStack stack) {
        KanohiPower power = power(stack);
        if (power == null) {
            return;
        }
        KanohiData data = data(stack);

        Component base = Component.translatable("item." + BionicleMasks.MOD_ID + "." + power.itemName());
        Component name = data.form() == KanohiForm.NUVA
                ? Component.translatable("item." + BionicleMasks.MOD_ID + ".kanohi.nuva", base)
                : Component.translatable("item." + BionicleMasks.MOD_ID + ".kanohi.named",
                Component.translatable("form." + BionicleMasks.MOD_ID + "." + data.form().id()), base);
        stack.set(DataComponents.ITEM_NAME, name);

        // Armor: the material's helmet-like protection, plus Iron element / Diamond crystal bonuses.
        DiskMaterial m = data.material();
        double armor = m.armor() + (data.has(Infusion.IRON) ? 2 : 0);
        double toughness = m.toughness() + (data.has(Infusion.IRON) ? 2 : 0) + (data.has(Infusion.DIAMOND) ? 1 : 0);
        double knockback = m.knockbackResistance() + (data.has(DiskTrait.HEAVY) ? 0.1 : 0) + (data.has(Infusion.STONE) ? 0.1 : 0);
        ItemAttributeModifiers.Builder modifiers = ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(BionicleMasks.id("kanohi.armor"), armor, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD);
        if (toughness > 0) {
            modifiers.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(BionicleMasks.id("kanohi.toughness"), toughness, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD);
        }
        if (knockback > 0) {
            modifiers.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(BionicleMasks.id("kanohi.knockback"), knockback, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers.build());

        // Durability. Preserved masks (and Legendary ones) never wear out.
        if (data.has(DiskTrait.PRESERVED) || data.form() == KanohiForm.LEGENDARY) {
            stack.remove(DataComponents.MAX_DAMAGE);
            stack.remove(DataComponents.DAMAGE);
        } else {
            int durability = (int) Math.round(m.durability() * (data.form() == KanohiForm.NUVA ? 1.5 : 1.0));
            stack.set(DataComponents.MAX_DAMAGE, durability);
            if (!stack.has(DataComponents.DAMAGE)) {
                stack.set(DataComponents.DAMAGE, 0);
            }
        }
        // Model: string 0 picks the element/crystal overlay for the upper 30%, color 0 tints it.
        String pattern = data.infusion().map(Infusion::pattern).orElse("");
        int color = data.infusion().map(Infusion::color).orElse(0xFFFFFF);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(pattern, data.form().id()), List.of(color)));
    }
}
