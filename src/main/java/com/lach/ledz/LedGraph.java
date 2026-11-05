package com.lach.ledz;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Strips link into runs when they share an endpoint and have the same config. */
public final class LedGraph {
    public static final int MAX_RUN = 512;

    public record Bar(BlockPos cell, int idx) {}

    private LedGraph() {}

    public static LedConfig config(Level level, Bar b) {
        if (!level.isLoaded(b.cell()) || !(level.getBlockEntity(b.cell()) instanceof LedStripBlockEntity be)) return null;
        return be.has(b.idx()) ? be.config(b.idx()) : null;
    }

    /** Bars that touch vertex v with config equal to cfg (any config if null). */
    public static List<Bar> at(Level level, BlockPos v, LedConfig cfg) {
        List<Bar> out = new ArrayList<>();
        for (int dx = -1; dx <= 0; dx++)
            for (int dy = -1; dy <= 0; dy++)
                for (int dz = -1; dz <= 0; dz++) {
                    BlockPos cell = v.offset(dx, dy, dz);
                    if (!level.isLoaded(cell) || !(level.getBlockEntity(cell) instanceof LedStripBlockEntity be)) continue;
                    for (int i = 0; i < Placement.COUNT; i++) {
                        if (!be.has(i) || (cfg != null && !be.config(i).equals(cfg))) continue;
                        BlockPos[] e = Placement.endpoints(cell, i);
                        if (e[0].equals(v) || e[1].equals(v)) out.add(new Bar(cell.immutable(), i));
                    }
                }
        return out;
    }

    public static Set<Bar> component(Level level, Bar start) {
        LedConfig cfg = config(level, start);
        Set<Bar> seen = new LinkedHashSet<>();
        if (cfg == null) return seen;
        ArrayDeque<Bar> q = new ArrayDeque<>();
        seen.add(start);
        q.add(start);
        while (!q.isEmpty() && seen.size() < MAX_RUN) {
            Bar b = q.poll();
            for (BlockPos v : Placement.endpoints(b.cell(), b.idx()))
                for (Bar n : at(level, v, cfg))
                    if (seen.size() < MAX_RUN && seen.add(n)) q.add(n);
        }
        return seen;
    }

    private static final Comparator<BlockPos> POS_ORDER = Comparator.<BlockPos>comparingInt(p -> p.getY())
            .thenComparingInt(p -> p.getX()).thenComparingInt(p -> p.getZ());

    /**
     * Gradient position at both endpoints for every bar in a run: map bar -> {t at low end, t at high end}.
     * Simple chains/loops are walked in order; branching runs fall back to graph distance.
     */
    public static Map<Bar, float[]> parametrize(Set<Bar> run) {
        if (run.isEmpty()) return new HashMap<>();
        Map<BlockPos, List<Bar>> byVertex = new HashMap<>();
        for (Bar b : run)
            for (BlockPos v : Placement.endpoints(b.cell(), b.idx()))
                byVertex.computeIfAbsent(v, k -> new ArrayList<>()).add(b);
        boolean simple = byVertex.values().stream().allMatch(l -> l.size() <= 2);
        BlockPos start = byVertex.entrySet().stream().filter(e -> e.getValue().size() == 1).map(Map.Entry::getKey)
                .min(POS_ORDER).orElseGet(() -> byVertex.keySet().stream().min(POS_ORDER).orElseThrow());

        Map<BlockPos, Integer> dist = new HashMap<>();
        Map<Bar, float[]> out = new HashMap<>();
        if (simple) {
            BlockPos v = start;
            Bar prev = null;
            Set<Bar> walked = new HashSet<>();
            List<Object[]> steps = new ArrayList<>();
            while (true) {
                Bar next = null;
                for (Bar b : byVertex.get(v)) if (b != prev && !walked.contains(b)) { next = b; break; }
                if (next == null) break;
                walked.add(next);
                BlockPos[] e = Placement.endpoints(next.cell(), next.idx());
                boolean fromLow = e[0].equals(v);
                steps.add(new Object[]{next, fromLow});
                v = fromLow ? e[1] : e[0];
                prev = next;
            }
            if (walked.size() == run.size()) {
                float n = steps.size();
                for (int k = 0; k < steps.size(); k++) {
                    Bar b = (Bar) steps.get(k)[0];
                    boolean fromLow = (Boolean) steps.get(k)[1];
                    float a = k / n, c = (k + 1) / n;
                    out.put(b, fromLow ? new float[]{a, c} : new float[]{c, a});
                }
                return out;
            }
        }
        // branching: BFS over vertices
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        dist.put(start, 0);
        q.add(start);
        int max = 1;
        while (!q.isEmpty()) {
            BlockPos v = q.poll();
            int d = dist.get(v);
            for (Bar b : byVertex.get(v)) {
                BlockPos[] e = Placement.endpoints(b.cell(), b.idx());
                BlockPos o = e[0].equals(v) ? e[1] : e[0];
                if (!dist.containsKey(o)) {
                    dist.put(o, d + 1);
                    max = Math.max(max, d + 1);
                    q.add(o);
                }
            }
        }
        for (Bar b : run) {
            BlockPos[] e = Placement.endpoints(b.cell(), b.idx());
            out.put(b, new float[]{dist.getOrDefault(e[0], 0) / (float) max, dist.getOrDefault(e[1], 0) / (float) max});
        }
        return out;
    }
}
