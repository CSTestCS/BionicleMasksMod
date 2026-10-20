package com.cstestcs.bioniclemasks.lore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every Kanohi mask. Each mask has a Kanoka forging recipe: the multiset of disk powers that must
 * be melted together at the Mask Forge (unlike the lore, every mask has one). Most masks need three
 * disks; Legendary masks need six disks of power level 8 or more plus a catalyst.
 */
public enum KanohiPower {
    // --- Toa Mata / Toa Nuva ---
    HAU("Hau", "Shielding", Kind.TOGGLE, 0, "GGF", Group.MATA,
            "Creates an invisible shield that blocks attacks the wearer knows are coming, but not surprise attacks.",
            "Toggle: blocks all damage from attackers in front of you; hits from behind still land."),
    KAUKAU("Kaukau", "Water Breathing", Kind.PASSIVE, 0, "PPG", Group.MATA,
            "Lets the wearer breathe underwater indefinitely.",
            "Passive: Water Breathing. Great and stronger masks add swim speed."),
    MIRU("Miru", "Levitation", Kind.TOGGLE, 0, "SST", Group.MATA,
            "Lets the wearer float, glide and fall safely.",
            "Toggle: hover gently, jump higher and take no fall damage."),
    KAKAMA("Kakama", "Speed", Kind.TOGGLE, 0, "TTE", Group.MATA,
            "Lets the wearer move at incredible speed.",
            "Toggle: great speed. Stronger masks run faster."),
    PAKARI("Pakari", "Strength", Kind.TOGGLE, 0, "EEG", Group.MATA,
            "Gives the wearer enormous physical strength.",
            "Toggle: Strength and Haste."),
    AKAKU("Akaku", "X-Ray Vision", Kind.TOGGLE, 0, "EER", Group.MATA,
            "Lets the wearer see through solid objects.",
            "Toggle: every creature nearby glows through walls, and you can see in the dark."),

    // --- Toa Metru (and the Noble masks of the Turaga) ---
    HUNA("Huna", "Concealment", Kind.TOGGLE, 0, "SSR", Group.METRU,
            "Makes the wearer invisible.",
            "Toggle: Invisibility."),
    RAU("Rau", "Translation", Kind.BURST, 6, "RRP", Group.METRU,
            "Lets the wearer read and understand any language or inscription.",
            "Use: understand the creature you look at (what it wants and how hurt it is). Passive: villagers understand you and trade for less."),
    MAHIKI("Mahiki", "Illusion", Kind.BURST, 20, "RRT", Group.METRU,
            "Creates illusions and lets the wearer disguise themselves.",
            "Use: vanish behind an illusion; nearby foes lose track of you."),
    KOMAU("Komau", "Mind Control", Kind.BURST, 30, "WWR", Group.METRU,
            "Controls the minds of sentient beings.",
            "Use: take over the mind of the villager, illager, piglin or other thinking being you look at; it fights your foes."),
    RURU("Ruru", "Night Vision", Kind.PASSIVE, 0, "GPE", Group.METRU,
            "Lets the wearer see in the dark.",
            "Passive: Night Vision; Great masks also block Blindness and Darkness."),
    MATATU("Matatu", "Telekinesis", Kind.BURST, 4, "TTS", Group.METRU,
            "Lets the wearer move objects with their mind.",
            "Use: lift and pull the creature or item you look at. Sneak to throw it away instead."),

    // --- Toa Inika ---
    CALIX("Calix", "Fate", Kind.TOGGLE, 0, "RTG", Group.INIKA,
            "Lets the wearer perform feats others would think impossible, such as incredible leaps and dodges.",
            "Toggle: huge leaps, speed and a chance to dodge any attack."),
    ELDA("Elda", "Detection", Kind.BURST, 10, "ETP", Group.INIKA,
            "Detects the presence of hidden beings and of other Kanohi.",
            "Use: reveal every creature nearby, even invisible ones, and sense the nearest other mask wearer."),
    SULETU("Suletu", "Telepathy", Kind.BURST, 4, "RWT", Group.INIKA,
            "Lets the wearer read thoughts and project their own.",
            "Use: read the thoughts of the creature you look at, or send your thoughts to a player."),
    SANOK("Sanok", "Accuracy", Kind.PASSIVE, 0, "EFT", Group.INIKA,
            "Gives the wearer perfect aim.",
            "Passive: everything you shoot or throw flies perfectly straight and arrows always crit."),
    KADIN("Kadin", "Flight", Kind.TOGGLE, 0, "SSE", Group.INIKA,
            "Lets the wearer fly.",
            "Toggle: fly freely. Drains hunger while airborne."),
    IDEN("Iden", "Spirit", Kind.TOGGLE, 0, "TTR", Group.INIKA,
            "Lets the wearer's spirit leave their body for a while.",
            "Toggle: become a spirit - invisible, able to fly, ignored by mobs and unable to touch anything."),

