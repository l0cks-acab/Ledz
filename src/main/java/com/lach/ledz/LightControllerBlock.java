package com.lach.ledz;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Light Controller: set it with the LED Configurator (a look with a channel) to push that look to every light on the
 * channel within range. Its redstone signal is passed on to those lights.
 */
public class LightControllerBlock extends BaseEntityBlock {
    public static final MapCodec<LightControllerBlock> CODEC = simpleCodec(LightControllerBlock::new);
    /** 0-15 dye channel, 16 = not set. */
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, 16);
    public static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.5f).sound(SoundType.COPPER).requiresCorrectToolForDrops();

    public LightControllerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(CHANNEL, 16));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CHANNEL); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LightControllerBlockEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LightControllerBlockEntity be) be.updatePower();
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof LightControllerBlockEntity be) be.loaded();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        int ch = !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LightControllerBlockEntity be ? be.channel() : -1;
        super.onRemove(state, level, pos, newState, moved);
        // lights fall back to their own redstone
        if (ch >= 0 && !level.isClientSide) ChannelNet.repower(level, pos, ch);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LightControllerBlockEntity be) {
            int ch = be.channel();
            if (ch < 0) player.displayClientMessage(Component.translatable("msg.ledz.controller.unset"), true);
            else player.displayClientMessage(Component.translatable("msg.ledz.controller.info", LedConfig.channelName(ch),
                    ChannelNet.lights(level, pos, ch).size(), be.power()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
