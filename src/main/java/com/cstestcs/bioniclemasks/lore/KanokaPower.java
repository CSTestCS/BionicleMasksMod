package com.cstestcs.bioniclemasks.lore;

import java.util.Locale;

/**
 * The eight Kanoka disk powers. The number is the second digit of a Kanoka code.
 * Descriptions follow BIONICLEsector01.
 */
public enum KanokaPower {
    RECONSTITUTE(1, "Reconstitutes at Random", "Scrambles the molecules of a being or object at random for a while."),
    FREEZE(2, "Freeze", "Freezes the target solid and turns nearby water to ice."),
    WEAKEN(3, "Weaken", "Saps the strength of a being, or cracks and crumbles weak objects."),
    REMOVE_POISON(4, "Remove Poison", "Purges poisons and other harmful effects from the target."),
    ENLARGE(5, "Enlarge", "Makes the target grow larger. Plants grow when struck."),
    SHRINK(6, "Shrink", "Makes the target shrink."),
    REGENERATE(7, "Regenerate", "Heals beings and restores damaged objects."),
    TELEPORT(8, "Teleport", "Teleports the target a short, random distance away.");

    private final int digit;
    private final String displayName;
    private final String description;

    KanokaPower(int digit, String displayName, String description) {
        this.digit = digit;
        this.displayName = displayName;
        this.description = description;
    }

    public int digit() {
        return digit;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The single-letter shorthand used in the recipe table. */
    public char letter() {
        return switch (this) {
            case RECONSTITUTE -> 'R';
            case FREEZE -> 'F';
            case WEAKEN -> 'W';
            case REMOVE_POISON -> 'P';
            case ENLARGE -> 'E';
            case SHRINK -> 'S';
            case REGENERATE -> 'G';
            case TELEPORT -> 'T';
        };
    }

    public static KanokaPower fromLetter(char c) {
        for (KanokaPower p : values()) {
            if (p.letter() == c) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown Kanoka letter " + c);
    }

    /** The Metru whose Great Disk carries this power, or null (no Great Disk of this power exists). */
    public Metru greatDiskMetru() {
        return switch (this) {
            case ENLARGE -> Metru.TA;        // 159
            case REGENERATE -> Metru.GA;     // 279
            case WEAKEN -> Metru.PO;         // 339
            case FREEZE -> Metru.KO;         // 429
            case TELEPORT -> Metru.LE;       // 589
            case RECONSTITUTE -> Metru.ONU;  // 619
            default -> null;
        };
    }

    public static KanokaPower byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
