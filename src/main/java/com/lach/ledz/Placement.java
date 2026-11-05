package com.lach.ledz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A strip placement inside a cell: the face it is stuck to (towards the supporting block)
 * and which edge of that face it hugs. 6 faces x 4 sides = 24 placements.
 */
public final class Placement {
    public static final int COUNT = 24;
    public static final float THICK = 1 / 16f;
    public static final float WIDTH = 2 / 16f;

    private static final Direction[] FACE = new Direction[COUNT];
    private static final Direction[] SIDE = new Direction[COUNT];
    private static final Map<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    static {
        for (Direction f : Direction.values()) {
            int n = 0;
            for (Direction s : Direction.values()) {
                if (s.getAxis() == f.getAxis()) continue;
                int i = f.ordinal() * 4 + n++;
                FACE[i] = f;
                SIDE[i] = s;
            }
        }
    }

    private Placement() {}

    public static Direction face(int i) { return FACE[i]; }
    public static Direction side(int i) { return SIDE[i]; }

    public static int index(Direction face, Direction side) {
        for (int i = face.ordinal() * 4; i < face.ordinal() * 4 + 4; i++) if (SIDE[i] == side) return i;
        return -1;
    }

    /** Axis the strip runs along. */
    public static Direction.Axis axis(int i) {
        for (Direction.Axis a : Direction.Axis.values())
            if (a != FACE[i].getAxis() && a != SIDE[i].getAxis()) return a;
        throw new IllegalStateException();
    }

    private static int pos(Direction d) { return d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0; }

    /** The two corner-lattice endpoints of the strip, low end first. */
    public static BlockPos[] endpoints(BlockPos cell, int i) {
        int[] c = new int[3];
        c[FACE[i].getAxis().ordinal()] = pos(FACE[i]);
        c[SIDE[i].getAxis().ordinal()] = pos(SIDE[i]);
        int a = axis(i).ordinal();
        BlockPos p0 = cell.offset(c[0], c[1], c[2]);
        c[a] = 1;
        BlockPos p1 = cell.offset(c[0], c[1], c[2]);
        return new BlockPos[]{p0, p1};
    }

    /** Local box [minX,minY,minZ,maxX,maxY,maxZ] of the strip, grown by {@code grow} (clamped to the cell). */
    public static float[] box(int i, float grow) {
        float[] b = {0, 0, 0, 1, 1, 1};
        Direction f = FACE[i], s = SIDE[i];
        int fa = f.getAxis().ordinal(), sa = s.getAxis().ordinal();
        if (pos(f) == 1) b[fa] = 1 - THICK - grow; else b[fa + 3] = THICK + grow;
        if (pos(s) == 1) b[sa] = 1 - WIDTH - grow; else b[sa + 3] = WIDTH + grow;
        for (int k = 0; k < 3; k++) {
            b[k] = Math.max(0, b[k]);
            b[k + 3] = Math.min(1, b[k + 3]);
        }
        return b;
    }

    public static AABB aabb(int i, float grow) {
        float[] b = box(i, grow);
        return new AABB(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    public static VoxelShape shape(int mask) {
        return SHAPES.computeIfAbsent(mask, m -> {
            VoxelShape s = Shapes.empty();
            for (int i = 0; i < COUNT; i++)
                if ((m & (1 << i)) != 0) s = Shapes.joinUnoptimized(s, Shapes.create(aabb(i, 1 / 32f)), BooleanOp.OR);
            return s.optimize();
        });
    }

    public static double distance(int i, Vec3 local) {
        AABB b = aabb(i, 0);
        double dx = Math.max(Math.max(b.minX - local.x, 0), local.x - b.maxX);
        double dy = Math.max(Math.max(b.minY - local.y, 0), local.y - b.maxY);
        double dz = Math.max(Math.max(b.minZ - local.z, 0), local.z - b.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Nearest present placement to a local point. */
    public static int nearest(int mask, Vec3 local) {
        int best = -1;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < COUNT; i++) {
            if ((mask & (1 << i)) == 0) continue;
            double d = distance(i, local);
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    /** Sides of a face sorted by distance of the local point to that edge. */
    public static List<Integer> sidesByDistance(Direction face, Vec3 local) {
        List<Integer> out = new ArrayList<>();
        for (int i = face.ordinal() * 4; i < face.ordinal() * 4 + 4; i++) out.add(i);
        out.sort((x, y) -> Double.compare(edgeDist(SIDE[x], local), edgeDist(SIDE[y], local)));
        return out;
    }

    private static double edgeDist(Direction s, Vec3 l) {
        double c = l.get(s.getAxis());
        return pos(s) == 1 ? 1 - c : c;
    }

    public static double faceDist(Direction f, Vec3 l) {
        double c = l.get(f.getAxis());
        return pos(f) == 1 ? 1 - c : c;
    }
}
