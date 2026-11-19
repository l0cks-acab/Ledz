package com.lach.ledz.client;

import com.lach.ledz.LedConfiguratorItem;
import com.lach.ledz.Ledz;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import com.lach.ledz.LightControllerBlock;
import net.minecraft.world.item.DyeColor;

@EventBusSubscriber(modid = Ledz.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(Ledz.LED_BE.get(), LedRenderer::new);
        e.registerBlockEntityRenderer(Ledz.LAMP_BE.get(), LampRenderer::new);
    }

    @SubscribeEvent
    static void blockColors(RegisterColorHandlersEvent.Block e) {
        e.register((state, level, pos, tint) -> {
            int ch = state.getValue(LightControllerBlock.CHANNEL);
            return tint == 0 && ch < 16 ? 0xFF000000 | DyeColor.byId(ch).getTextureDiffuseColor() : 0xFF5A6470;
        }, Ledz.CONTROLLER_BLOCK.get());
    }

    @SubscribeEvent
    static void itemColors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> 0xFF5A6470, Ledz.CONTROLLER_ITEM.get());
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent e) {
        LedConfiguratorItem.openScreen = hand -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.setScreen(new ConfiguratorScreen(hand, mc.player.getItemInHand(hand)));
        };
    }
}
