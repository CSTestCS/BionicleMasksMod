package com.cstestcs.bioniclemasks.lore;

import java.util.Locale;

/**
 * Extra traits added by optional additive materials in the Kanoka Disk Forge. A Kanohi forged from
 * disks inherits every trait its disks carried.
 */
public enum DiskTrait {
    EMPOWERED("Empowered", "+1 power level (up to 8).", "Mask powers are 15% stronger."),
    LASTING("Lasting", "Effects last 50% longer.", "Mask effects last 50% longer."),
    RESONANT("Resonant", "Power spreads to everything near the impact.", "Mask power range +50%."),
    SWIFT("Swift", "Thrown 50% faster.", "Mask powers recharge 20% faster."),
    BOUNCING("Bouncing", "Ricochets off the first block it hits.", "Halves fall damage."),
    HEAVY("Heavy", "Hits harder and knocks targets back.", "Wearer resists knockback."),
    GALE("Gale", "Bursts into a gust of wind on impact.", "Taking a hit blasts attackers away."),
    PRESERVED("Preserved", "Never breaks.", "Mask never loses durability."),
    FORTUNATE("Fortunate", "Half the time, the power strikes twice.", "Grants Luck while worn."),
    SILENT("Silent", "Makes no sound.", "Powers make no sound.");

    private final String displayName;
    private final String diskEffect;
    private final String maskEffect;

    DiskTrait(String displayName, String diskEffect, String maskEffect) {
        this.displayName = displayName;
        this.diskEffect = diskEffect;
        this.maskEffect = maskEffect;
    }

    public String displayName() {
        return displayName;
    }

    public String diskEffect() {
        return diskEffect;
    }

    public String maskEffect() {
        return maskEffect;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DiskTrait byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
