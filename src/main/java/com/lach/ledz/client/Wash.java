package com.lach.ledz.client;

import com.lach.ledz.Placement;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fake coloured light: additive quads fading out across the surfaces next to a strip
 * (the surface it is stuck to and the surface past its edge).
 */
public final class Wash {
    public static final float REACH = 2.5f;
    private static final float EPS = 0.012f;
    private static final int SUB = 2;

    /** x, y, z, rgb, alpha per vertex; 4 vertices per quad. Coordinates are cell-local. */
    public record Vertex(float x, float y, float z, int rgb, int alpha) {}

    private Wash() {}

    private static int sgn(Direction d) { return d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0; }

    private static boolean surface(Level level, BlockPos behind, Direction facing) {
        BlockState st = level.getBlockState(behind);
        return st.isFaceSturdy(level, behind, facing);
    }

    private static boolean open(Level level, BlockPos p) {
        return !level.getBlockState(p).canOcclude();
    }

    /**
     * @param colors strip colours sampled at SEG+1 points along its axis
     * @param strength 0..1
     */
    public static void build(Level level, BlockPos cell, int idx, int[] colors, float strength, List<Vertex> out) {
        Direction f = Placement.face(idx), s = Placement.side(idx);
        int a = Placement.axis(idx).ordinal();
        int maxA = Math.round(110 * strength);
        if (maxA <= 0) return;
        // plane on the supporting face, spreading away from the edge (direction -s)
        plane(level, cell, f, s, a, colors, maxA, out);
        // plane on the block past the edge, spreading away from the face (direction -f)
        if (surface(level, cell.relative(s), s.getOpposite())) plane(level, cell, s, f, a, colors, maxA, out);
    }

    /** Wash on the surface behind {@code wall}, starting at the {@code edge} side and spreading towards -edge. */
    private static void plane(Level level, BlockPos cell, Direction wall, Direction edge, int a, int[] colors, int maxA, List<Vertex> out) {
        int wa = wall.getAxis().ordinal(), ea = edge.getAxis().ordinal();
        float wc = sgn(wall) == 1 ? 1 - EPS : EPS;
        float start = sgn(edge);
        int dir = sgn(edge) == 1 ? -1 : 1;
        int blocks = (int) Math.ceil(REACH);
        int seg = colors.length - 1;
        for (int k = 0; k < blocks; k++) {
            BlockPos air = cell.relative(edge.getOpposite(), k);
            if (k > 0 && !open(level, air)) break;
            BlockPos behind = air.relative(wall);
            if (!surface(level, behind, wall.getOpposite())) break;
            for (int sub = 0; sub < SUB; sub++) {
                float d0 = k + sub / (float) SUB, d1 = k + (sub + 1) / (float) SUB;
                if (d0 >= REACH) break;
                d1 = Math.min(d1, REACH);
                int a0 = fade(d0, maxA), a1 = fade(d1, maxA);
                for (int j = 0; j < seg; j++) {
                    float u0 = j / (float) seg, u1 = (j + 1) / (float) seg;
                    float[][] pts = {{d0, u0}, {d1, u0}, {d1, u1}, {d0, u1}};
                    int[] al = {a0, a1, a1, a0};
                    int[] cs = {colors[j], colors[j], colors[j + 1], colors[j + 1]};
                    for (int v = 0; v < 4; v++) {
                        float[] xyz = new float[3];
                        xyz[wa] = wc;
                        xyz[ea] = start + dir * pts[v][0];
                        xyz[a] = pts[v][1];
                        out.add(new Vertex(xyz[0], xyz[1], xyz[2], cs[v], al[v]));
                    }
                }
            }
        }
    }

    private static int fade(float d, int maxA) {
        float t = 1 - d / REACH;
        return Math.round(maxA * t * t);
    }
}