    // --- Toa Mahri ---
    ARTHRON("Arthron", "Sonar", Kind.BURST, 8, "EET", Group.MAHRI,
            "Lets the wearer sense their surroundings with sonar.",
            "Use: a sonar ping marks every creature around you; it reaches twice as far underwater."),
    FAXON("Faxon", "Kindred", Kind.BURST, 20, "RGP", Group.MAHRI,
            "Lets the wearer use the powers of any creature or Rahi nearby.",
            "Use: copy the natural ability of the nearest creature (a fish's gills, a blaze's fire immunity, a rabbit's leap...)."),
    ZATTH("Zatth", "Summoning", Kind.BURST, 45, "TGG", Group.MAHRI,
            "Summons Rahi to the wearer's aid. It summons, but does not control.",
            "Use: summon wolves that fight beside you for a minute."),
    GARAI("Garai", "Gravity", Kind.BURST, 8, "EEW", Group.MAHRI,
            "Lets the wearer increase or decrease gravity in an area.",
            "Use: crush nearby foes under heavy gravity. Sneak-use: lighten your own gravity for a while."),
    VOLITAK("Volitak", "Stealth", Kind.TOGGLE, 0, "SRF", Group.MAHRI,
            "Camouflages the wearer and deadens the sound around them.",
            "Toggle: invisible, silent, and mobs struggle to notice you."),

    // --- Toa Hagah ---
    PEHKUI("Pehkui", "Diminishment", Kind.TOGGLE, 0, "SSS", Group.HAGAH,
            "Shrinks the wearer while keeping their full strength.",
            "Toggle: shrink to a fraction of your size with no loss of strength."),
    KUALSI("Kualsi", "Quick Travel", Kind.BURST, 3, "TTT", Group.HAGAH,
            "Teleports the wearer to any place they can see.",
            "Use: teleport to the spot you are looking at."),
    CLAIRVOYANCE("Clairvoyance", "Clairvoyance", Kind.BURST, 30, "TPR", Group.HAGAH,
            "Gives glimpses of the near future. The wearer cannot control what or when they see.",
            "Use: see the coming weather, nightfall and which foes will come for you. Visions also arrive on their own."),
    EMULATION("Emulation", "Emulation", Kind.BURST, 10, "RRR", Group.HAGAH,
            "Lets the wearer mimic any voice or sound.",
            "Use: throw the call of the creature you look at to where you look; nearby creatures go to investigate."),
    GROWTH("Growth", "Growth", Kind.TOGGLE, 0, "EEE", Group.HAGAH,
            "Lets the wearer grow to giant size, with strength to match - though not a Pakari's.",
            "Toggle: grow huge, gaining reach and some strength."),
    RAHI_CONTROL("Rahi Control", "Rahi Control", Kind.BURST, 20, "WWG", Group.HAGAH,
            "Lets the wearer control Rahi beasts.",
            "Use: tame or control the animal or monster you look at."),

    // --- Other Great Kanohi ---
    AVOHKII("Avohkii", "Light", Kind.BURST, 5, "GGE", Group.OTHER,
            "Creates beams of light and brings understanding to others.",
            "Use: fire a beam of light that sears the undead. Passive: immune to Blindness and Darkness."),
    KRAAHKAN("Kraahkan", "Shadows", Kind.BURST, 20, "WWW", Group.OTHER,
            "Creates darkness, anger and fear. It drains any wearer who is not a Makuta.",
            "Use: a wave of shadow weakens and blinds everything around you - and saps you too."),
    AVSA("Avsa", "Hunger", Kind.BURST, 6, "WWT", Group.OTHER,
            "Absorbs light and energy from a target at a distance.",
            "Use: drain life and energy from the creature you look at."),
    CRAST("Crast", "Repulsion", Kind.BURST, 4, "TWE", Group.OTHER,
            "Hurls objects and beings away from the wearer at high speed, one target at a time.",
            "Use: hurl the creature you look at away from you."),
    FELNAS("Felnas", "Disruption", Kind.BURST, 15, "WPR", Group.OTHER,
            "Disrupts the powers of another being.",
            "Use: strip the powers and helpful effects from the creature you look at; a mask wearer's mask goes dark."),
    JUTLIN("Jutlin", "Corruption", Kind.BURST, 10, "WWS", Group.OTHER,
            "Causes objects to break down and rot.",
            "Use: rot the block or creature you look at; a creature's armor corrodes."),
    KIRIL("Kiril", "Regeneration", Kind.BURST, 30, "GGG", Group.OTHER,
            "Restores damaged inanimate objects to their undamaged state.",
            "Use: mend your gear and restore cracked or damaged blocks around you."),
    MOHTREK("Mohtrek", "Time Duplication", Kind.BURST, 40, "TTG", Group.OTHER,
            "Summons past versions of the wearer into the present.",
            "Use: for a while, echoes of your past self strike alongside every blow you land."),
    OLISI("Olisi", "Alternate Futures", Kind.BURST, 15, "RRS", Group.OTHER,
            "Projects visions of possible futures into a target's mind.",
            "Use: trap the creature you look at in visions; it wanders, confused and harmless."),
    OLMAK("Olmak", "Dimensional Gates", Kind.BURST, 30, "TTW", Group.OTHER,
            "Opens gates to other dimensions and places.",
            "Use: open a gate to the distant spot you look at, taking nearby allies. Sneak-use: open a gate between the Overworld and the Nether."),
    RODE("Rode", "Truth", Kind.BURST, 8, "PPR", Group.OTHER,
            "Lets the wearer see through lies and illusions.",
            "Use: reveal the true nature of everything nearby. Passive: immune to Nausea and Blindness."),
    SHELEK("Shelek", "Silence", Kind.BURST, 20, "FFW", Group.OTHER,
            "Robs others of the ability to hear.",
            "Use: silence everything around you; deafened foes lose track of you."),
    TRYNA("Tryna", "Reanimation", Kind.BURST, 45, "GGR", Group.OTHER,
            "Reanimates the dead, who obey the wearer while the mask is worn.",
            "Use: raise undead servants that fight for you for a minute."),

