package com.cstestcs.bioniclemasks.lore;

import java.util.Locale;

/**
 * Elements and crystals that can be infused into a Kanohi at the Mask Forge. An infused mask has the
 * upper 30% of its model turned into the element or crystal, and gains an elemental boost.
 */
public enum Infusion {
    // Elements of the BIONICLE universe.
    FIRE(Kind.ELEMENT, "Fire", "minecraft:fire_charge", 0xFF6A1A, "flame", 0.0,
            "Immune to fire. Melee hits set foes ablaze."),
    WATER(Kind.ELEMENT, "Water", "minecraft:water_bucket", 0x2E7BFF, "wave", 0.0,
            "Breathe and swim freely underwater."),
    AIR(Kind.ELEMENT, "Air", "minecraft:wind_charge", 0xCFF5F0, "swirl", 0.0,
            "Fall gently. Melee hits blow foes away."),
    EARTH(Kind.ELEMENT, "Earth", "minecraft:rooted_dirt", 0x6B4424, "rock", 0.0,
            "Haste while mining; tougher underground."),
    STONE(Kind.ELEMENT, "Stone", "minecraft:smooth_stone", 0xA59673, "rock", 0.0,
            "Resistance and steady footing."),
    ICE(Kind.ELEMENT, "Ice", "minecraft:blue_ice", 0xA7DCFF, "frost", 0.0,
            "Immune to freezing. Melee hits chill foes."),
    LIGHTNING(Kind.ELEMENT, "Lightning", "minecraft:lightning_rod", 0xFFF36B, "spark", 0.0,
            "Speed. Melee hits jolt foes for extra damage."),
    LIGHT(Kind.ELEMENT, "Light", "minecraft:glowstone", 0xFFF8C8, "spark", 0.0,
            "Night Vision. Melee hits burn the undead."),
    SHADOW(Kind.ELEMENT, "Shadow", "minecraft:sculk", 0x2B1546, "shadow", 0.0,
            "Unseen in darkness. Melee hits drain life."),
    PLASMA(Kind.ELEMENT, "Plasma", "minecraft:magma_cream", 0xFF3D4F, "flame", 0.0,
            "Immune to fire. Melee hits sear foes."),
    SONICS(Kind.ELEMENT, "Sonics", "minecraft:note_block", 0xB48CFF, "swirl", 0.0,
            "Immune to Darkness. Melee hits stun foes."),
    GRAVITY(Kind.ELEMENT, "Gravity", "minecraft:heavy_core", 0x3B3B78, "rock", 0.0,
            "No fall damage. Melee hits pin foes down."),
    MAGNETISM(Kind.ELEMENT, "Magnetism", "minecraft:lodestone", 0x8E93A3, "rock", 0.0,
            "Pulls nearby dropped items to you."),
    IRON(Kind.ELEMENT, "Iron", "minecraft:iron_block", 0xCFCFCF, "rock", 0.0,
            "+2 armor and +2 armor toughness."),
    PLANT_LIFE(Kind.ELEMENT, "Plant Life", "minecraft:moss_block", 0x4F8F2C, "leaf", 0.0,
            "Heals slowly in sunlight. Melee hits entangle foes."),
    PSIONICS(Kind.ELEMENT, "Psionics", "minecraft:ender_eye", 0x7DE3B4, "swirl", 0.0,
            "Melee hits scramble foes' minds."),

    // Crystals.
    AMETHYST(Kind.CRYSTAL, "Amethyst", "minecraft:amethyst_shard", 0xA97CE2, "crystal", 0.25,
            "Mask power +25%."),
    QUARTZ(Kind.CRYSTAL, "Quartz", "minecraft:quartz", 0xF1E9DE, "crystal", 0.10,
            "Mask power +10% and recharges 25% faster."),
    PRISMARINE(Kind.CRYSTAL, "Prismarine", "minecraft:prismarine_crystals", 0x93E3CC, "crystal", 0.15,
            "Mask power +15%. Breathe underwater."),
    ECHO(Kind.CRYSTAL, "Echo", "minecraft:echo_shard", 0x15606F, "crystal", 0.15,
            "Mask power +15%. Immune to Darkness; powers are silent."),
    DIAMOND(Kind.CRYSTAL, "Diamond", "minecraft:diamond", 0x84F2EC, "crystal", 0.20,
            "Mask power +20% and +1 armor toughness."),
    EMERALD(Kind.CRYSTAL, "Emerald", "minecraft:emerald", 0x32D66F, "crystal", 0.10,
            "Mask power +10%. Villagers trade for less."),
    LAPIS(Kind.CRYSTAL, "Lapis", "minecraft:lapis_lazuli", 0x2C58CC, "crystal", 0.10,
            "Mask power +10%. Grants Luck."),
    END_CRYSTAL(Kind.CRYSTAL, "End Crystal", "minecraft:end_crystal", 0xE6B9F2, "crystal", 0.30,
            "Mask power +30%. Slowly regenerates health.");

    public enum Kind { ELEMENT, CRYSTAL }

    private final Kind kind;
    private final String displayName;
    private final String itemId;
    private final int color;
    private final String pattern;
    private final double potencyBonus;
    private final String effect;

    Infusion(Kind kind, String displayName, String itemId, int color, String pattern, double potencyBonus, String effect) {
        this.kind = kind;
        this.displayName = displayName;
        this.itemId = itemId;
        this.color = color;
        this.pattern = pattern;
        this.potencyBonus = potencyBonus;
        this.effect = effect;
    }

    public Kind kind() {
        return kind;
    }

    public String displayName() {
        return displayName;
    }

    /** The vanilla item that infuses this element or crystal. */
    public String itemId() {
        return itemId;
    }

    public int color() {
        return color;
    }

    /** The overlay texture style used for the upper 30% of the mask. */
    public String pattern() {
        return pattern;
    }

    public double potencyBonus() {
        return potencyBonus;
    }

    public String effect() {
        return effect;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Infusion byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }

    public static Infusion byItemId(String itemId) {
        for (Infusion i : values()) {
            if (i.itemId.equals(itemId)) {
                return i;
            }
        }
        return null;
    }

    /** Every overlay style, in a fixed order. */
    public static final String[] PATTERNS = {"flame", "wave", "swirl", "rock", "frost", "spark", "shadow", "leaf", "crystal"};
}
