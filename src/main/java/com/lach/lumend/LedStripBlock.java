package com.lach.lumend;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LedStripBlock extends BaseEntityBlock {
    public static final MapCodec<LedStripBlock> CODEC = simpleCodec(LedStripBlock::new);
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    static BlockBehaviour.Properties properties(BlockBehaviour.Properties p) {
        return p.noCollision().noOcclusion().strength(0.2f).sound(SoundType.GLASS)
            .pushReaction(PushReaction.DESTROY).lightLevel(s -> s.getValue(LIGHT));
    }

    public LedStripBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIGHT, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIGHT); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LedStripBlockEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (level.getBlockEntity(pos) instanceof LedStripBlockEntity be && be.mask() != 0) return Placement.shape(be.mask());
        return Shapes.box(0.4, 0.4, 0.4, 0.6, 0.6, 0.6);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) { return true; }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Lumend.LED_STRIP.get());
    }

    /** Strip the player is looking at in this cell, or -1. */
    public static int targeted(Player player, BlockPos pos, int mask) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1f, false);
        Vec3 loc = hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK ? bh.getLocation() : player.getEyePosition();
        return Placement.nearest(mask, loc.subtract(Vec3.atLowerCornerOf(pos)));
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof LedStripBlockEntity be) {
            if (be.count() > 1) {
                int i = targeted(player, pos, be.mask());
                if (i >= 0 && !level.isClientSide()) {
                    be.remove(i);
                    if (!player.getAbilities().instabuild) popResource(level, pos, new ItemStack(Lumend.LED_STRIP.get()));
                    level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6f, 1.6f);
                }
                return false;
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof LedStripBlockEntity be && be.count() > 0)
            return List.of(new ItemStack(Lumend.LED_STRIP.get(), be.count()));
        return List.of();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof LedStripBlockEntity be) {
            int dropped = be.refresh();
            if (dropped > 0) popResource(level, pos, new ItemStack(Lumend.LED_STRIP.get(), dropped));
            if (be.mask() == 0) level.removeBlock(pos, false);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean moving) {
        super.neighborChanged(state, level, pos, block, orientation, moving);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof LedStripBlockEntity be)) return;
        int dropped = be.refresh();
        if (dropped > 0) popResource(level, pos, new ItemStack(Lumend.LED_STRIP.get(), dropped));
        if (be.mask() == 0) level.removeBlock(pos, false);
    }
}
