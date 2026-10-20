package com.cstestcs.bioniclemasks.forge;

import com.cstestcs.bioniclemasks.component.KanokaData;
import com.cstestcs.bioniclemasks.item.KanokaStacks;
import com.cstestcs.bioniclemasks.kanoka.Tags;
import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.DiskTrait;
import com.cstestcs.bioniclemasks.lore.KanokaPower;
import com.cstestcs.bioniclemasks.lore.Metru;
import com.cstestcs.bioniclemasks.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Kanoka Disk Forge rules. A disk is pressed from:
 * <ul>
 *     <li>a vanilla tool material (planks, cobblestone, copper, iron, gold, diamond or netherite), which
 *     sets the power level - wood 1, gold 2, stone 3, copper 4, iron 5, diamond 7, netherite 8;</li>
 *     <li>a power catalyst, which sets which of the eight powers the disk carries;</li>
 *     <li>up to two optional additives, each adding a trait or choosing the Metru the disk is made in.
 *     Without a Metru additive, the biome decides.</li>
 * </ul>
 * A netherite disk pressed with a Nether Star becomes one of the six level-9 Great Disks.
 */
public final class DiskPressing {
    public static final Map<Item, KanokaPower> POWER_CATALYSTS = Map.of(
            Items.FERMENTED_SPIDER_EYE, KanokaPower.RECONSTITUTE,
            Items.PACKED_ICE, KanokaPower.FREEZE,
            Items.ROTTEN_FLESH, KanokaPower.WEAKEN,
            Items.HONEY_BOTTLE, KanokaPower.REMOVE_POISON,
            Items.BONE_MEAL, KanokaPower.ENLARGE,
            Items.PUFFERFISH, KanokaPower.SHRINK,
            Items.GLISTERING_MELON_SLICE, KanokaPower.REGENERATE,
            Items.ENDER_PEARL, KanokaPower.TELEPORT);

    public static final Map<Item, DiskTrait> TRAIT_ADDITIVES = Map.of(
            Items.GLOWSTONE_DUST, DiskTrait.EMPOWERED,
            Items.REDSTONE, DiskTrait.LASTING,
            Items.AMETHYST_SHARD, DiskTrait.RESONANT,
            Items.QUARTZ, DiskTrait.SWIFT,
            Items.SLIME_BALL, DiskTrait.BOUNCING,
            Items.OBSIDIAN, DiskTrait.HEAVY,
            Items.BREEZE_ROD, DiskTrait.GALE,
            Items.HONEYCOMB, DiskTrait.PRESERVED,
            Items.EMERALD, DiskTrait.FORTUNATE,
            Items.ECHO_SHARD, DiskTrait.SILENT);

    public static final Map<Item, Metru> METRU_ADDITIVES = Map.of(
            Items.BLAZE_POWDER, Metru.TA,
            Items.PRISMARINE_SHARD, Metru.GA,
            Items.CLAY_BALL, Metru.PO,
            Items.SNOWBALL, Metru.KO,
            Items.FEATHER, Metru.LE,
            Items.GLOW_INK_SAC, Metru.ONU);

    private static final TagKey<Biome> IS_OCEAN = biomeTag("is_ocean");
    private static final TagKey<Biome> IS_RIVER = biomeTag("is_river");
    private static final TagKey<Biome> IS_BADLANDS = biomeTag("is_badlands");
    private static final TagKey<Biome> IS_FOREST = biomeTag("is_forest");
    private static final TagKey<Biome> IS_JUNGLE = biomeTag("is_jungle");
    private static final TagKey<Biome> IS_NETHER = biomeTag("is_nether");

    private DiskPressing() {
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, Identifier.withDefaultNamespace(path));
    }

    public static DiskMaterial material(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(Tags.PLANKS)) {
            return DiskMaterial.WOOD;
        }
        if (stack.is(Tags.STONE_TOOL_MATERIALS)) {
            return DiskMaterial.STONE;
        }
        if (stack.is(Items.COPPER_INGOT)) {
            return DiskMaterial.COPPER;
        }
        if (stack.is(Items.IRON_INGOT)) {
            return DiskMaterial.IRON;
        }
        if (stack.is(Items.GOLD_INGOT)) {
            return DiskMaterial.GOLD;
        }
        if (stack.is(Items.DIAMOND)) {
            return DiskMaterial.DIAMOND;
        }
        if (stack.is(Items.NETHERITE_INGOT)) {
            return DiskMaterial.NETHERITE;
        }
        return null;
    }

    public static boolean isAdditive(ItemStack stack) {
        return TRAIT_ADDITIVES.containsKey(stack.getItem()) || METRU_ADDITIVES.containsKey(stack.getItem()) || stack.is(Items.NETHER_STAR);
    }

    /** The Metru a disk pressed here would come from, judged by the land around the forge. */
    public static Metru metruFromSurroundings(Level level, BlockPos pos) {
        if (pos.getY() < 0) {
            return Metru.ONU; // deep underground, like the Archives
        }
        Holder<Biome> biome = level.getBiome(pos);
        if (biome.is(IS_NETHER)) {
            return Metru.TA;
        }
        if (biome.is(IS_OCEAN) || biome.is(IS_RIVER)) {
            return Metru.GA;
        }
        if (biome.is(IS_BADLANDS)) {
            return Metru.PO;
        }
        if (biome.is(IS_FOREST) || biome.is(IS_JUNGLE)) {
            return Metru.LE;
        }
        float temperature = biome.value().getBaseTemperature();
        if (temperature < 0.3F) {
            return Metru.KO;
        }
        if (temperature >= 1.0F) {
            return Metru.PO; // dry, sandy lands like Po-Metru's canyons
        }
        return Metru.TA; // Ta-Metru, where most Kanoka are forged
    }

    /** The disk these inputs would press, or empty if they do not make a disk. */
    public static ItemStack result(ItemStack materialStack, ItemStack catalyst, ItemStack additiveA, ItemStack additiveB, Level level, BlockPos pos) {
        DiskMaterial material = material(materialStack);
        KanokaPower power = POWER_CATALYSTS.get(catalyst.getItem());
        if (material == null || power == null || catalyst.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<DiskTrait> traits = new ArrayList<>();
        Metru metru = null;
        boolean netherStar = false;
        for (ItemStack additive : new ItemStack[]{additiveA, additiveB}) {
            if (additive.isEmpty()) {
                continue;
            }
            if (additive.is(Items.NETHER_STAR)) {
                netherStar = true;
                continue;
            }
            DiskTrait trait = TRAIT_ADDITIVES.get(additive.getItem());
            if (trait != null && !traits.contains(trait)) {
                traits.add(trait);
            }
            Metru m = METRU_ADDITIVES.get(additive.getItem());
            if (m != null && metru == null) {
                metru = m;
            }
        }
        if (netherStar) {
            // Only netherite, with a power one of the Great Disks held, can become a Great Disk.
            Metru greatMetru = power.greatDiskMetru();
            if (material != DiskMaterial.NETHERITE || greatMetru == null) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(ModItems.GREAT_DISKS.get(greatMetru).get());
        }
        if (metru == null) {
            metru = metruFromSurroundings(level, pos);
        }
        int powerLevel = material.level() + (traits.contains(DiskTrait.EMPOWERED) ? 1 : 0);
        powerLevel = Math.min(8, powerLevel); // only the Great Disks reach level 9
        return KanokaStacks.create(power, new KanokaData(material, powerLevel, metru, traits));
    }
}
