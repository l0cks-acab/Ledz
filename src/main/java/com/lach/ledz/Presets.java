package com.lach.ledz;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Saved looks on an LED Configurator, slot 0-7 to config. */
public record Presets(Map<Integer, LedConfig> slots) {
    public static final int SLOTS = 8;
    public static final Presets EMPTY = new Presets(Map.of());

    public static final Codec<Presets> CODEC = Codec.unboundedMap(Codec.STRING, LedConfig.CODEC).xmap(
            m -> {
                Map<Integer, LedConfig> out = new HashMap<>();
                m.forEach((k, v) -> {
                    try {
                        int i = Integer.parseInt(k);
                        if (i >= 0 && i < SLOTS) out.put(i, v);
                    } catch (NumberFormatException ignored) {}
                });
                return new Presets(Map.copyOf(out));
            },
            p -> {
                Map<String, LedConfig> out = new HashMap<>();
                p.slots.forEach((k, v) -> out.put(Integer.toString(k), v));
                return out;
            });

    public static final StreamCodec<ByteBuf, Presets> STREAM_CODEC = ByteBufCodecs.<ByteBuf, Integer, LedConfig, Map<Integer, LedConfig>>map(
            HashMap::new, ByteBufCodecs.VAR_INT, LedConfig.STREAM_CODEC, SLOTS).map(m -> new Presets(Map.copyOf(m)), Presets::slots);

    public LedConfig get(int slot) { return slots.get(slot); }

    public Presets with(int slot, LedConfig cfg) {
        if (slot < 0 || slot >= SLOTS) return this;
        Map<Integer, LedConfig> m = new HashMap<>(slots);
        if (cfg == null) m.remove(slot);
        else m.put(slot, cfg.sanitized());
        return new Presets(Map.copyOf(m));
    }
}
