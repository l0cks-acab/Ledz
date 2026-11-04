package com.lach.lumend.client;

import com.lach.lumend.LedConfiguratorItem;
import com.lach.lumend.Lumend;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Lumend.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(Lumend.LED_BE.get(), LedRenderer::new);
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent e) {
        LedConfiguratorItem.openScreen = hand -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.setScreen(new ConfiguratorScreen(hand, mc.player.getItemInHand(hand)));
        };
    }
}
