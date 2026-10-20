package com.cstestcs.bioniclemasks.forge;

import com.cstestcs.bioniclemasks.component.KanohiData;
import com.cstestcs.bioniclemasks.component.KanokaData;
import com.cstestcs.bioniclemasks.item.KanohiItem;
import com.cstestcs.bioniclemasks.item.KanohiStacks;
import com.cstestcs.bioniclemasks.item.KanokaItem;
import com.cstestcs.bioniclemasks.item.KanokaStacks;
import com.cstestcs.bioniclemasks.kanoka.Tags;
import com.cstestcs.bioniclemasks.lore.Infusion;
import com.cstestcs.bioniclemasks.lore.KanohiForm;
import com.cstestcs.bioniclemasks.lore.KanohiPower;
import com.cstestcs.bioniclemasks.lore.MaskForging;
import com.cstestcs.bioniclemasks.registry.ModItems;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Turns what is in the Mask Forge into a result. Pure rules live in {@link MaskForging}; this class
 * reads and writes Minecraft item stacks around them.
 */
public final class MaskForgeLogic {
    private MaskForgeLogic() {
    }

    /** What forging would do. {@code output} is empty when it cannot forge; {@code message} explains. */
    public record Plan(ItemStack output, String message, boolean consumeDisks, boolean consumeCatalyst, int color) {
        static Plan fail(String message) {
            return new Plan(ItemStack.EMPTY, message, false, false, 0xFF7777);
        }
    }

    /**
     * Works out the result of forging.
     *
     * @param random the forge's random source; pass null for a preview, which never rolls dice
     */
    public static Plan plan(List<ItemStack> disks, ItemStack catalyst, Random random) {
        List<ItemStack> filled = disks.stream().filter(s -> !s.isEmpty()).toList();
        if (filled.isEmpty()) {
            return Plan.fail("Place Kanoka disks - or a finished Kanohi - in the forge.");
        }
        long masks = filled.stream().filter(s -> s.getItem() instanceof KanohiItem).count();
        if (masks == filled.size()) {
            return masks == 1 ? finishing(filled.getFirst(), catalyst) : golden(filled, catalyst);
        }
        if (masks > 0) {
            return Plan.fail("Kanoka and Kanohi cannot be forged together.");
        }
        return forging(filled, catalyst, random);
    }

    /** Melting disks into a mask (or the six Great Disks into the Disk of Time). */
    private static Plan forging(List<ItemStack> filled, ItemStack catalyst, Random random) {
        List<MaskForging.Disk> disks = new ArrayList<>();
        for (ItemStack stack : filled) {
            KanokaItem item = (KanokaItem) stack.getItem();
            KanokaData data = KanokaStacks.data(stack);
            disks.add(new MaskForging.Disk(item.power(), data.material(), data.level(), Set.copyOf(data.traits()), data.metru(),
                    item.kind() == KanokaItem.Kind.GREAT, item.kind() == KanokaItem.Kind.TIME));
        }
        MaskForging.Catalyst type = catalyst.isEmpty() ? MaskForging.Catalyst.NONE
                : catalyst.is(Items.TOTEM_OF_UNDYING) ? MaskForging.Catalyst.TOTEM_OF_UNDYING
                : catalyst.is(Items.NETHER_STAR) ? MaskForging.Catalyst.NETHER_STAR : MaskForging.Catalyst.OTHER;

        // Previews never roll the dice: show what the disks are trying to become.
        Random dice = random != null ? random : new Random(0);
        MaskForging.Outcome outcome = MaskForging.forge(disks, type, dice);
        if (!outcome.ok()) {
            return Plan.fail(switch (outcome.error()) {
                case "no_recipe" -> "These disks do not form any Kanohi.";
                case "needs_totem" -> "The Mask of Life needs a Totem of Undying as its catalyst.";
                case "needs_nether_star" -> "The Mask of Creation needs a Nether Star as its catalyst.";
                case "legendary_level" -> "Legendary masks need disks of power level 8 or more.";
                case "time_alone" -> "The Disk of Time must be carved alone.";
                default -> "The forge cannot use this.";
            });
        }
        if (outcome.diskOfTime()) {
            return new Plan(new ItemStack(ModItems.DISK_OF_TIME.get()), "The six Great Disks merge into the Disk of Time!", true, false, 0xFFD45A);
        }
        KanohiData data = new KanohiData(outcome.form(), outcome.material(), Optional.empty(), List.copyOf(outcome.traits()), outcome.origin(), 0);
        ItemStack mask = KanohiStacks.create(outcome.mask(), data);
        String name = outcome.mask().displayName();
        String message;
        if (random == null) {
            message = outcome.mismatched()
                    ? "Will forge " + name + ". The disks do not match - only a 25% chance it will hold any power."
                    : "Will forge a " + outcome.form().displayName() + " " + name + " (" + outcome.material().displayName() + ").";
        } else if (outcome.mismatched()) {
            message = outcome.form().powered()
                    ? "The mismatched disks bonded! A " + outcome.form().displayName() + " " + name + " of " + outcome.material().displayName() + "."
                    : "The mismatched disks did not bond. The " + name + " came out powerless.";
        } else {
            message = "Forged a " + outcome.form().displayName() + " " + name + ".";
        }
        return new Plan(mask, message, true, outcome.consumesCatalyst(), outcome.form().powered() ? 0x7FFF7F : 0xFFAA55);
    }

