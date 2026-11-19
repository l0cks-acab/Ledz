package com.lach.ledz;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** LED Panel (flat square) or LED Spot (small round downlight), stuck to a face and shining out along FACING. */
public class LedLampBlock extends BaseEntityBlock {
    public static final MapCodec<LedLampBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("spot").forGetter(b -> b.spot), propertiesCodec()).apply(i, LedLampBlock::new));
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
            .noCollission().noOcclusion().strength(0.3f).sound(SoundType.GLASS)
            .pushReaction(PushReaction.DESTROY).lightLevel(s -> s.getValue(LedStripBlock.LIGHT));

    public final boolean spot;
    private final VoxelShape[] shapes = new VoxelShape[6];

    public LedLampBlock(boolean spot, Properties props) {
        super(props);
        this.spot = spot;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(LedStripBlock.LIGHT, 0));
        float lo = lateral(), h = height();
        for (Direction d : Direction.values()) {
            double[] min = {lo, lo, lo}, max = {1 - lo, 1 - lo, 1 - lo};
            int a = d.getAxis().ordinal();
            boolean pos = d.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            min[a] = pos ? 0 : 1 - h;
            max[a] = pos ? h : 1;
            shapes[d.ordinal()] = Shapes.box(min[0], min[1], min[2], max[0], max[1], max[2]);
        }
    }

    /** Housing thickness. */
    public float height() { return spot ? 2 / 16f : 1 / 16f; }

    /** Housing inset from the cell edge. */
    public float lateral() { return spot ? 5 / 16f : 1 / 16f; }

    /** Glowing area inset from the cell edge. */
    public float glowInset() { return spot ? 6 / 16f : 2 / 16f; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, LedStripBlock.LIGHT); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LedLampBlockEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes[state.getValue(FACING).ordinal()];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState st = defaultBlockState().setValue(FACING, ctx.getClickedFace());
        return st.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? st : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction f = state.getValue(FACING);
        BlockPos s = pos.relative(f.getOpposite());
        return spot ? canSupportCenter(level, s, f) : level.getBlockState(s).isFaceSturdy(level, s, f);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (dir == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, other, level, pos, otherPos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LedLampBlockEntity be) be.repower();
    }

    /** Lateral neighbours on the same plane, facing the same way. */
    private static BlockPos[] around(BlockState state, BlockPos pos) {
        Direction.Axis a = state.getValue(FACING).getAxis();
        java.util.List<BlockPos> out = new java.util.ArrayList<>(4);
        for (Direction d : Direction.values()) if (d.getAxis() != a) out.add(pos.relative(d));
        return out.toArray(new BlockPos[0]);
    }

    private static boolean sameKind(BlockState a, BlockState b) {
        return a.getBlock() == b.getBlock() && b.getValue(FACING) == a.getValue(FACING);
    }

    /** Touching lamps of the same kind, facing and look (up to 1024). */
    public static java.util.Set<BlockPos> group(Level level, BlockPos start) {
        java.util.Set<BlockPos> seen = new java.util.LinkedHashSet<>();
        if (!(level.getBlockEntity(start) instanceof LedLampBlockEntity first)) return seen;
        BlockState st = level.getBlockState(start);
        LedConfig cfg = first.config();
        java.util.ArrayDeque<BlockPos> q = new java.util.ArrayDeque<>();
        q.add(start.immutable());
        seen.add(start.immutable());
        while (!q.isEmpty() && seen.size() < 1024) {
            BlockPos p = q.poll();
            for (BlockPos n : around(st, p)) {
                if (seen.contains(n) || !sameKind(st, level.getBlockState(n))) continue;
                if (level.getBlockEntity(n) instanceof LedLampBlockEntity e && e.config().equals(cfg)) {
                    seen.add(n);
                    q.add(n);
                }
            }
        }
        return seen;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof LedLampBlockEntity be)) return;
        for (BlockPos n : around(state, pos))
            if (sameKind(state, level.getBlockState(n)) && level.getBlockEntity(n) instanceof LedLampBlockEntity o) {
                be.setConfig(o.config());
                return;
            }
        be.repower();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof LedLampBlockEntity be) be.repower();
    }
}
