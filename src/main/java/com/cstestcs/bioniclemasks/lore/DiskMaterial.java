package com.cstestcs.bioniclemasks.lore;

import java.util.Locale;

/**
 * Vanilla tool materials used in place of protodermis. The tool tier sets a disk's power level
 * (the third digit of its code) and a mask's potency; each material also has its own trait.
 */
public enum DiskMaterial {
    //        level potency break  armor tough kbRes durability color
    WOOD(1, 0.60, 0.25, 1, 0.0, 0.0, 80, 0xA0804F, "Kindling: burns as forge fuel."),
    GOLD(2, 0.75, 0.20, 2, 0.0, 0.0, 110, 0xF4D23B, "Gilded: piglins stay calm and mask powers recharge 40% faster."),
    STONE(3, 0.80, 0.15, 1, 0.0, 0.05, 130, 0x8E8E8E, "Dense: hits knock targets back harder."),
    COPPER(4, 0.90, 0.12, 2, 0.0, 0.0, 170, 0xC86F45, "Conductive: one power level stronger during thunderstorms."),
    IRON(5, 1.00, 0.08, 2, 0.0, 0.0, 240, 0xDCDCDC, "Sturdy: reliable and rarely chips."),
    DIAMOND(7, 1.25, 0.04, 3, 2.0, 0.0, 400, 0x5BE3D7, "Brilliant: sharp and very hard to break."),
    NETHERITE(8, 1.50, 0.02, 3, 3.0, 0.10, 520, 0x4C4146, "Fireproof: immune to fire and lava, and steadies the wearer.");

    private final int level;
    private final double potency;
    private final double breakChance;
    private final int armor;
    private final double toughness;
    private final double knockbackResistance;
    private final int durability;
    private final int color;
    private final String trait;

    DiskMaterial(int level, double potency, double breakChance, int armor, double toughness,
                 double knockbackResistance, int durability, int color, String trait) {
        this.level = level;
        this.potency = potency;
        this.breakChance = breakChance;
        this.armor = armor;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.durability = durability;
        this.color = color;
        this.trait = trait;
    }

    /** The Kanoka power level (1-8) of a disk pressed from this material. */
    public int level() {
        return level;
    }

    public double potency() {
        return potency;
    }

    public double breakChance() {
        return breakChance;
    }

    public int armor() {
        return armor;
    }

    public double toughness() {
        return toughness;
    }

    public double knockbackResistance() {
        return knockbackResistance;
    }

    public int durability() {
        return durability;
    }

    public int color() {
        return color;
    }

    public String trait() {
        return trait;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        String id = id();
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    public static DiskMaterial byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
