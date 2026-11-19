package com.lach.ledz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server-side index of loaded lights and Light Controllers, so controllers can reach the lights on their channel. */
public final class ChannelNet {
    public static final int RANGE = 48;

    /** A light that can listen to a channel. */
    public interface Light {
        /** Bit per channel this light uses. */
        int channelMask();

        /** Re-read redstone and controller power. */
        void repower();

        /** Apply a controller's look to every part of this light on the channel. Returns parts changed. */
        int applyChannel(int channel, LedConfig cfg);
    }

    private static final Map<Level, Set<BlockEntity>> LIGHTS = new WeakHashMap<>();
    private static final Map<Level, Set<LightControllerBlockEntity>> CONTROLLERS = new WeakHashMap<>();

    private ChannelNet() {}

    public static void addLight(BlockEntity be) {
        if (be.getLevel() != null && !be.getLevel().isClientSide) LIGHTS.computeIfAbsent(be.getLevel(), l -> new HashSet<>()).add(be);
    }

    public static void removeLight(BlockEntity be) {
        if (be.getLevel() != null) {
            Set<BlockEntity> s = LIGHTS.get(be.getLevel());
            if (s != null) s.remove(be);
        }
    }

    public static void addController(LightControllerBlockEntity be) {
        if (be.getLevel() != null && !be.getLevel().isClientSide) CONTROLLERS.computeIfAbsent(be.getLevel(), l -> new HashSet<>()).add(be);
    }

    public static void removeController(LightControllerBlockEntity be) {
        if (be.getLevel() != null) {
            Set<LightControllerBlockEntity> s = CONTROLLERS.get(be.getLevel());
            if (s != null) s.remove(be);
        }
    }

    private static boolean near(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) <= RANGE && Math.abs(a.getY() - b.getY()) <= RANGE && Math.abs(a.getZ() - b.getZ()) <= RANGE;
    }

    /** Strongest controller signal reaching a light at pos on any channel in the mask. */
    public static int power(Level level, BlockPos pos, int mask) {
        if (mask == 0 || level.isClientSide) return 0;
        Set<LightControllerBlockEntity> s = CONTROLLERS.get(level);
        if (s == null) return 0;
        int p = 0;
        for (LightControllerBlockEntity c : s) {
            int ch = c.channel();
            if (ch >= 0 && (mask & (1 << ch)) != 0 && !c.isRemoved() && near(c.getBlockPos(), pos)) p = Math.max(p, c.power());
        }
        return p;
    }

    /** Loaded lights within range of pos that use the channel. */
    public static List<Light> lights(Level level, BlockPos pos, int channel) {
        List<Light> out = new ArrayList<>();
        Set<BlockEntity> s = LIGHTS.get(level);
        if (s == null || channel < 0) return out;
        for (BlockEntity be : List.copyOf(s))
            if (be instanceof Light l && !be.isRemoved() && (l.channelMask() & (1 << channel)) != 0 && near(be.getBlockPos(), pos)) out.add(l);
        return out;
    }

    /** Tell lights on the channel that a controller's signal changed. */
    public static void repower(Level level, BlockPos pos, int channel) {
        for (Light l : lights(level, pos, channel)) l.repower();
    }
}