    /** One finished mask plus a catalyst: element/crystal infusion, dye, or a Kanohi Nuva upgrade. */
    private static Plan finishing(ItemStack mask, ItemStack catalyst) {
        if (catalyst.isEmpty()) {
            return Plan.fail("Add an element, crystal, dye or Dragon's Breath as the catalyst.");
        }
        KanohiPower power = KanohiStacks.power(mask);
        KanohiData data = KanohiStacks.data(mask);
        ItemStack result = mask.copyWithCount(1);

        Infusion infusion = Infusion.byItemId(BuiltInRegistries.ITEM.getKey(catalyst.getItem()).toString());
        if (infusion != null) {
            KanohiStacks.setData(result, data.withInfusion(infusion));
            String kind = infusion.kind() == Infusion.Kind.ELEMENT ? "element of " : "crystal: ";
            return new Plan(result, "Infuse the mask with the " + kind + infusion.displayName() + ".", true, true, infusion.color());
        }

        DyeColor dye = catalyst.get(DataComponents.DYE);
        if (dye != null) {
            int mixed = mix(mask.get(DataComponents.DYED_COLOR), dye.getTextureDiffuseColor());
            result.set(DataComponents.DYED_COLOR, new DyedItemColor(mixed));
            return new Plan(result, "Dye the mask " + dye.getName() + ".", true, true, mixed);
        }

        if (catalyst.is(Items.DRAGON_BREATH)) {
            if (!power.mata() || data.form() != KanohiForm.GREAT) {
                return Plan.fail("Only a Great mask of the Toa Mata can become a Kanohi Nuva.");
            }
            KanohiStacks.setData(result, data.withForm(KanohiForm.NUVA));
            return new Plan(result, "Energize the mask into a Kanohi " + power.maskName() + " Nuva.", true, true, 0x9FE0FF);
        }
        return Plan.fail("That catalyst does nothing to a finished mask.");
    }

    /** The six Great masks of the Toa Mata plus a block of gold make a Golden Kanohi. */
    private static Plan golden(List<ItemStack> masks, ItemStack catalyst) {
        EnumSet<KanohiPower> found = EnumSet.noneOf(KanohiPower.class);
        for (ItemStack mask : masks) {
            KanohiPower power = KanohiStacks.power(mask);
            KanohiForm form = KanohiStacks.data(mask).form();
            if (power == null || !power.mata() || (form != KanohiForm.GREAT && form != KanohiForm.NUVA)) {
                return Plan.fail("A Golden Kanohi is made from the six Great masks of the Toa Mata.");
            }
            found.add(power);
        }
        if (found.size() != 6) {
            return Plan.fail("A Golden Kanohi needs one each of Hau, Kaukau, Miru, Kakama, Pakari and Akaku.");
        }
        if (!catalyst.is(Items.GOLD_BLOCK)) {
            return Plan.fail("A Golden Kanohi needs a Block of Gold as its catalyst.");
        }
        ItemStack first = masks.getFirst();
        KanohiPower shape = KanohiStacks.power(first);
        KanohiData data = KanohiStacks.data(first);
        int selected = KanohiPower.MATA_MASKS.indexOf(shape);
        ItemStack golden = KanohiStacks.create(shape, new KanohiData(KanohiForm.GOLDEN, data.material(), data.infusion(), data.traits(), data.origin(), selected));
        return new Plan(golden, "The six masks fuse into a Golden Kanohi " + shape.maskName() + ".", true, true, KanohiStacks.GOLDEN_COLOR);
    }

    /** Vanilla-style dye mixing: average the colors, then restore brightness. */
    private static int mix(DyedItemColor existing, int dye) {
        int[] colors = existing != null ? new int[]{existing.rgb(), dye} : new int[]{dye};
        int r = 0;
        int g = 0;
        int b = 0;
        int brightness = 0;
        for (int c : colors) {
            int cr = (c >> 16) & 0xFF;
            int cg = (c >> 8) & 0xFF;
            int cb = c & 0xFF;
            r += cr;
            g += cg;
            b += cb;
            brightness += Math.max(cr, Math.max(cg, cb));
        }
        int n = colors.length;
        r /= n;
        g /= n;
        b /= n;
        float avgBrightness = brightness / (float) n;
        float max = Math.max(r, Math.max(g, b));
        if (max > 0) {
            r = (int) (r * avgBrightness / max);
            g = (int) (g * avgBrightness / max);
            b = (int) (b * avgBrightness / max);
        }
        return (r << 16) | (g << 8) | b;
    }

    /** Whether this item can be burned in the forge. */
    public static boolean isFuel(ItemStack stack) {
        return stack.is(Tags.FORGE_FUEL);
    }
}
