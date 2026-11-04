package com.lach.lumend;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * Strip look: 2-4 colour stops, animation mode, speed, brightness, direction, light level and redstone behaviour.
 */
public record LedConfig(List<Integer> stops, int mode, int speed, int brightness, boolean reverse, int light, int redstone, int wash) {
    public static final int STATIC = 0, SCROLL = 1, PULSE = 2, RAINBOW = 3, MODES = 4;
    public static final int RS_IGNORE = 0, RS_ON = 1, RS_OFF = 2, RS_DIM = 3, RS_MODES = 4;
    public static final int MIN_STOPS = 2, MAX_STOPS = 4;

    public static final LedConfig STRIP_DEFAULT = new LedConfig(List.of(0xFFF4E0, 0xFFF4E0), STATIC, 5, 100, false, 0, RS_IGNORE, 0);
    public static final LedConfig CONFIGURATOR_DEFAULT = new LedConfig(List.of(0x1E5BFF, 0x19E6A0), STATIC, 5, 100, false, 0, RS_IGNORE, 0);

    public static final Codec<LedConfig> CODEC = RecordCodecBuilder.<LedConfig>create(i -> i.group(
            Codec.INT.listOf().fieldOf("stops").forGetter(LedConfig::stops),
            Codec.INT.optionalFieldOf("mode", STATIC).forGetter(LedConfig::mode),
            Codec.INT.optionalFieldOf("speed", 5).forGetter(LedConfig::speed),
            Codec.INT.optionalFieldOf("brightness", 100).forGetter(LedConfig::brightness),
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(LedConfig::reverse),
            Codec.INT.optionalFieldOf("light", 0).forGetter(LedConfig::light),
            Codec.INT.optionalFieldOf("redstone", RS_IGNORE).forGetter(LedConfig::redstone),
            Codec.INT.optionalFieldOf("wash", 0).forGetter(LedConfig::wash)
    ).apply(i, LedConfig::new)).xmap(LedConfig::sanitized, c -> c);

    public static final StreamCodec<ByteBuf, LedConfig> STREAM_CODEC = StreamCodec.of(
            (buf, c) -> {
                ByteBufCodecs.VAR_INT.encode(buf, c.stops.size());
                for (int s : c.stops) ByteBufCodecs.INT.encode(buf, s);
                buf.writeByte(c.mode);
                buf.writeByte(c.speed);
                buf.writeByte(c.brightness);
                buf.writeBoolean(c.reverse);
                buf.writeByte(c.light);
                buf.writeByte(c.redstone);
                buf.writeByte(c.wash);
            },
            buf -> {
                int n = Math.min(ByteBufCodecs.VAR_INT.decode(buf), MAX_STOPS);
                List<Integer> stops = new ArrayList<>();
                for (int k = 0; k < n; k++) stops.add(ByteBufCodecs.INT.decode(buf));
                return new LedConfig(stops, buf.readByte(), buf.readByte(), buf.readByte(), buf.readBoolean(), buf.readByte(), buf.readByte(), buf.readByte()).sanitized();
            });

    public LedConfig sanitized() {
        List<Integer> s = new ArrayList<>();
        for (int c : stops) if (s.size() < MAX_STOPS) s.add(c & 0xFFFFFF);
        while (s.size() < MIN_STOPS) s.add(s.isEmpty() ? 0xFFFFFF : s.get(s.size() - 1));
        return new LedConfig(List.copyOf(s), Mth.clamp(mode, 0, MODES - 1), Mth.clamp(speed, 1, 20),
                Mth.clamp(brightness, 0, 100), reverse, Mth.clamp(light, 0, 15), Mth.clamp(redstone, 0, RS_MODES - 1), Mth.clamp(wash, 0, 100));
    }

    public LedConfig withStops(List<Integer> s) { return new LedConfig(s, mode, speed, brightness, reverse, light, redstone, wash); }
    public LedConfig withMode(int v) { return new LedConfig(stops, v, speed, brightness, reverse, light, redstone, wash); }
    public LedConfig withSpeed(int v) { return new LedConfig(stops, mode, v, brightness, reverse, light, redstone, wash); }
    public LedConfig withBrightness(int v) { return new LedConfig(stops, mode, speed, v, reverse, light, redstone, wash); }
    public LedConfig withReverse(boolean v) { return new LedConfig(stops, mode, speed, brightness, v, light, redstone, wash); }
    public LedConfig withLight(int v) { return new LedConfig(stops, mode, speed, brightness, reverse, v, redstone, wash); }
    public LedConfig withRedstone(int v) { return new LedConfig(stops, mode, speed, brightness, reverse, light, v, wash); }

    public LedConfig withWash(int v) { return new LedConfig(stops, mode, speed, brightness, reverse, light, redstone, v); }

    /** 0..1 output factor for a redstone signal. */
    public float redstoneFactor(int power) {
        return switch (redstone) {
            case RS_ON -> power > 0 ? 1 : 0;
            case RS_OFF -> power > 0 ? 0 : 1;
            case RS_DIM -> power / 15f;
            default -> 1;
        };
    }

    /** Sample the static gradient, t in 0..1. */
    public int gradient(double t) {
        t = Mth.clamp(t, 0, 1);
        int n = stops.size();
        double p = t * (n - 1);
        int a = Math.min((int) Math.floor(p), n - 2);
        return lerp(stops.get(a), stops.get(a + 1), p - a);
    }

    /** Animated colour (RGB) at run position t and time in seconds, before brightness/redstone. */
    public int color(double t, double seconds) {
        if (reverse) t = 1 - t;
        double cps = speed * 0.05;
        return switch (mode) {
            case SCROLL -> {
                double u = frac(t - seconds * cps);
                yield gradient(u < 0.5 ? u * 2 : 2 - u * 2);
            }
            case PULSE -> scale(gradient(t), (float) (0.25 + 0.75 * (0.5 + 0.5 * Math.sin(seconds * cps * Math.PI * 2))));
            case RAINBOW -> Mth.hsvToRgb((float) frac(t + seconds * cps), 0.9f, 1f) & 0xFFFFFF;
            default -> gradient(t);
        };
    }

    /** Light level the strip should emit for a redstone signal. */
    public int lightFor(int power) {
        return Math.round(light * redstoneFactor(power) * Math.max(brightness, 25) / 100f);
    }

    public static double frac(double v) { return v - Math.floor(v); }

    public static int lerp(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static int scale(int c, float f) {
        f = Mth.clamp(f, 0, 1);
        return ((int) (((c >> 16) & 255) * f) << 16) | ((int) (((c >> 8) & 255) * f) << 8) | (int) ((c & 255) * f);
    }
}
