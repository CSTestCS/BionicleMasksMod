package com.cstestcs.bioniclemasks.mask;

import com.cstestcs.bioniclemasks.component.KanohiData;
import com.cstestcs.bioniclemasks.item.KanohiItem;
import com.cstestcs.bioniclemasks.item.KanohiStacks;
import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.DiskTrait;
import com.cstestcs.bioniclemasks.lore.Infusion;
import com.cstestcs.bioniclemasks.lore.KanohiForm;
import com.cstestcs.bioniclemasks.lore.KanohiPower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Everything a mask power needs to know about the mask being worn, with all of its form, material,
 * trait and infusion modifiers already worked out.
 *
 * @param power    the active power (for a Golden Kanohi, the selected one of the six Mata powers)
 * @param strength overall potency: 0.55 for an iron Noble mask up to 3+ for a Legendary one
 * @param amp      a 0-4 effect amplifier derived from strength
 */
public record MaskContext(ServerPlayer player, ServerLevel level, ItemStack stack, KanohiPower power, KanohiData data,
                          double strength, int amp, double range, double duration, double cooldown, boolean silent,
                          boolean elementalEnergy) {

    /** The context for whatever powered mask the player wears, or null. */
    public static MaskContext of(ServerPlayer player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(stack.getItem() instanceof KanohiItem item)) {
            return null;
        }
        KanohiData data = KanohiStacks.data(stack);
        if (!data.form().powered()) {
            return null;
        }
        KanohiPower power = item.power();
        if (data.form() == KanohiForm.GOLDEN) {
            power = KanohiPower.MATA_MASKS.get(Math.floorMod(data.selected(), KanohiPower.MATA_MASKS.size()));
        }
        boolean elementalEnergy = item.power() == KanohiPower.ELEMENTAL_ENERGY && MaskState.get(player).active;

        double strength = data.form().potency() * data.material().potency();
        double infusionBonus = data.infusion().map(Infusion::potencyBonus).orElse(0.0);
        strength *= 1.0 + infusionBonus * (elementalEnergy ? 2 : 1);
        if (data.has(DiskTrait.EMPOWERED)) {
            strength *= 1.15;
        }
        int amp = (int) Math.max(0, Math.min(4, Math.round(strength * 2) - 1));

        double range = data.has(DiskTrait.RESONANT) ? 1.5 : 1.0;
        double duration = data.has(DiskTrait.LASTING) ? 1.5 : 1.0;
        double cooldown = 1.0;
        if (data.form() == KanohiForm.NOBLE) {
            cooldown *= 1.5;
        }
        if (data.has(DiskTrait.SWIFT)) {
            cooldown *= 0.8;
        }
        if (data.material() == DiskMaterial.GOLD) {
            cooldown *= 0.6;
        }
        if (data.has(Infusion.QUARTZ)) {
            cooldown *= 0.75;
        }
        boolean silent = data.has(DiskTrait.SILENT) || data.has(Infusion.ECHO);
        return new MaskContext(player, (ServerLevel) player.level(), stack, power, data, strength, amp, range, duration, cooldown, silent, elementalEnergy);
    }

    /** Ticks for an effect whose base length is {@code seconds}, scaled by strength and the Lasting trait. */
    public int ticks(double seconds) {
        return (int) Math.round(seconds * 20 * duration * Math.max(0.6, Math.min(2.0, strength)));
    }

    public double radius(double base) {
        return base * range * Math.max(0.75, Math.min(2.0, strength));
    }

    public boolean nuva() {
        return data.form() == KanohiForm.NUVA;
    }

    public boolean greatOrBetter() {
        return data.form() != KanohiForm.NOBLE;
    }
}
