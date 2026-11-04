package com.lach.lumend;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LedStripBlockEntity extends BlockEntity {
    /** Bumped whenever any strip changes, so client run caches know to rebuild. */
    public static volatile int VERSION;

    private static final Codec<Map<String, LedConfig>> STRIPS = Codec.unboundedMap(Codec.STRING, LedConfig.CODEC);

    private int mask;
    private int power;
    private final LedConfig[] configs = new LedConfig[Placement.COUNT];

    public LedStripBlockEntity(BlockPos pos, BlockState state) {
        super(Lumend.LED_BE.get(), pos, state);
    }

    public int mask() { return mask; }
    public int power() { return power; }
    public boolean has(int i) { return (mask & (1 << i)) != 0; }
    public LedConfig config(int i) { return configs[i] == null ? LedConfig.STRIP_DEFAULT : configs[i]; }
    public int count() { return Integer.bitCount(mask); }

    public void add(int i, LedConfig cfg) {
        mask |= 1 << i;
        configs[i] = cfg;
        changed();
    }

    public void remove(int i) {
        mask &= ~(1 << i);
        configs[i] = null;
        changed();
    }

    public void clear() {
        mask = 0;
        changed();
    }

    public void setConfig(int i, LedConfig cfg) {
        if (!has(i) || config(i).equals(cfg)) return;
        configs[i] = cfg;
        changed();
    }

    /** Server: re-read redstone and drop strips that lost support. Returns strips dropped. */
    public int refresh() {
        if (level == null || level.isClientSide()) return 0;
        int dropped = 0;
        for (int i = 0; i < Placement.COUNT; i++) {
            if (!has(i)) continue;
            Direction f = Placement.face(i);
            BlockPos s = worldPosition.relative(f);
            if (!level.getBlockState(s).isFaceSturdy(level, s, f.getOpposite())) {
                mask &= ~(1 << i);
                configs[i] = null;
                dropped++;
            }
        }
        int p = level.getBestNeighborSignal(worldPosition);
        boolean dirty = dropped > 0 || p != power;
        power = p;
        if (dirty) changed();
        else updateLight();
        return dropped;
    }

    private void updateLight() {
        if (level == null || level.isClientSide()) return;
        int l = 0;
        for (int i = 0; i < Placement.COUNT; i++) if (has(i)) l = Math.max(l, config(i).lightFor(power));
        BlockState st = getBlockState();
        if (st.hasProperty(LedStripBlock.LIGHT) && st.getValue(LedStripBlock.LIGHT) != l)
            level.setBlock(worldPosition, st.setValue(LedStripBlock.LIGHT, l), Block.UPDATE_CLIENTS);
    }

    private void changed() {
        VERSION++;
        setChanged();
        if (level != null && !level.isClientSide()) {
            updateLight();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("mask", mask);
        out.putInt("power", power);
        Map<String, LedConfig> m = new HashMap<>();
        for (int i = 0; i < Placement.COUNT; i++) if (has(i)) m.put(Integer.toString(i), config(i));
        out.store("strips", STRIPS, m);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        mask = in.getIntOr("mask", 0);
        power = in.getIntOr("power", 0);
        Map<String, LedConfig> m = in.read("strips", STRIPS).orElse(Map.of());
        for (int i = 0; i < Placement.COUNT; i++) configs[i] = has(i) ? m.get(Integer.toString(i)) : null;
        VERSION++;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        VERSION++;
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
