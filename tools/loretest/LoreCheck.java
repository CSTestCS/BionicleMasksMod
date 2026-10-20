import com.cstestcs.bioniclemasks.lore.*;
import java.util.*;

/** Standalone checks for the Minecraft-free lore rules. Run with tools/check_lore.sh. */
public class LoreCheck {
    static int failures = 0;

    static void check(boolean ok, String what) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }

    static MaskForging.Disk disk(KanokaPower p, DiskMaterial m) {
        return new MaskForging.Disk(p, m, m.level(), Set.of(), Metru.TA, false, false);
    }

    public static void main(String[] args) {
        // Every mask must have a recipe (or be the Vahi) and all recipes must be unique (checked in static init).
        for (KanohiPower p : KanohiPower.values()) {
            check(p == KanohiPower.VAHI || !p.recipe().isEmpty(), p + " has a recipe");
            if (!p.recipe().isEmpty()) {
                check(KanohiPower.byRecipe(p.recipe()) == p, p + " recipe resolves back");
            }
        }
        System.out.println("Masks: " + KanohiPower.values().length + " " + KanohiPower.countByGroup());

        Random random = new Random(1);
        // Matching iron disks give a Noble Hau.
        var hau = KanohiPower.HAU.recipe().stream().map(p -> disk(p, DiskMaterial.IRON)).toList();
        var out = MaskForging.forge(hau, MaskForging.Catalyst.NONE, random);
        check(out.ok() && out.mask() == KanohiPower.HAU && out.form() == KanohiForm.NOBLE && !out.mismatched(), "iron hau noble");
        // Diamond gives Great, wood gives powerless.
        var great = MaskForging.forge(KanohiPower.HAU.recipe().stream().map(p -> disk(p, DiskMaterial.DIAMOND)).toList(), MaskForging.Catalyst.NONE, random);
        check(great.form() == KanohiForm.GREAT, "diamond great");
        var wood = MaskForging.forge(KanohiPower.HAU.recipe().stream().map(p -> disk(p, DiskMaterial.WOOD)).toList(), MaskForging.Catalyst.NONE, random);
        check(wood.form() == KanohiForm.POWERLESS, "wood powerless");

        // Mismatched materials: about 25% powered.
        int powered = 0, trials = 100000;
        for (int i = 0; i < trials; i++) {
            var mixed = List.of(disk(KanokaPower.REGENERATE, DiskMaterial.DIAMOND), disk(KanokaPower.REGENERATE, DiskMaterial.NETHERITE), disk(KanokaPower.FREEZE, DiskMaterial.DIAMOND));
            var o = MaskForging.forge(mixed, MaskForging.Catalyst.NONE, random);
            check(o.mismatched(), "mismatch flagged");
            check(o.material() == DiskMaterial.DIAMOND || o.material() == DiskMaterial.NETHERITE, "material from inputs");
            if (o.form().powered()) powered++;
        }
        double rate = powered / (double) trials;
        System.out.printf("Mismatched powered rate: %.4f%n", rate);
        check(Math.abs(rate - 0.25) < 0.01, "25% chance");

        // Legendary rules.
        var ignika = KanohiPower.IGNIKA.recipe().stream().map(p -> disk(p, DiskMaterial.NETHERITE)).toList();
        check(!MaskForging.forge(ignika, MaskForging.Catalyst.NONE, random).ok(), "ignika needs totem");
        var ig = MaskForging.forge(ignika, MaskForging.Catalyst.TOTEM_OF_UNDYING, random);
        check(ig.ok() && ig.form() == KanohiForm.LEGENDARY && ig.consumesCatalyst(), "ignika forged");
        var ironIgnika = KanohiPower.IGNIKA.recipe().stream().map(p -> disk(p, DiskMaterial.IRON)).toList();
        check(!MaskForging.forge(ironIgnika, MaskForging.Catalyst.TOTEM_OF_UNDYING, random).ok(), "ignika needs level 8");

        // Great Disks -> Disk of Time -> Vahi.
        List<MaskForging.Disk> greats = new ArrayList<>();
        for (KanokaPower p : KanokaPower.values()) {
            if (p.greatDiskMetru() != null) greats.add(new MaskForging.Disk(p, DiskMaterial.NETHERITE, 9, Set.of(), p.greatDiskMetru(), true, false));
        }
        check(greats.size() == 6, "six great disks");
        check(MaskForging.forge(greats, MaskForging.Catalyst.NONE, random).diskOfTime(), "disk of time");
        var vahi = MaskForging.forge(List.of(new MaskForging.Disk(null, DiskMaterial.NETHERITE, 9, Set.of(), Metru.TA, false, true)), MaskForging.Catalyst.NONE, random);
        check(vahi.mask() == KanohiPower.VAHI && vahi.form() == KanohiForm.LEGENDARY, "vahi");
        // Great disk codes match the lore: 159 279 339 429 589 619.
        Set<String> codes = new TreeSet<>();
        for (var g : greats) codes.add(MaskForging.code(g.metru(), g.power(), 9));
        check(codes.equals(new TreeSet<>(List.of("159", "279", "339", "429", "589", "619"))), "great disk codes " + codes);

        System.out.println(failures == 0 ? "ALL LORE CHECKS PASSED" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }
}
