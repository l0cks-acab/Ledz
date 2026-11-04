package com.lach.lumend;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class LedStripItem extends Item {
    public LedStripItem(Properties props) {
        super(props);
    }

    private static boolean supported(Level level, BlockPos cell, Direction face) {
        BlockPos s = cell.relative(face);
        return level.getBlockState(s).isFaceSturdy(level, s, face.getOpposite());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        BlockState cs = level.getBlockState(clicked);
        BlockPos cell;
        Direction face;
        int mask = 0;
        if (cs.is(Lumend.LED_BLOCK.get()) && level.getBlockEntity(clicked) instanceof LedStripBlockEntity be) {
            cell = clicked;
            mask = be.mask();
            Vec3 local = ctx.getClickLocation().subtract(Vec3.atLowerCornerOf(cell));
            int near = Placement.nearest(mask, local);
            face = near >= 0 ? Placement.face(near) : null;
            if (face == null || freeSide(face, mask, local) < 0) {
                face = null;
                double bd = Double.MAX_VALUE;
                for (Direction f : Direction.values()) {
                    double d = Placement.faceDist(f, local);
                    if (d < bd && supported(level, cell, f) && freeSide(f, mask, local) >= 0) { bd = d; face = f; }
                }
            }
            if (face == null) return InteractionResult.FAIL;
        } else {
            cell = clicked.relative(ctx.getClickedFace());
            face = ctx.getClickedFace().getOpposite();
            BlockState target = level.getBlockState(cell);
            if (target.is(Lumend.LED_BLOCK.get()) && level.getBlockEntity(cell) instanceof LedStripBlockEntity be) mask = be.mask();
            else if (!target.canBeReplaced(new BlockPlaceContext(ctx))) return InteractionResult.FAIL;
        }
        if (!supported(level, cell, face)) return InteractionResult.FAIL;
        Vec3 local = ctx.getClickLocation().subtract(Vec3.atLowerCornerOf(cell));
        int idx = freeSide(face, mask, local);
        if (idx < 0) return InteractionResult.FAIL;

        if (!level.isClientSide) {
            if (!level.getBlockState(cell).is(Lumend.LED_BLOCK.get()))
                level.setBlock(cell, Lumend.LED_BLOCK.get().defaultBlockState(), 3);
            if (!(level.getBlockEntity(cell) instanceof LedStripBlockEntity be)) return InteractionResult.FAIL;
            LedConfig cfg = LedConfig.STRIP_DEFAULT;
            find:
            for (BlockPos v : Placement.endpoints(cell, idx))
                for (LedGraph.Bar n : LedGraph.at(level, v, null)) {
                    cfg = LedGraph.config(level, n);
                    break find;
                }
            be.add(idx, cfg == null ? LedConfig.STRIP_DEFAULT : cfg);
            be.refresh();
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            level.playSound(null, cell, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.7f, 1.4f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static int freeSide(Direction face, int mask, Vec3 local) {
        for (int i : Placement.sidesByDistance(face, local)) if ((mask & (1 << i)) == 0) return i;
        return -1;
    }
}
