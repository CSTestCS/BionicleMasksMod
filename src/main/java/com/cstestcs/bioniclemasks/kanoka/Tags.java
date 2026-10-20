package com.cstestcs.bioniclemasks.kanoka;

import com.cstestcs.bioniclemasks.BionicleMasks;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Tag keys built by name, so they do not depend on where vanilla keeps its tag constants. */
public final class Tags {
    public static final TagKey<Item> FORGE_FUEL = item(BionicleMasks.id("forge_fuel"));
    public static final TagKey<Item> KANOKA = item(BionicleMasks.id("kanoka"));
    public static final TagKey<Item> PLANKS = item(Identifier.withDefaultNamespace("planks"));
    public static final TagKey<Item> STONE_TOOL_MATERIALS = item(Identifier.withDefaultNamespace("stone_tool_materials"));
    public static final TagKey<Item> DYES = item(Identifier.withDefaultNamespace("dyes"));

    /** Block families a Reconstitutes-at-Random disk can scramble a block within. */
    public static final List<TagKey<Block>> RECONSTITUTE_FAMILIES = List.of(
            block("base_stone_overworld"), block("base_stone_nether"), block("logs"), block("planks"), block("wool"),
            block("terracotta"), block("leaves"), block("sand"), block("flowers"), block("dirt"), block("ice"),
            block("coral_blocks"), block("wart_blocks"), block("glazed_terracotta"), block("concrete"));

    private Tags() {
    }

    public static TagKey<Item> item(Identifier id) {
        return TagKey.create(Registries.ITEM, id);
    }

    public static TagKey<net.minecraft.world.entity.EntityType<?>> entity(String vanillaPath) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace(vanillaPath));
    }

    public static TagKey<Block> block(String vanillaPath) {
        return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(vanillaPath));
    }

    /** A random other member of the first family the state belongs to, or null. */
    public static Block randomFamilyMember(BlockState state, RandomSource random) {
        for (TagKey<Block> family : RECONSTITUTE_FAMILIES) {
            if (state.is(family)) {
                List<Block> members = new java.util.ArrayList<>();
                for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(family)) {
                    if (holder.value() != state.getBlock()) {
                        members.add(holder.value());
                    }
                }
                if (!members.isEmpty()) {
                    return members.get(random.nextInt(members.size()));
                }
            }
        }
        return null;
    }
}
