package com.cstestcs.bioniclemasks.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Codecs for the plain lore enums, which store themselves by lower-case id. */
public final class LoreCodecs {
    private LoreCodecs() {
    }

    public static <E extends Enum<E>> Codec<E> enumCodec(Function<String, E> byId, Function<E, String> toId) {
        return Codec.STRING.comapFlatMap(s -> {
            try {
                return DataResult.success(byId.apply(s));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Unknown value " + s);
            }
        }, toId);
    }

    public static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumStream(Function<String, E> byId, Function<E, String> toId) {
        return ByteBufCodecs.STRING_UTF8.map(byId, toId);
    }

    /** A list of enum values sent as one comma separated string. */
    public static <E extends Enum<E>> StreamCodec<ByteBuf, List<E>> enumListStream(Function<String, E> byId, Function<E, String> toId) {
        return ByteBufCodecs.STRING_UTF8.map(s -> {
            List<E> list = new ArrayList<>();
            if (!s.isEmpty()) {
                for (String part : s.split(",")) {
                    list.add(byId.apply(part));
                }
            }
            return List.copyOf(list);
        }, list -> String.join(",", list.stream().map(toId).toList()));
    }
}
