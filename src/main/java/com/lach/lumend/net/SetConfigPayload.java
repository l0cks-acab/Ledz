package com.lach.lumend.net;

import com.lach.lumend.LedConfig;
import com.lach.lumend.Lumend;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetConfigPayload(boolean offhand, LedConfig config) implements CustomPacketPayload {
    public static final Type<SetConfigPayload> TYPE = new Type<>(Lumend.id("set_config"));
    public static final StreamCodec<ByteBuf, SetConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetConfigPayload::offhand,
            LedConfig.STREAM_CODEC, SetConfigPayload::config,
            SetConfigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
