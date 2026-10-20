package com.cstestcs.bioniclemasks.lore;

import java.util.Locale;

/**
 * The six districts of Metru Nui. The number is the first digit of a Kanoka code and
 * each district's disks have their own flight trait (per BIONICLEsector01).
 */
public enum Metru {
    TA(1, "Ta-Metru", "Stronger: smashes through weak obstacles in its path."),
    GA(2, "Ga-Metru", "Steers in flight toward where the thrower is looking."),
    PO(3, "Po-Metru", "Deflects other projectiles it passes."),
    KO(4, "Ko-Metru", "Dodges obstacles, slipping past the first blocks in its way."),
    LE(5, "Le-Metru", "Flies farther and faster."),
    ONU(6, "Onu-Metru", "Returns to the thrower after it lands.");

    private final int digit;
    private final String displayName;
    private final String trait;

    Metru(int digit, String displayName, String trait) {
        this.digit = digit;
        this.displayName = displayName;
        this.trait = trait;
    }

    public int digit() {
        return digit;
    }

    public String displayName() {
        return displayName;
    }

    public String trait() {
        return trait;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Metru byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
