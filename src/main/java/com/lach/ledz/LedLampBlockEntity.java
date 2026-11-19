package com.lach.ledz;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LedLampBlockEntity extends BlockEntity implements ChannelNet.Light {
    private LedConfig config = LedConfig.LAMP_DEFAULT;
    private int power;

    public LedLampBlockEntity(BlockPos pos, BlockState state) {
        super(Ledz.LAMP_BE.get(), pos, state);
    }

    public LedConfig config() { return config; }
    public int power() { return power; }

    public void setConfig(LedConfig cfg) {
        if (config.equals(cfg)) return;
        config = cfg;
        power = computePower();
        changed();
    }

    @Override
    public int channelMask() {
        return config.channel() >= 0 ? 1 << config.channel() : 0;
    }

    private int computePower() {
        if (level == null || level.isClientSide) return power;
        return Math.max(level.getBestNeighborSignal(worldPosition), ChannelNet.power(level, worldPosition, channelMask()));
    }

    @Override
    public void repower() {
        int p = computePower();
        if (p != power) {
            power = p;
            changed();
        } else updateLight();
    }

    @Override
    public int applyChannel(int channel, LedConfig cfg) {
        if (config.channel() != channel || config.equals(cfg)) return 0;
        setConfig(cfg);
        return 1;
    }

    private void updateLight() {
        if (level == null || level.isClientSide) return;
        int l = config.lightFor(power);
        BlockState st = getBlockState();
        if (st.hasProperty(LedStripBlock.LIGHT) && st.getValue(LedStripBlock.LIGHT) != l)
            level.setBlock(worldPosition, st.setValue(LedStripBlock.LIGHT, l), Block.UPDATE_CLIENTS);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            // onLoad only runs on the next tick for freshly placed lights
            if (!isRemoved()) ChannelNet.addLight(this);
            updateLight();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("power", power);
        LedConfig.CODEC.encodeStart(NbtOps.INSTANCE, config).ifSuccess(t -> tag.put("config", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getInt("power");
        config = LedConfig.LAMP_DEFAULT;
        if (tag.contains("config")) LedConfig.CODEC.parse(NbtOps.INSTANCE, tag.get("config")).ifSuccess(c -> config = c);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ChannelNet.addLight(this);
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public void setRemoved() {
        ChannelNet.removeLight(this);
        super.setRemoved();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
