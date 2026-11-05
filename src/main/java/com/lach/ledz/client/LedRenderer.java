package com.lach.ledz.client;

import com.lach.ledz.LedConfig;
import com.lach.ledz.LedStripBlockEntity;
import com.lach.ledz.Ledz;
import com.lach.ledz.Placement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleToIntFunction;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;

public class LedRenderer implements BlockEntityRenderer<LedStripBlockEntity> {
    private static final int SEGMENTS = 8;
    private static final float OVERLAP = 0.004f;
    private static final int OFF_COLOR = 0x2A2C30;

    public LedRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(LedStripBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null || be.mask() == 0) return;
        double secs = (level.getGameTime() % 2_400_000L + partialTick) / 20.0;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Ledz.id("block/led"));
        VertexConsumer solid = buffers.getBuffer(RenderType.solid());
        PoseStack.Pose pose = ps.last();
        boolean[] lit = new boolean[Placement.COUNT];
        int[] capBits = new int[Placement.COUNT];
        DoubleToIntFunction[] colors = new DoubleToIntFunction[Placement.COUNT];
        for (int i = 0; i < Placement.COUNT; i++) {
            if (!be.has(i)) continue;
            LedConfig cfg = be.config(i);
            float[] t = LedRuns.get(level, be.getBlockPos(), i);
            float f = cfg.redstoneFactor(be.power()) * cfg.brightness() / 100f;
            lit[i] = f > 0.02f;
            colors[i] = lit[i] ? u -> LedConfig.scale(cfg.color(t[0] + (t[1] - t[0]) * u, secs), 0.15f + 0.85f * f) : u -> OFF_COLOR;
            capBits[i] = caps(level, be.getBlockPos(), i);
            box(solid, pose, sprite, Placement.box(i, 0), Placement.axis(i).ordinal(), capBits[i], colors[i], 255,
                    lit[i] ? LightTexture.FULL_BRIGHT : light);
        }
        List<Wash.Vertex> wash = new ArrayList<>();
        for (int i = 0; i < Placement.COUNT; i++) {
            if (!lit[i] || be.config(i).wash() == 0) continue;
            int[] samples = new int[SEGMENTS + 1];
            for (int k = 0; k <= SEGMENTS; k++) samples[k] = colors[i].applyAsInt(k / (double) SEGMENTS);
            float f = be.config(i).redstoneFactor(be.power()) * be.config(i).brightness() / 100f;
            Wash.build(level, be.getBlockPos(), i, samples, be.config(i).wash() / 100f * f, wash);
        }
        if (!wash.isEmpty()) {
            VertexConsumer add = buffers.getBuffer(RenderType.lightning());
            for (int q = 0; q < wash.size(); q += 4) {
                for (int v = 0; v < 4; v++) washVertex(add, pose, wash.get(q + v));
                for (int v = 3; v >= 0; v--) washVertex(add, pose, wash.get(q + v));
            }
        }
    }

    private static void washVertex(VertexConsumer vc, PoseStack.Pose pose, Wash.Vertex v) {
        vc.addVertex(pose, v.x(), v.y(), v.z()).setColor((v.rgb() >> 16) & 255, (v.rgb() >> 8) & 255, v.rgb() & 255, v.alpha());
    }

    @Override
    public boolean shouldRenderOffScreen(LedStripBlockEntity be) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(LedStripBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(Wash.REACH + 0.5);
    }

    /** Box with per-vertex colour along the strip axis. */
    private static void box(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, float[] b, int axis, int caps,
                            DoubleToIntFunction color, int alpha, int light) {
        // joined ends overlap their neighbour slightly so no crack shows between block entities
        b = b.clone();
        if ((caps & 1) == 0) b[axis] -= OVERLAP;
        if ((caps & 2) == 0) b[axis + 3] += OVERLAP;
        for (int k = 0; k < 3; k++) {
            int p = (k + 1) % 3, q = (k + 2) % 3;
            for (int sign = -1; sign <= 1; sign += 2) {
                if (k == axis && (caps & (sign > 0 ? 2 : 1)) == 0) continue;
                float c = sign > 0 ? b[k + 3] : b[k];
                int segP = p == axis ? SEGMENTS : 1, segQ = q == axis ? SEGMENTS : 1;
                for (int sp = 0; sp < segP; sp++)
                    for (int sq = 0; sq < segQ; sq++) {
                        float p0 = lerp(b[p], b[p + 3], sp / (float) segP), p1 = lerp(b[p], b[p + 3], (sp + 1) / (float) segP);
                        float q0 = lerp(b[q], b[q + 3], sq / (float) segQ), q1 = lerp(b[q], b[q + 3], (sq + 1) / (float) segQ);
                        float[][] pq = sign > 0
                                ? new float[][]{{p0, q0}, {p1, q0}, {p1, q1}, {p0, q1}}
                                : new float[][]{{p0, q0}, {p0, q1}, {p1, q1}, {p1, q0}};
                        for (float[] v : pq) {
                            float[] xyz = new float[3];
                            xyz[k] = c;
                            xyz[p] = v[0];
                            xyz[q] = v[1];
                            int rgb = color.applyAsInt(Math.clamp(xyz[axis], 0f, 1f));
                            float[] n = new float[3];
                            n[k] = sign;
                            vc.addVertex(pose, xyz[0], xyz[1], xyz[2])
                                    .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha)
                                    .setUv(s.getU(p == axis ? 0.5f : v[0]), s.getV(q == axis ? 0.5f : v[1]))
                                    .setLight(light)
                                    .setNormal(pose, n[0], n[1], n[2]);
                        }
                    }
            }
        }
    }

    /** Bit 0: cap the low end, bit 1: cap the high end. An end touching another strip gets no cap. */
    static int caps(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, int idx) {
        net.minecraft.core.BlockPos[] e = Placement.endpoints(pos, idx);
        int caps = 0;
        for (int k = 0; k < 2; k++) {
            boolean joined = false;
            for (com.lach.ledz.LedGraph.Bar b : com.lach.ledz.LedGraph.at(level, e[k], null))
                if (!(b.cell().equals(pos) && b.idx() == idx)) { joined = true; break; }
            if (!joined) caps |= 1 << k;
        }
        return caps;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
