package com.lach.lumend.client;

import com.lach.lumend.LedConfig;
import com.lach.lumend.LedConfiguratorItem;
import com.lach.lumend.net.SetConfigPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class ConfiguratorScreen extends Screen {
    private static final int W = 232, H = 208;
    private static final int PANEL = 0xF0101418, BORDER = 0xFF3A4652, LABEL = 0xFFB8C4CC;

    private final InteractionHand hand;
    private LedConfig cfg;
    private final List<Integer> stops;
    private int sel;
    private boolean syncing;

    private int x0, y0;
    private EditBox hex;
    private IntSlider r, g, b;

    public ConfiguratorScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("gui.lumend.configurator"));
        this.hand = hand;
        this.cfg = LedConfiguratorItem.get(stack);
        this.stops = new ArrayList<>(cfg.stops());
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        int x = x0 + 8, half = 106;
        sel = Math.min(sel, stops.size() - 1);

        addRenderableWidget(Button.builder(Component.literal("+"), bt -> {
            if (stops.size() < LedConfig.MAX_STOPS) { stops.add(sel + 1, stops.get(sel)); sel++; rebuildWidgets(); }
        }).bounds(x + 4 * 26, y0 + 36, 16, 16).build()).active = stops.size() < LedConfig.MAX_STOPS;
        addRenderableWidget(Button.builder(Component.literal("-"), bt -> {
            if (stops.size() > LedConfig.MIN_STOPS) { stops.remove(sel); sel = Math.max(0, sel - 1); rebuildWidgets(); }
        }).bounds(x + 4 * 26 + 18, y0 + 36, 16, 16).build()).active = stops.size() > LedConfig.MIN_STOPS;

        hex = new EditBox(font, x0 + W - 8 - 76, y0 + 36, 76, 16, Component.literal("hex"));
        hex.setMaxLength(7);
        hex.setValue(String.format(Locale.ROOT, "#%06X", stops.get(sel)));
        hex.setResponder(s -> {
            if (syncing) return;
            String h = s.startsWith("#") ? s.substring(1) : s;
            if (h.length() != 6) return;
            try {
                setStop(Integer.parseInt(h, 16), false);
            } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(hex);

        int c = stops.get(sel);
        r = addRenderableWidget(new IntSlider(x, y0 + 58, 70, 16, "gui.lumend.red", 0, 255, (c >> 16) & 255, v -> channel(16, v)));
        g = addRenderableWidget(new IntSlider(x + 73, y0 + 58, 70, 16, "gui.lumend.green", 0, 255, (c >> 8) & 255, v -> channel(8, v)));
        b = addRenderableWidget(new IntSlider(x + 146, y0 + 58, 70, 16, "gui.lumend.blue", 0, 255, c & 255, v -> channel(0, v)));

        addRenderableWidget(Button.builder(Component.translatable("gui.lumend.mode", Component.translatable("gui.lumend.mode." + cfg.mode())),
                bt -> { cfg = cfg.withMode((cfg.mode() + 1) % LedConfig.MODES); rebuildWidgets(); }).bounds(x, y0 + 102, half, 18).build());
        addRenderableWidget(new IntSlider(x + half + 4, y0 + 102, half, 18, "gui.lumend.speed", 1, 20, cfg.speed(), v -> cfg = cfg.withSpeed(v)));
        addRenderableWidget(new IntSlider(x, y0 + 124, half, 18, "gui.lumend.brightness", 0, 100, cfg.brightness(), v -> cfg = cfg.withBrightness(v)));
        addRenderableWidget(new IntSlider(x + half + 4, y0 + 124, half, 18, "gui.lumend.light", 0, 15, cfg.light(), v -> cfg = cfg.withLight(v)));
        addRenderableWidget(Button.builder(Component.translatable("gui.lumend.redstone", Component.translatable("gui.lumend.redstone." + cfg.redstone())),
                bt -> { cfg = cfg.withRedstone((cfg.redstone() + 1) % LedConfig.RS_MODES); rebuildWidgets(); }).bounds(x, y0 + 146, half, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(cfg.reverse() ? "gui.lumend.reverse.on" : "gui.lumend.reverse.off"),
                bt -> { cfg = cfg.withReverse(!cfg.reverse()); rebuildWidgets(); }).bounds(x + half + 4, y0 + 146, half, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), bt -> save()).bounds(x, y0 + H - 26, half, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), bt -> onClose()).bounds(x + half + 4, y0 + H - 26, half, 18).build());
    }

    private void channel(int shift, int v) {
        int c = stops.get(sel);
        c = (c & ~(255 << shift)) | (v << shift);
        setStop(c, true);
    }

    private void setStop(int rgb, boolean updateHex) {
        stops.set(sel, rgb & 0xFFFFFF);
        syncing = true;
        if (updateHex) hex.setValue(String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF));
        else {
            r.setInt((rgb >> 16) & 255);
            g.setInt((rgb >> 8) & 255);
            b.setInt(rgb & 255);
        }
        syncing = false;
    }

    private LedConfig current() {
        return cfg.withStops(List.copyOf(stops)).sanitized();
    }

    private void save() {
        ClientPacketDistributor.sendToServer(new SetConfigPayload(hand == InteractionHand.OFF_HAND, current()));
        onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        for (int k = 0; k < stops.size(); k++) {
            int sx = x0 + 8 + k * 26, sy = y0 + 36;
            if (mx >= sx && mx < sx + 22 && my >= sy && my < sy + 16) {
                sel = k;
                rebuildWidgets();
                return true;
            }
        }
        DyeColor[] dyes = DyeColor.values();
        for (int k = 0; k < dyes.length; k++) {
            int px = x0 + 8 + k * 13, py = y0 + 80;
            if (mx >= px && mx < px + 12 && my >= py && my < py + 12) {
                setStop(dyes[k].getTextureDiffuseColor() & 0xFFFFFF, false);
                hex.setValue(String.format(Locale.ROOT, "#%06X", stops.get(sel)));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gg, int mx, int my, float pt) {
        super.extractBackground(gg, mx, my, pt);
        gg.fill(x0 - 1, y0 - 1, x0 + W + 1, y0 + H + 1, BORDER);
        gg.fill(x0, y0, x0 + W, y0 + H, PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gg, int mx, int my, float pt) {
        super.extractRenderState(gg, mx, my, pt);
        gg.text(font, title, x0 + 8, y0 + 7, 0xFFFFFFFF, false);

        // animated preview
        LedConfig c = current();
        double secs = Util.getMillis() / 1000.0;
        int px = x0 + 8, pw = W - 16, py = y0 + 20;
        gg.fill(px - 1, py - 1, px + pw + 1, py + 11, BORDER);
        for (int i = 0; i < pw; i++) {
            int rgb = LedConfig.scale(c.color(i / (double) (pw - 1), secs), 0.15f + 0.85f * c.brightness() / 100f);
            gg.fill(px + i, py, px + i + 1, py + 10, 0xFF000000 | rgb);
        }

        // stops
        for (int k = 0; k < stops.size(); k++) {
            int sx = x0 + 8 + k * 26, sy = y0 + 36;
            gg.fill(sx - 1, sy - 1, sx + 23, sy + 17, k == sel ? 0xFFFFFFFF : BORDER);
            gg.fill(sx, sy, sx + 22, sy + 16, 0xFF000000 | stops.get(k));
        }

        // dye palette
        DyeColor[] dyes = DyeColor.values();
        for (int k = 0; k < dyes.length; k++) {
            int qx = x0 + 8 + k * 13, qy = y0 + 80;
            boolean hover = mx >= qx && mx < qx + 12 && my >= qy && my < qy + 12;
            gg.fill(qx - 1, qy - 1, qx + 13, qy + 13, hover ? 0xFFFFFFFF : BORDER);
            gg.fill(qx, qy, qx + 12, qy + 12, 0xFF000000 | (dyes[k].getTextureDiffuseColor() & 0xFFFFFF));
        }
        gg.text(font, Component.translatable("gui.lumend.hint"), x0 + 8, y0 + 168, LABEL, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static class IntSlider extends AbstractSliderButton {
        private final String key;
        private final int min, max;
        private final IntConsumer set;

        IntSlider(int x, int y, int w, int h, String key, int min, int max, int val, IntConsumer set) {
            super(x, y, w, h, Component.empty(), (val - min) / (double) (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.set = set;
            updateMessage();
        }

        int get() {
            return min + (int) Math.round(value * (max - min));
        }

        void setInt(int v) {
            value = (v - min) / (double) (max - min);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (key != null) setMessage(Component.translatable(key, get()));
        }

        @Override
        protected void applyValue() {
            set.accept(get());
        }
    }
}
