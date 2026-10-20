package com.cstestcs.bioniclemasks.component;

import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.DiskTrait;
import com.cstestcs.bioniclemasks.lore.Infusion;
import com.cstestcs.bioniclemasks.lore.KanohiForm;
import com.cstestcs.bioniclemasks.lore.Metru;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The data on a Kanohi: its form, material, element or crystal infusion, inherited disk traits, the
 * Metru its disks came from, and (for Golden Kanohi) which of the six Mata powers is selected.
 */
public record KanohiData(KanohiForm form, DiskMaterial material, Optional<Infusion> infusion, List<DiskTrait> traits,
                         Metru origin, int selected) {
    public static final Codec<KanohiForm> FORM_CODEC = LoreCodecs.enumCodec(KanohiForm::byId, KanohiForm::id);
    public static final Codec<Infusion> INFUSION_CODEC = LoreCodecs.enumCodec(Infusion::byId, Infusion::id);

    public static final Codec<KanohiData> CODEC = RecordCodecBuilder.create(i -> i.group(
            FORM_CODEC.fieldOf("form").forGetter(KanohiData::form),
            KanokaData.MATERIAL_CODEC.fieldOf("material").forGetter(KanohiData::material),
            INFUSION_CODEC.optionalFieldOf("infusion").forGetter(KanohiData::infusion),
            KanokaData.TRAIT_CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(KanohiData::traits),
            KanokaData.METRU_CODEC.optionalFieldOf("origin", Metru.TA).forGetter(KanohiData::origin),
            Codec.INT.optionalFieldOf("selected", 0).forGetter(KanohiData::selected)
    ).apply(i, KanohiData::new));

    private static final StreamCodec<ByteBuf, Optional<Infusion>> INFUSION_STREAM = ByteBufCodecs.STRING_UTF8.map(
            s -> s.isEmpty() ? Optional.empty() : Optional.of(Infusion.byId(s)),
            o -> o.map(Infusion::id).orElse(""));

    public static final StreamCodec<ByteBuf, KanohiData> STREAM_CODEC = StreamCodec.composite(
            LoreCodecs.enumStream(KanohiForm::byId, KanohiForm::id), KanohiData::form,
            LoreCodecs.enumStream(DiskMaterial::byId, DiskMaterial::id), KanohiData::material,
            INFUSION_STREAM, KanohiData::infusion,
            LoreCodecs.enumListStream(DiskTrait::byId, DiskTrait::id), KanohiData::traits,
            LoreCodecs.enumStream(Metru::byId, Metru::id), KanohiData::origin,
            ByteBufCodecs.VAR_INT, KanohiData::selected,
            KanohiData::new);

    public static final KanohiData DEFAULT = new KanohiData(KanohiForm.POWERLESS, DiskMaterial.IRON, Optional.empty(), List.of(), Metru.TA, 0);

    public KanohiData {
        traits = List.copyOf(traits);
    }

    public boolean has(DiskTrait trait) {
        return traits.contains(trait);
    }

    public boolean has(Infusion i) {
        return infusion.isPresent() && infusion.get() == i;
    }

    public KanohiData withInfusion(Infusion i) {
        return new KanohiData(form, material, Optional.of(i), traits, origin, selected);
    }

    public KanohiData withForm(KanohiForm f) {
        return new KanohiData(f, material, infusion, traits, origin, selected);
    }

    public KanohiData withSelected(int s) {
        return new KanohiData(form, material, infusion, traits, origin, s);
    }
}
