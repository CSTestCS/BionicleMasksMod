package com.cstestcs.bioniclemasks.component;

import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.DiskTrait;
import com.cstestcs.bioniclemasks.lore.Metru;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The data on a Kanoka disk. The power comes from the item itself; this records what it was pressed
 * from (material), its power level (1-9), where it was made (Metru) and any extra traits.
 */
public record KanokaData(DiskMaterial material, int level, Metru metru, List<DiskTrait> traits) {
    public static final Codec<DiskMaterial> MATERIAL_CODEC = LoreCodecs.enumCodec(DiskMaterial::byId, DiskMaterial::id);
    public static final Codec<Metru> METRU_CODEC = LoreCodecs.enumCodec(Metru::byId, Metru::id);
    public static final Codec<DiskTrait> TRAIT_CODEC = LoreCodecs.enumCodec(DiskTrait::byId, DiskTrait::id);

    public static final Codec<KanokaData> CODEC = RecordCodecBuilder.create(i -> i.group(
            MATERIAL_CODEC.fieldOf("material").forGetter(KanokaData::material),
            Codec.intRange(1, 9).fieldOf("level").forGetter(KanokaData::level),
            METRU_CODEC.fieldOf("metru").forGetter(KanokaData::metru),
            TRAIT_CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(KanokaData::traits)
    ).apply(i, KanokaData::new));

    public static final StreamCodec<ByteBuf, KanokaData> STREAM_CODEC = StreamCodec.composite(
            LoreCodecs.enumStream(DiskMaterial::byId, DiskMaterial::id), KanokaData::material,
            ByteBufCodecs.VAR_INT, KanokaData::level,
            LoreCodecs.enumStream(Metru::byId, Metru::id), KanokaData::metru,
            LoreCodecs.enumListStream(DiskTrait::byId, DiskTrait::id), KanokaData::traits,
            KanokaData::new);

    public static final KanokaData DEFAULT = new KanokaData(DiskMaterial.IRON, DiskMaterial.IRON.level(), Metru.TA, List.of());

    public KanokaData {
        traits = List.copyOf(traits);
    }

    public boolean has(DiskTrait trait) {
        return traits.contains(trait);
    }
}
