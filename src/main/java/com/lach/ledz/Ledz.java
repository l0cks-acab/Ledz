package com.lach.ledz;

import com.lach.ledz.net.SetConfigPayload;
import com.lach.ledz.net.SetPresetPayload;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Ledz.MODID)
public class Ledz {
    public static final String MODID = "ledz";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LedConfig>> LED_CONFIG = COMPONENTS.registerComponentType("led_config",
            b -> b.persistent(LedConfig.CODEC).networkSynchronized(LedConfig.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Presets>> PRESETS = COMPONENTS.registerComponentType("presets",
            b -> b.persistent(Presets.CODEC).networkSynchronized(Presets.STREAM_CODEC));

    public static final DeferredBlock<LedStripBlock> LED_BLOCK = BLOCKS.registerBlock("led_strip", LedStripBlock::new, LedStripBlock.PROPERTIES);
    public static final DeferredItem<LedStripItem> LED_STRIP = ITEMS.registerItem("led_strip", LedStripItem::new);
    public static final DeferredItem<LedConfiguratorItem> CONFIGURATOR = ITEMS.registerItem("led_configurator", LedConfiguratorItem::new,
            new Item.Properties().stacksTo(1));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LedStripBlockEntity>> LED_BE = BLOCK_ENTITIES.register("led_strip",
            () -> BlockEntityType.Builder.of(LedStripBlockEntity::new, LED_BLOCK.get()).build(null));

    public static final DeferredBlock<LedLampBlock> LED_PANEL = BLOCKS.registerBlock("led_panel", p -> new LedLampBlock(false, p), LedLampBlock.PROPERTIES);
    public static final DeferredBlock<LedLampBlock> LED_SPOT = BLOCKS.registerBlock("led_spot", p -> new LedLampBlock(true, p), LedLampBlock.PROPERTIES);
    public static final DeferredBlock<LightControllerBlock> CONTROLLER_BLOCK = BLOCKS.registerBlock("light_controller", LightControllerBlock::new, LightControllerBlock.PROPERTIES);
    public static final DeferredItem<BlockItem> LED_PANEL_ITEM = ITEMS.registerSimpleBlockItem(LED_PANEL);
    public static final DeferredItem<BlockItem> LED_SPOT_ITEM = ITEMS.registerSimpleBlockItem(LED_SPOT);
    public static final DeferredItem<BlockItem> CONTROLLER_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LedLampBlockEntity>> LAMP_BE = BLOCK_ENTITIES.register("led_lamp",
            () -> BlockEntityType.Builder.of(LedLampBlockEntity::new, LED_PANEL.get(), LED_SPOT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LightControllerBlockEntity>> CONTROLLER_BE = BLOCK_ENTITIES.register("light_controller",
            () -> BlockEntityType.Builder.of(LightControllerBlockEntity::new, CONTROLLER_BLOCK.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID))
            .withTabsBefore(CreativeModeTabs.COLORED_BLOCKS)
            .icon(() -> new ItemStack(CONFIGURATOR.get()))
            .displayItems((params, out) -> {
                out.accept(LED_STRIP.get());
                out.accept(LED_PANEL_ITEM.get());
                out.accept(LED_SPOT_ITEM.get());
                out.accept(CONTROLLER_ITEM.get());
                out.accept(CONFIGURATOR.get());
            })
            .build());

    public Ledz(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(Ledz::registerPayloads);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(SetConfigPayload.TYPE, SetConfigPayload.STREAM_CODEC, (p, ctx) -> {
            ItemStack stack = ctx.player().getItemInHand(p.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (stack.is(CONFIGURATOR.get())) stack.set(LED_CONFIG.get(), p.config().sanitized());
        });
        r.playToServer(SetPresetPayload.TYPE, SetPresetPayload.STREAM_CODEC, (p, ctx) -> {
            ItemStack stack = ctx.player().getItemInHand(p.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!stack.is(CONFIGURATOR.get())) return;
            Presets cur = stack.getOrDefault(PRESETS.get(), Presets.EMPTY);
            stack.set(PRESETS.get(), cur.with(p.slot(), p.config().orElse(null)));
        });
    }
}
