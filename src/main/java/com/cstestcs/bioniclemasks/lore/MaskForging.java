package com.cstestcs.bioniclemasks.lore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The rules of the Mask Forge, kept free of Minecraft classes so they can be tested on their own.
 *
 * <ul>
 *     <li>The multiset of disk powers picks the mask (see {@link KanohiPower#recipe()}).</li>
 *     <li>The average power level picks the form: below 2.5 powerless, below 6.5 Noble, otherwise Great.</li>
 *     <li>If the disks are not all the same material, there is only a 25% chance the mask is powered at
 *     all. If it is, it takes the material of one of the disks at random; if not, it comes out as a
 *     plain powerless mask.</li>
 *     <li>Legendary masks need six disks of level 8 or more plus a catalyst. The six Great Disks together
 *     make the Disk of Time, and the Disk of Time alone makes the Vahi.</li>
 * </ul>
 */
public final class MaskForging {
    public static final double MISMATCH_SUCCESS_CHANCE = 0.25;
    public static final int LEGENDARY_MIN_LEVEL = 8;

    private MaskForging() {
    }

    /** One disk in the forge. {@code power} is null only for the Disk of Time. */
    public record Disk(KanokaPower power, DiskMaterial material, int level, Set<DiskTrait> traits, Metru metru,
                       boolean great, boolean time) {
    }

    public enum Catalyst { NONE, TOTEM_OF_UNDYING, NETHER_STAR, OTHER }

    /** What the forge produced. Exactly one of {@code mask} / {@code diskOfTime} / {@code error} is set. */
    public record Outcome(KanohiPower mask, KanohiForm form, DiskMaterial material, Set<DiskTrait> traits, Metru origin,
                          boolean diskOfTime, boolean mismatched, boolean consumesCatalyst, String error) {
        public static Outcome error(String message) {
            return new Outcome(null, null, null, Set.of(), null, false, false, false, message);
        }

        public boolean ok() {
            return error == null;
        }
    }

    public static Outcome forge(List<Disk> disks, Catalyst catalyst, Random random) {
        if (disks.isEmpty()) {
            return Outcome.error("empty");
        }

        // The six Great Disks become the Disk of Time.
        if (disks.size() == 6 && disks.stream().allMatch(Disk::great)) {
            Set<Metru> metru = EnumSet.noneOf(Metru.class);
            disks.forEach(d -> metru.add(d.metru()));
            if (metru.size() == 6) {
                return new Outcome(null, null, DiskMaterial.NETHERITE, unionTraits(disks), Metru.TA,
                        true, false, false, null);
            }
        }

        // The Disk of Time is carved into the Vahi.
        if (disks.size() == 1 && disks.getFirst().time()) {
            Disk time = disks.getFirst();
            return new Outcome(KanohiPower.VAHI, KanohiForm.LEGENDARY, time.material(), time.traits(), Metru.TA,
                    false, false, false, null);
        }
        if (disks.stream().anyMatch(Disk::time)) {
            return Outcome.error("time_alone");
        }

        List<KanokaPower> powers = new ArrayList<>();
        disks.forEach(d -> powers.add(d.power()));
        KanohiPower mask = KanohiPower.byRecipe(powers);
        if (mask == null) {
            return Outcome.error("no_recipe");
        }

        boolean consumesCatalyst = false;
        if (mask.legendary()) {
            Catalyst needed = mask == KanohiPower.IGNIKA ? Catalyst.TOTEM_OF_UNDYING : Catalyst.NETHER_STAR;
            if (catalyst != needed) {
                return Outcome.error(mask == KanohiPower.IGNIKA ? "needs_totem" : "needs_nether_star");
            }
            if (disks.stream().anyMatch(d -> d.level() < LEGENDARY_MIN_LEVEL)) {
                return Outcome.error("legendary_level");
            }
            consumesCatalyst = true;
        }

        Set<DiskMaterial> materials = EnumSet.noneOf(DiskMaterial.class);
        disks.forEach(d -> materials.add(d.material()));
        boolean mismatched = materials.size() > 1;

        // Mismatched disks: 25% chance of a powered mask, made of one of the disks' materials at random.
        // On failure the mask comes out plain and powerless.
        boolean powered = !mismatched || random.nextDouble() < MISMATCH_SUCCESS_CHANCE;
        DiskMaterial material = mismatched
                ? disks.get(random.nextInt(disks.size())).material()
                : disks.getFirst().material();

        KanohiForm form;
        Set<DiskTrait> traits;
        if (!powered) {
            form = KanohiForm.POWERLESS;
            traits = Set.of();
        } else if (mask.legendary()) {
            form = KanohiForm.LEGENDARY;
            traits = unionTraits(disks);
        } else {
            double average = disks.stream().mapToInt(Disk::level).average().orElse(0);
            form = KanohiForm.fromAverageLevel(average);
            traits = form.powered() ? unionTraits(disks) : Set.of();
        }

        return new Outcome(mask, form, material, traits, commonestMetru(disks), false, mismatched, consumesCatalyst, null);
    }

    private static Set<DiskTrait> unionTraits(List<Disk> disks) {
        Set<DiskTrait> traits = new LinkedHashSet<>();
        disks.forEach(d -> traits.addAll(d.traits()));
        return traits;
    }

    private static Metru commonestMetru(List<Disk> disks) {
        Map<Metru, Integer> counts = new EnumMap<>(Metru.class);
        disks.forEach(d -> counts.merge(d.metru(), 1, Integer::sum));
        Metru best = disks.getFirst().metru();
        for (Map.Entry<Metru, Integer> e : counts.entrySet()) {
            if (e.getValue() > counts.get(best)) {
                best = e.getKey();
            }
        }
        return best;
    }

    /** The three-digit Kanoka code: origin Metru, power, power level. */
    public static String code(Metru metru, KanokaPower power, int level) {
        return "" + metru.digit() + power.digit() + level;
    }
}
