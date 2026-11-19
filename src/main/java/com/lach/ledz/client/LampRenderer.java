package com.lach.ledz.client;

import com.lach.ledz.LedConfig;
import com.lach.ledz.LedLampBlock;
import com.lach.ledz.LedLampBlockEntity;
import com.lach.ledz.Ledz;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Glowing face of LED panels and spots, plus the pool of light they cast. */
public class LampRenderer implements BlockEntityRenderer<LedLampBlockEntity> {
    private static final int SEG = 8;
    private static final int OFF_COLOR = 0x2A2C30;

    public LampRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(LedLampBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        BlockState st = be.getBlockState();
        if (level == null || !(st.getBlock() instanceof LedLampBlock lamp)) return;
        Direction facing = st.getValue(LedLampBlock.FACING);
        double secs = (level.getGameTime() % 2_400_000L + partialTick) / 20.0;
        LedConfig cfg = be.config();
        float f = cfg.redstoneFactor(be.power()) * cfg.brightness() / 100f;
        boolean lit = f > 0.02f;
        float bright = 0.15f + 0.85f * f;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Ledz.id("block/led"));
        VertexConsumer vc = buffers.getBuffer(RenderType.solid());
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(facing.getRotation());
        ps.translate(-0.5, -0.5, -0.5);
        PoseStack.Pose pose = ps.last();
        float a = lamp.glowInset(), b = 1 - a, y = lamp.height() + 0.002f;
        int segs = lamp.spot ? 1 : SEG;
        int lt = lit ? LightTexture.FULL_BRIGHT : light;
        for (int s = 0; s < segs; s++) {
            float x0 = a + (b - a) * s / segs, x1 = a + (b - a) * (s + 1) / segs;
            int c0 = lit ? LedConfig.scale(cfg.color(lamp.spot ? 0.5 : s / (double) segs, secs), bright) : OFF_COLOR;
            int c1 = lit ? LedConfig.scale(cfg.color(lamp.spot ? 0.5 : (s + 1) / (double) segs, secs), bright) : OFF_COLOR;
            vertex(vc, pose, sprite, x0, y, a, c0, lt);
            vertex(vc, pose, sprite, x0, y, b, c0, lt);
            vertex(vc, pose, sprite, x1, y, b, c1, lt);
            vertex(vc, pose, sprite, x1, y, a, c1, lt);
        }
        ps.popPose();

        if (lit && cfg.wash() > 0) {
            int rgb = LedConfig.scale(lamp.spot ? cfg.color(0.5, secs) : cfg.average(secs), bright);
            List<Wash.Vertex> pool = new ArrayList<>();
            Pool.build(level, be.getBlockPos(), facing, rgb, cfg.wash() / 100f * f, !lamp.spot, pool);
            if (!pool.isEmpty()) {
                VertexConsumer add = buffers.getBuffer(WashRenderType.WASH);
                PoseStack.Pose p = ps.last();
                for (int q = 0; q < pool.size(); q += 4) {
                    for (int v = 0; v < 4; v++) wash(add, p, pool.get(q + v));
                    for (int v = 3; v >= 0; v--) wash(add, p, pool.get(q + v));
                }
            }
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, float x, float y, float z, int rgb, int light) {
        vc.addVertex(pose, x, y, z).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255)
                .setUv(s.getU(x), s.getV(z)).setLight(light).setNormal(pose, 0, 1, 0);
    }

    private static void wash(VertexConsumer vc, PoseStack.Pose pose, Wash.Vertex v) {
        vc.addVertex(pose, v.x(), v.y(), v.z()).setColor((v.rgb() >> 16) & 255, (v.rgb() >> 8) & 255, v.rgb() & 255, v.alpha());
    }

    @Override
    public AABB getRenderBoundingBox(LedLampBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(Pool.MAX_DIST + 4);
    }
}
