package com.cstestcs.bioniclemasks.lore;

import java.util.Locale;

/**
 * The forms a Kanohi can take. Matoran wear powerless masks, Turaga wear Noble masks, Toa wear
 * Great masks, Toa Nuva wear Kanohi Nuva (which can share their power with nearby allies), the Toa
 * Mata received Golden Kanohi holding all six of their masks' powers, and only three Legendary masks exist.
 */
public enum KanohiForm {
    POWERLESS("Powerless", 0.0, "A Matoran mask. It has no power, only protection."),
    NOBLE("Noble", 0.55, "A Turaga's mask. Weaker and slower to recharge than a Great mask."),
    GREAT("Great", 1.0, "A Toa's mask, at full power."),
    NUVA("Nuva", 1.25, "A Kanohi Nuva. Stronger, and its power is shared with nearby allies."),
    GOLDEN("Golden", 1.0, "A Golden Kanohi. Holds all six powers of the Toa Mata masks; press the cycle key to switch."),
    LEGENDARY("Legendary", 2.0, "One of the Legendary Kanohi.");

    private final String displayName;
    private final double potency;
    private final String description;

    KanohiForm(String displayName, double potency, String description) {
        this.displayName = displayName;
        this.potency = potency;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public double potency() {
        return potency;
    }

    public String description() {
        return description;
    }

    public boolean powered() {
        return this != POWERLESS;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The form produced by forging disks with this average power level. */
    public static KanohiForm fromAverageLevel(double averageLevel) {
        if (averageLevel < 2.5) {
            return POWERLESS;
        }
        if (averageLevel < 6.5) {
            return NOBLE;
        }
        return GREAT;
    }

    public static KanohiForm byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
