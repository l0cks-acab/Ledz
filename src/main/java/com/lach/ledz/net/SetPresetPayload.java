package com.lach.ledz.net;

import com.lach.ledz.LedConfig;
import com.lach.ledz.Ledz;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Save (or clear, when empty) a preset slot on the held configurator. */
public record SetPresetPayload(boolean offhand, int slot, Optional<LedConfig> config) implements CustomPacketPayload {
    public static final Type<SetPresetPayload> TYPE = new Type<>(Ledz.id("set_preset"));
    public static final StreamCodec<ByteBuf, SetPresetPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetPresetPayload::offhand,
            ByteBufCodecs.VAR_INT, SetPresetPayload::slot,
            ByteBufCodecs.optional(LedConfig.STREAM_CODEC), SetPresetPayload::config,
            SetPresetPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
