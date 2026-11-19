package com.lach.ledz.client;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Fake coloured pool of light cast by a panel or spot onto the first surface it faces. */
public final class Pool {
    public static final int MAX_DIST = 12;
    private static final float EPS = 0.012f;
    private static final int SUB = 4;

    private Pool() {}

    /**
     * @param origin the lamp's cell; output vertices are relative to it
     * @param soft   true: wide soft fade (panel), false: tighter beam with a firmer edge (spot)
     */
    public static void build(Level level, BlockPos origin, Direction facing, int rgb, float strength, boolean soft, List<Wash.Vertex> out) {
        BlockPos p = origin;
        int dist = 0;
        boolean found = false;
        for (int k = 0; k <= MAX_DIST; k++) {
            BlockPos n = p.relative(facing);
            BlockState s = level.getBlockState(n);
            if (s.isFaceSturdy(level, n, facing.getOpposite())) { found = true; break; }
            if (s.canOcclude()) return;
            p = n;
            dist++;
        }
        if (!found) return;
        float radius = soft ? 2.0f + 0.25f * dist : 0.8f + 0.3f * dist;
        int maxA = Math.round((soft ? 120 : 170) * strength / (1 + 0.1f * dist));
        if (maxA <= 0) return;

        int na = facing.getAxis().ordinal();
        int ua = (na + 1) % 3, va = (na + 2) % 3;
        boolean posDir = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        int[] rel = {p.getX() - origin.getX(), p.getY() - origin.getY(), p.getZ() - origin.getZ()};
        float nc = rel[na] + (posDir ? 1 - EPS : EPS);
        int reach = (int) Math.ceil(radius);
        for (int du = -reach; du <= reach; du++)
            for (int dv = -reach; dv <= reach; dv++) {
                int[] off = new int[3];
                off[ua] = du;
                off[va] = dv;
                BlockPos q = p.offset(off[0], off[1], off[2]);
                if ((du != 0 || dv != 0 || dist > 0) && level.getBlockState(q).canOcclude()) continue;
                BlockPos behind = q.relative(facing);
                if (!level.getBlockState(behind).isFaceSturdy(level, behind, facing.getOpposite())) continue;
                for (int i = 0; i < SUB; i++)
                    for (int j = 0; j < SUB; j++) {
                        float u0 = du + i / (float) SUB, u1 = du + (i + 1) / (float) SUB;
                        float v0 = dv + j / (float) SUB, v1 = dv + (j + 1) / (float) SUB;
                        float[][] pts = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
                        int[] al = new int[4];
                        boolean any = false;
                        for (int c = 0; c < 4; c++) {
                            float r = (float) Math.hypot(pts[c][0] - 0.5f, pts[c][1] - 0.5f) / radius;
                            al[c] = alpha(r, maxA, soft);
                            any |= al[c] > 0;
                        }
                        if (!any) continue;
                        for (int c = 0; c < 4; c++) {
                            float[] xyz = new float[3];
                            xyz[na] = nc;
                            xyz[ua] = rel[ua] + pts[c][0];
                            xyz[va] = rel[va] + pts[c][1];
                            out.add(new Wash.Vertex(xyz[0], xyz[1], xyz[2], rgb, al[c]));
                        }
                    }
            }
    }

    private static int alpha(float r, int maxA, boolean soft) {
        if (r >= 1) return 0;
        if (soft) {
            float t = 1 - r;
            return Math.round(maxA * t * t);
        }
        // spot: flat core, quick falloff at the rim
        float t = Math.min(1, (1 - r) / 0.4f);
        return Math.round(maxA * t * t * (3 - 2 * t));
    }
}
