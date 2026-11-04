package com.lach.lumend.client;

import com.lach.lumend.LedGraph;
import com.lach.lumend.LedStripBlockEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Client cache of gradient positions per strip, rebuilt when any strip changes. */
public final class LedRuns {
    private static final float[] DEFAULT = {0, 1};
    private static final Map<LedGraph.Bar, float[]> CACHE = new HashMap<>();
    private static int version = -1;
    private static Level lastLevel;

    private LedRuns() {}

    public static float[] get(Level level, BlockPos pos, int idx) {
        if (version != LedStripBlockEntity.VERSION || lastLevel != level) {
            CACHE.clear();
            version = LedStripBlockEntity.VERSION;
            lastLevel = level;
        }
        LedGraph.Bar b = new LedGraph.Bar(pos, idx);
        float[] t = CACHE.get(b);
        if (t == null) {
            Map<LedGraph.Bar, float[]> m = LedGraph.parametrize(LedGraph.component(level, b));
            CACHE.putAll(m);
            t = m.getOrDefault(b, DEFAULT);
            CACHE.put(b, t);
        }
        return t;
    }
}
