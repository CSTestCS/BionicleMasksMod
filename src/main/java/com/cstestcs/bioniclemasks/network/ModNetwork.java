package com.cstestcs.bioniclemasks.network;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.mask.MaskPowers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Client-to-server messages: the mask keys and the Mask Forge's "Forge" button. */
public final class ModNetwork {
    private ModNetwork() {
    }

    /** A mask key was pressed. Action 0 = use power, 1 = use power while sneaking (alternate), 2 = cycle Golden Kanohi. */
    public record MaskKey(int action) implements CustomPacketPayload {
        public static final Type<MaskKey> TYPE = new Type<>(BionicleMasks.id("mask_key"));
        public static final StreamCodec<ByteBuf, MaskKey> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(MaskKey::new, MaskKey::action);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** A button in an open forge screen was pressed. */
    public record ForgeButton(int containerId, int button) implements CustomPacketPayload {
        public static final Type<ForgeButton> TYPE = new Type<>(BionicleMasks.id("forge_button"));
        public static final StreamCodec<ByteBuf, ForgeButton> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ForgeButton::containerId,
                ByteBufCodecs.VAR_INT, ForgeButton::button,
                ForgeButton::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(MaskKey.TYPE, MaskKey.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.action() == 2) {
                    MaskPowers.cycle(player);
                } else {
                    MaskPowers.use(player);
                }
            }
        });
        registrar.playToServer(ForgeButton.TYPE, ForgeButton.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu.containerId == payload.containerId()) {
                player.containerMenu.clickMenuButton(player, payload.button());
                player.containerMenu.broadcastChanges();
            }
        });
    }
}