    // --- Unnamed Great Kanohi (known only by their powers) ---
    ADAPTATION("Adaptation", "Adaptation", Kind.PASSIVE, 0, "PFG", Group.UNNAMED,
            "Adapts the wearer's body to survive any environment.",
            "Passive: breathe underwater, resist fire in lava, and shrug off freezing when you need to."),
    CHARISMA("Charisma", "Charisma", Kind.BURST, 20, "RWE", Group.UNNAMED,
            "Slowly shifts a target's perception until they agree with the wearer.",
            "Use: the creature you look at comes round to your side and stops attacking you."),
    CONJURING("Conjuring", "Conjuring", Kind.BURST, 8, "REG", Group.UNNAMED,
            "Conjures objects out of nothing.",
            "Use: conjure a temporary wall where you look. Sneak-use: conjure a platform under you."),
    ELEMENTAL_ENERGY("Elemental Energy", "Elemental Energy", Kind.TOGGLE, 0, "EFG", Group.UNNAMED,
            "Boosts the wearer's own elemental power.",
            "Toggle: doubles the mask's element or crystal boost, plus Strength and Resistance."),
    FUSION("Fusion", "Fusion", Kind.BURST, 40, "EST", Group.UNNAMED,
            "Fuses the wearer with another being, combining their power.",
            "Use: fuse with the ally you look at - you both gain strength, toughness and its health."),
    INCOMPREHENSION("Incomprehension", "Incomprehension", Kind.BURST, 20, "WRS", Group.UNNAMED,
            "Leaves others unable to understand or communicate.",
            "Use: nearby foes forget what they were doing and cannot pick a target."),
    INTANGIBILITY("Intangibility", "Intangibility", Kind.TOGGLE, 0, "STR", Group.UNNAMED,
            "Makes the wearer intangible.",
            "Toggle: attacks pass straight through you, but you cannot strike back."),
    POSSIBILITIES("Possibilities", "Possibilities", Kind.TOGGLE, 0, "RRE", Group.UNNAMED,
            "Lets the wearer bend the odds of what might happen.",
            "Toggle: great Luck - better loot, fishing and fortune."),
    PSYCHOMETRY("Psychometry", "Psychometry", Kind.BURST, 4, "PTS", Group.UNNAMED,
            "Reads the history of an object or being by touch.",
            "Use: learn the history of the creature or container you look at."),
    SENSORY_APTITUDE("Sensory Aptitude", "Sensory Aptitude", Kind.TOGGLE, 0, "PES", Group.UNNAMED,
            "Sharpens all of the wearer's senses.",
            "Toggle: see in the dark and sense every creature close by."),
    SONICS("Sonics", "Sonics", Kind.BURST, 8, "FWT", Group.UNNAMED,
            "Unleashes powerful sound.",
            "Use: a sonic blast that damages and knocks back everything in front of you and shatters glass."),

