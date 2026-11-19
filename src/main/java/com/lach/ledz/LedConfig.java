package com.lach.ledz;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * Light look: 2-4 colour stops, animation mode, speed, brightness, direction, light level, redstone behaviour,
 * colour wash and Light Controller channel (-1 = none, 0-15 = dye colour).
 */
public record LedConfig(List<Integer> stops, int mode, int speed, int brightness, boolean reverse, int light, int redstone, int wash, int channel) {
    public static final int STATIC = 0, SCROLL = 1, PULSE = 2, RAINBOW = 3, CHASE = 4, SPARKLE = 5, STROBE = 6, BREATHE = 7, MODES = 8;
    public static final int NO_CHANNEL = -1, CHANNELS = 16;
    public static final int RS_IGNORE = 0, RS_ON = 1, RS_OFF = 2, RS_DIM = 3, RS_MODES = 4;
    public static final int MIN_STOPS = 2, MAX_STOPS = 4;

    public static final LedConfig STRIP_DEFAULT = new LedConfig(List.of(0xFFF4E0, 0xFFF4E0), STATIC, 5, 100, false, 0, RS_IGNORE, 0, NO_CHANNEL);
    public static final LedConfig LAMP_DEFAULT = new LedConfig(List.of(0xFFF4E0, 0xFFF4E0), STATIC, 5, 100, false, 12, RS_IGNORE, 40, NO_CHANNEL);
    public static final LedConfig CONFIGURATOR_DEFAULT = new LedConfig(List.of(0x1E5BFF, 0x19E6A0), STATIC, 5, 100, false, 0, RS_IGNORE, 0, NO_CHANNEL);

    public static final Codec<LedConfig> CODEC = RecordCodecBuilder.<LedConfig>create(i -> i.group(
            Codec.INT.listOf().fieldOf("stops").forGetter(LedConfig::stops),
            Codec.INT.optionalFieldOf("mode", STATIC).forGetter(LedConfig::mode),
            Codec.INT.optionalFieldOf("speed", 5).forGetter(LedConfig::speed),
            Codec.INT.optionalFieldOf("brightness", 100).forGetter(LedConfig::brightness),
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(LedConfig::reverse),
            Codec.INT.optionalFieldOf("light", 0).forGetter(LedConfig::light),
            Codec.INT.optionalFieldOf("redstone", RS_IGNORE).forGetter(LedConfig::redstone),
            Codec.INT.optionalFieldOf("wash", 0).forGetter(LedConfig::wash),
            Codec.INT.optionalFieldOf("channel", NO_CHANNEL).forGetter(LedConfig::channel)
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
                buf.writeByte(c.channel);
            },
            buf -> {
                int n = Math.min(ByteBufCodecs.VAR_INT.decode(buf), MAX_STOPS);
                List<Integer> stops = new ArrayList<>();
                for (int k = 0; k < n; k++) stops.add(ByteBufCodecs.INT.decode(buf));
                return new LedConfig(stops, buf.readByte(), buf.readByte(), buf.readByte(), buf.readBoolean(), buf.readByte(), buf.readByte(), buf.readByte(), buf.readByte()).sanitized();
            });

    public LedConfig sanitized() {
        List<Integer> s = new ArrayList<>();
        for (int c : stops) if (s.size() < MAX_STOPS) s.add(c & 0xFFFFFF);
        while (s.size() < MIN_STOPS) s.add(s.isEmpty() ? 0xFFFFFF : s.get(s.size() - 1));
        return new LedConfig(List.copyOf(s), Mth.clamp(mode, 0, MODES - 1), Mth.clamp(speed, 1, 20),
                Mth.clamp(brightness, 0, 100), reverse, Mth.clamp(light, 0, 15), Mth.clamp(redstone, 0, RS_MODES - 1), Mth.clamp(wash, 0, 100), Mth.clamp(channel, NO_CHANNEL, CHANNELS - 1));
    }

    public LedConfig withStops(List<Integer> s) { return new LedConfig(s, mode, speed, brightness, reverse, light, redstone, wash, channel); }
    public LedConfig withMode(int v) { return new LedConfig(stops, v, speed, brightness, reverse, light, redstone, wash, channel); }
    public LedConfig withSpeed(int v) { return new LedConfig(stops, mode, v, brightness, reverse, light, redstone, wash, channel); }
    public LedConfig withBrightness(int v) { return new LedConfig(stops, mode, speed, v, reverse, light, redstone, wash, channel); }
    public LedConfig withReverse(boolean v) { return new LedConfig(stops, mode, speed, brightness, v, light, redstone, wash, channel); }
    public LedConfig withLight(int v) { return new LedConfig(stops, mode, speed, brightness, reverse, v, redstone, wash, channel); }
    public LedConfig withRedstone(int v) { return new LedConfig(stops, mode, speed, brightness, reverse, light, v, wash, channel); }

    public LedConfig withWash(int v) { return new LedConfig(stops, mode, speed, brightness, reverse, light, redstone, v, channel); }

    public LedConfig withChannel(int v) { return new LedConfig(stops, mode, speed, brightness, reverse, light, redstone, wash, v); }

    public static net.minecraft.network.chat.Component channelName(int ch) {
        return ch < 0 ? net.minecraft.network.chat.Component.translatable("gui.ledz.channel.none")
                : net.minecraft.network.chat.Component.translatable("color.minecraft." + net.minecraft.world.item.DyeColor.byId(ch).getName());
    }

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
            case CHASE -> {
                // three comets per run, each with a fading tail
                double d = frac((seconds * cps * 1.5 - t) * 3);
                float tail = d < 0.45 ? (float) Math.pow(1 - d / 0.45, 2) : 0;
                yield scale(gradient(t), 0.08f + 0.92f * tail);
            }
            case SPARKLE -> {
                // dim base, random points flare up and fade
                int cell = (int) Math.floor(t * 40);
                long h = hash(cell);
                double rate = 0.6 + (h & 255) / 255.0 * 0.8;
                double phase = ((h >> 8) & 1023) / 1023.0;
                double s = Math.max(0, Math.sin((seconds * cps * 4 * rate + phase) * Math.PI * 2));
                float k = (float) Math.pow(s, 12);
                yield lerp(scale(gradient(t), 0.3f), lerp(gradient(t), 0xFFFFFF, 0.5), k);
            }
            case STROBE -> frac(seconds * (1 + speed * 0.5)) < 0.3 ? gradient(t) : 0;
            case BREATHE -> {
                // whole run fades through the stops in turn
                int n = stops.size();
                double p = frac(seconds * cps * 0.5) * n;
                int a = (int) Math.floor(p) % n;
                double u = p - Math.floor(p);
                u = u * u * (3 - 2 * u);
                yield lerp(stops.get(a), stops.get((a + 1) % n), u);
            }
            default -> gradient(t);
        };
    }

    private static long hash(int v) {
        long x = v * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        return x ^ (x >>> 29);
    }

    /** Average colour of the light right now, for pools of light cast by panels and spots. */
    public int average(double seconds) {
        int r = 0, g = 0, b = 0;
        for (int k = 0; k < 5; k++) {
            int c = color(k / 4.0, seconds);
            r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255;
        }
        return ((r / 5) << 16) | ((g / 5) << 8) | (b / 5);
    }

    /** Light level the strip should emit for a redstone signal. */
    public int lightFor(int power) {
        float f = redstoneFactor(power);
        // matches the renderer: a strip that draws as off gives no light
        if (f * brightness / 100f <= 0.02f) return 0;
        return Math.round(light * f * Math.max(brightness, 25) / 100f);
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
