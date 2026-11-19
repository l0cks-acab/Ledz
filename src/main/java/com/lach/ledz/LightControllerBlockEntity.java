package com.lach.ledz;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LightControllerBlockEntity extends BlockEntity {
    private LedConfig config;
    private int power;

    public LightControllerBlockEntity(BlockPos pos, BlockState state) {
        super(Ledz.CONTROLLER_BE.get(), pos, state);
    }

    public LedConfig config() { return config; }
    public int power() { return power; }
    public int channel() { return config == null ? LedConfig.NO_CHANNEL : config.channel(); }

    /** Store the look and push it to all lights on its channel. Returns lights changed. */
    public int broadcast(LedConfig cfg) {
        if (level == null || level.isClientSide || cfg.channel() < 0) return 0;
        int old = channel();
        config = cfg;
        setChanged();
        BlockState st = getBlockState();
        if (st.getValue(LightControllerBlock.CHANNEL) != cfg.channel())
            level.setBlock(worldPosition, st.setValue(LightControllerBlock.CHANNEL, cfg.channel()), Block.UPDATE_ALL);
        int n = 0;
        for (ChannelNet.Light l : ChannelNet.lights(level, worldPosition, cfg.channel())) n += l.applyChannel(cfg.channel(), cfg);
        if (old >= 0 && old != cfg.channel()) ChannelNet.repower(level, worldPosition, old);
        ChannelNet.repower(level, worldPosition, cfg.channel());
        return n;
    }

    public void updatePower() {
        if (level == null || level.isClientSide) return;
        int p = level.getBestNeighborSignal(worldPosition);
        if (p == power) return;
        power = p;
        setChanged();
        if (channel() >= 0) ChannelNet.repower(level, worldPosition, channel());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("power", power);
        if (config != null) LedConfig.CODEC.encodeStart(NbtOps.INSTANCE, config).ifSuccess(t -> tag.put("config", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getInt("power");
        config = null;
        if (tag.contains("config")) LedConfig.CODEC.parse(NbtOps.INSTANCE, tag.get("config")).ifSuccess(c -> config = c);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ChannelNet.addController(this);
            // lights loaded before this controller need its signal: done on the next tick
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    /** First tick after loading. */
    public void loaded() {
        if (level == null || level.isClientSide) return;
        power = level.getBestNeighborSignal(worldPosition);
        if (channel() >= 0) ChannelNet.repower(level, worldPosition, channel());
    }

    @Override
    public void setRemoved() {
        ChannelNet.removeController(this);
        super.setRemoved();
    }
}
