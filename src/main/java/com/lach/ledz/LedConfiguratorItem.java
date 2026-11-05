package com.lach.ledz;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LedConfiguratorItem extends Item {
    /** Set on the client to open the configurator screen. */
    public static Consumer<InteractionHand> openScreen = h -> {};

    public LedConfiguratorItem(Properties props) {
        super(props);
    }

    public static LedConfig get(ItemStack stack) {
        LedConfig c = stack.get(Ledz.LED_CONFIG.get());
        return c == null ? LedConfig.CONFIGURATOR_DEFAULT : c;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (!(level.getBlockEntity(pos) instanceof LedStripBlockEntity)) pos = pos.relative(ctx.getClickedFace());
        if (!(level.getBlockEntity(pos) instanceof LedStripBlockEntity be) || be.mask() == 0) return InteractionResult.PASS;
        Vec3 local = ctx.getClickLocation().subtract(Vec3.atLowerCornerOf(pos));
        int idx = Placement.nearest(be.mask(), local);
        // clicking the supporting block: only counts when close to a strip
        if (idx < 0 || Placement.distance(idx, local) > 0.3) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack stack = ctx.getItemInHand();
            if (player != null && player.isShiftKeyDown()) {
                stack.set(Ledz.LED_CONFIG.get(), be.config(idx));
                player.displayClientMessage(Component.translatable("msg.ledz.copied"), true);
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.6f);
            } else {
                LedConfig cfg = get(stack);
                Set<LedGraph.Bar> run = LedGraph.component(level, new LedGraph.Bar(pos.immutable(), idx));
                for (LedGraph.Bar b : run)
                    if (level.getBlockEntity(b.cell()) instanceof LedStripBlockEntity e) e.setConfig(b.idx(), cfg);
                if (player != null) player.displayClientMessage(Component.translatable("msg.ledz.applied", run.size()), true);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.5f);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) openScreen.accept(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        LedConfig c = get(stack);
        MutableComponent bar = Component.empty();
        for (int s : c.stops()) bar.append(Component.literal("██").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(s))));
        tip.add(bar);
        tip.add(Component.translatable("gui.ledz.mode." + c.mode()).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" · " + c.brightness() + "%")));
        tip.add(Component.translatable("tip.ledz.configurator.use").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.translatable("tip.ledz.configurator.apply").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.translatable("tip.ledz.configurator.copy").withStyle(ChatFormatting.DARK_GRAY));
    }
}