    // --- Legendary Kanohi ---
    VAHI("Vahi", "Time", Kind.BURST, 40, "", Group.LEGENDARY,
            "Controls the flow of time. Forged by Vakama from the Disk of Time, itself made from the six Great Disks.",
            "Use: slow time to a crawl for everything around you. Sneak-use: speed up your own time."),
    IGNIKA("Ignika", "Life", Kind.BURST, 60, "GGGGGG", Group.LEGENDARY,
            "The Mask of Life, which can give and take life.",
            "Use: restore the life of all allies nearby. Sneak-use: drain the life from the creature you look at. Passive: saves you from death once per recharge."),
    CREATION("Creation", "Creation", Kind.BURST, 30, "RFPEGT", Group.LEGENDARY,
            "Artakha's mask, which can create anything the wearer imagines.",
            "Use: bring life to the land around you - plants grow, grass spreads and animals appear.");

    public enum Kind { PASSIVE, TOGGLE, BURST }

    public enum Group { MATA, METRU, INIKA, MAHRI, HAGAH, OTHER, UNNAMED, LEGENDARY }

    /** The six masks of the Toa Mata, which also exist as Kanohi Nuva and Golden Kanohi. */
    public static final List<KanohiPower> MATA_MASKS = List.of(HAU, KAUKAU, MIRU, KAKAMA, PAKARI, AKAKU);

    private final String maskName;
    private final String powerName;
    private final Kind kind;
    private final int cooldownSeconds;
    private final String recipe;
    private final Group group;
    private final String lore;
    private final String gameplay;

    KanohiPower(String maskName, String powerName, Kind kind, int cooldownSeconds, String recipe, Group group,
                String lore, String gameplay) {
        this.maskName = maskName;
        this.powerName = powerName;
        this.kind = kind;
        this.cooldownSeconds = cooldownSeconds;
        this.recipe = recipe;
        this.group = group;
        this.lore = lore;
        this.gameplay = gameplay;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The registry name of this mask's item. */
    public String itemName() {
        return "kanohi_" + id();
    }

    /** True for masks that have a name in the Matoran language ("Kanohi Hau"). */
    public boolean named() {
        return group != Group.UNNAMED && this != CLAIRVOYANCE && this != EMULATION && this != GROWTH
                && this != RAHI_CONTROL && this != CREATION;
    }

    /** "Kanohi Hau" or "Mask of Clairvoyance". */
    public String displayName() {
        return named() ? "Kanohi " + maskName : "Mask of " + maskName;
    }

    /** "Mask of Shielding". */
    public String title() {
        return "Mask of " + powerName;
    }

    public String maskName() {
        return maskName;
    }

    public String powerName() {
        return powerName;
    }

    public Kind kind() {
        return kind;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public Group group() {
        return group;
    }

    public String lore() {
        return lore;
    }

    public String gameplay() {
        return gameplay;
    }

    public boolean legendary() {
        return group == Group.LEGENDARY;
    }

    public boolean mata() {
        return group == Group.MATA;
    }

    /** The disk powers that must be forged together, or an empty list for the Vahi (forged from the Disk of Time). */
    public List<KanokaPower> recipe() {
        List<KanokaPower> list = new ArrayList<>();
        for (char c : recipe.toCharArray()) {
            list.add(KanokaPower.fromLetter(c));
        }
        return list;
    }

    /** The forms this mask can be forged in. */
    public List<KanohiForm> forms() {
        if (legendary()) {
            return List.of(KanohiForm.POWERLESS, KanohiForm.LEGENDARY);
        }
        if (mata()) {
            return List.of(KanohiForm.POWERLESS, KanohiForm.NOBLE, KanohiForm.GREAT, KanohiForm.NUVA, KanohiForm.GOLDEN);
        }
        return List.of(KanohiForm.POWERLESS, KanohiForm.NOBLE, KanohiForm.GREAT);
    }

    /** A sorted key for a multiset of disk powers. */
    public static String key(List<KanokaPower> powers) {
        char[] chars = new char[powers.size()];
        for (int i = 0; i < powers.size(); i++) {
            chars[i] = powers.get(i).letter();
        }
        Arrays.sort(chars);
        return new String(chars);
    }

    private static final Map<String, KanohiPower> BY_RECIPE = new HashMap<>();

    static {
        for (KanohiPower power : values()) {
            if (power.recipe.isEmpty()) {
                continue;
            }
            KanohiPower previous = BY_RECIPE.put(key(power.recipe()), power);
            if (previous != null) {
                throw new IllegalStateException("Kanohi " + power + " and " + previous + " share a recipe");
            }
        }
    }

    /** The mask whose recipe is exactly this multiset of disk powers, or null. */
    public static KanohiPower byRecipe(List<KanokaPower> powers) {
        return BY_RECIPE.get(key(powers));
    }

    /** How many masks of each group exist. */
    public static Map<Group, Integer> countByGroup() {
        Map<Group, Integer> counts = new EnumMap<>(Group.class);
        for (KanohiPower p : values()) {
            counts.merge(p.group, 1, Integer::sum);
        }
        return counts;
    }

    public static KanohiPower byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
