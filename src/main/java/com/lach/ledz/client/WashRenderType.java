package com.lach.ledz.client;

import com.lach.ledz.Ledz;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Additive glow like lightning, but without depth writes so overlapping washes blend instead of z-fighting. */
public final class WashRenderType {
    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.LIGHTNING_SNIPPET)
            .withLocation(Ledz.id("pipeline/wash"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 1.0f, 10.0f))
            .build();

    public static final RenderType WASH = RenderType.create("ledz_wash",
            RenderSetup.builder(PIPELINE).sortOnUpload().createRenderSetup());

    private WashRenderType() {}
}
