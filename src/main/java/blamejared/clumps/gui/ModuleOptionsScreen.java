package blamejared.clumps.gui;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.ClumpsClient;
import blamejared.clumps.Option;
import blamejared.clumps.modules.BlockESP;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ModuleOptionsScreen extends Screen {

    private static final int PANEL_W = 420;
    private static final int ROW_H = 28;
    private static final int BOX_W = 50;
    private static final int SLIDER_W = 200;
    private static final int CORNER_R = 18;

    private final ClumpsModule module;
    private final Screen parent;
    private final List<ScrollEntry> scrollWidgets = new ArrayList<>();
    private Button keybindButton;
    private boolean waitingForKeybind = false;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private int panelX, panelY, panelH;
    private int listTop, listBottom;

    public ModuleOptionsScreen(ClumpsModule module, Screen parent) {
        super(Component.literal(module.getName() + " Options"));
        this.module = module;
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelX = width / 2 - PANEL_W / 2;
        panelY = (int)(height * 0.12f);
        panelH = (int)(height * 0.76f);
        listTop = panelY + 8;
        listBottom = panelY + panelH - 4;

        clearWidgets();
        scrollWidgets.clear();

        int y = listTop + 4;

        keybindButton = addRenderableWidget(Button.builder(
                Component.literal(getKeybindLabel()),
                btn -> { waitingForKeybind = true; btn.setMessage(Component.literal("Press a key...")); }
        ).bounds(panelX + 20, y, PANEL_W - 40, ROW_H - 2).build());
        scrollWidgets.add(new ScrollEntry(keybindButton, y));
        y += ROW_H + 4;

        for (Option<?> opt : module.getOptions()) {
            if (opt instanceof Option.IntOption intOpt) {
                y = addIntOptionRow(y, intOpt);
            } else if (opt instanceof Option.DoubleOption dblOpt) {
                y = addDoubleOptionRow(y, dblOpt);
            } else if (opt instanceof Option.BoolOption boolOpt) {
                AbstractWidget btn = addRenderableWidget(Button.builder(
                        Component.literal(boolOpt.getName() + ": " + boolOpt.getDisplayValue()),
                        b -> { boolOpt.next(); ClumpsClient.saveConfig(); b.setMessage(Component.literal(boolOpt.getName() + ": " + boolOpt.getDisplayValue())); }
                ).bounds(panelX + 20, y, PANEL_W - 40, ROW_H - 2).build());
                scrollWidgets.add(new ScrollEntry(btn, y));
                y += ROW_H + 4;
            } else {
                final Option<?> o = opt;
                AbstractWidget btn = addRenderableWidget(Button.builder(
                        Component.literal(o.getName() + ": " + o.getDisplayValue()),
                        b -> { o.next(); ClumpsClient.saveConfig(); b.setMessage(Component.literal(o.getName() + ": " + o.getDisplayValue())); }
                ).bounds(panelX + 20, y, PANEL_W - 40, ROW_H - 2).build());
                scrollWidgets.add(new ScrollEntry(btn, y));
                y += ROW_H + 4;
            }
        }

        if (module instanceof BlockESP blockESP) {
            AbstractWidget customBtn = addRenderableWidget(Button.builder(
                    Component.literal("Pick Blocks..."),
                    btn -> minecraft.setScreen(new BlockPickerScreen(blockESP, this))
            ).bounds(panelX + 20, y, PANEL_W - 40, ROW_H - 2).build());
            scrollWidgets.add(new ScrollEntry(customBtn, y));
            y += ROW_H + 4;
        }

        maxScroll = Math.max(0, y - listBottom);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
        updateScrollLayout();

        addRenderableWidget(Button.builder(
                Component.literal("\u2190 Back"),
                btn -> minecraft.setScreen(parent)
        ).bounds(width / 2 - 50, height - 30, 100, 20).build());
    }

    private int addIntOptionRow(int y, Option.IntOption intOpt) {
        int min = intOpt.getMin();
        int max = intOpt.getMax();
        double initial = (double) (intOpt.getValue() - min) / (max - min);
        int sliderX = panelX + 20;
        int boxX = sliderX + SLIDER_W + 4;

        EditBox box = new EditBox(font, boxX, y, BOX_W, ROW_H - 2, Component.literal(intOpt.getName()));
        box.setMaxLength(8);
        box.setValue(String.valueOf(intOpt.getValue()));
        box.setBordered(false);
        box.setTextColor(0xFFFFFFFF);

        AbstractSliderButton slider = new AbstractSliderButton(
                sliderX, y, SLIDER_W, ROW_H - 2,
                Component.literal(intOpt.getName() + ": " + intOpt.getValue()), initial) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(intOpt.getName() + ": " + intOpt.getValue()));
            }

            @Override
            protected void applyValue() {
                int snapped = Mth.clamp(min + Math.round((float) (value * (max - min))), min, max);
                intOpt.setValue(snapped);
                ClumpsClient.saveConfig();
                updateMessage();
                box.setValue(String.valueOf(intOpt.getValue()));
            }
        };

        box.setResponder(text -> {
            try {
                int parsed = Integer.parseInt(text.trim());
                int clamped = Mth.clamp(parsed, min, max);
                intOpt.setValue(clamped);
                ClumpsClient.saveConfig();
                slider.setMessage(Component.literal(intOpt.getName() + ": " + intOpt.getValue()));
            } catch (NumberFormatException ignored) {}
        });

        addRenderableWidget(slider);
        addRenderableWidget(box);
        scrollWidgets.add(new ScrollEntry(slider, y));
        scrollWidgets.add(new ScrollEntry(box, y));

        return y + ROW_H + 4;
    }

    private int addDoubleOptionRow(int y, Option.DoubleOption dblOpt) {
        double min = dblOpt.getMin();
        double max = dblOpt.getMax();
        double initial = (max == min) ? 0.0 : (dblOpt.getValue() - min) / (max - min);
        int wideBox = 110;
        int wideSliderW = PANEL_W - 40 - wideBox - 4;
        int sliderX = panelX + 20;
        int boxX = sliderX + wideSliderW + 4;

        EditBox box = new EditBox(font, boxX, y, wideBox, ROW_H - 2, Component.literal(dblOpt.getName()));
        box.setMaxLength(32);
        box.setValue(dblOpt.getDisplayValue());
        box.setBordered(false);
        box.setTextColor(0xFFFFFFFF);

        AbstractSliderButton slider = new AbstractSliderButton(
                sliderX, y, wideSliderW, ROW_H - 2,
                Component.literal(dblOpt.getName() + ": " + dblOpt.getDisplayValue()),
                Mth.clamp(initial, 0.0, 1.0)) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(dblOpt.getName() + ": " + dblOpt.getDisplayValue()));
            }

            @Override
            protected void applyValue() {
                double raw = min + value * (max - min);
                double step = dblOpt.getStep();
                double snapped = step > 0 ? Math.round(raw / step) * step : raw;
                if (snapped < min) snapped = min;
                if (snapped > max) snapped = max;
                dblOpt.setValue(snapped);
                ClumpsClient.saveConfig();
                updateMessage();
                box.setValue(dblOpt.getDisplayValue());
            }
        };

        box.setResponder(text -> {
            String t = text.trim();
            if (t.isEmpty() || t.equals("-") || t.equals(".") || t.equals("-.")) return;
            try {
                double parsed = Double.parseDouble(t);
                if (Double.isNaN(parsed) || Double.isInfinite(parsed)) return;
                double clamped = parsed;
                if (clamped < min) clamped = min;
                if (clamped > max) clamped = max;
                dblOpt.setValue(clamped);
                ClumpsClient.saveConfig();
                slider.setMessage(Component.literal(dblOpt.getName() + ": " + dblOpt.getDisplayValue()));
            } catch (NumberFormatException ignored) {}
        });

        addRenderableWidget(slider);
        addRenderableWidget(box);
        scrollWidgets.add(new ScrollEntry(slider, y));
        scrollWidgets.add(new ScrollEntry(box, y));

        return y + ROW_H + 4;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (waitingForKeybind && module.keybind != null) {
            waitingForKeybind = false;
            module.keybind.setKey(event.key() == 256
                    ? InputConstants.UNKNOWN
                    : InputConstants.getKey(event));
            KeyMapping.resetMapping();
            ClumpsClient.saveConfig();
            keybindButton.setMessage(Component.literal(getKeybindLabel()));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (maxScroll > 0) {
            scrollOffset = Math.clamp(scrollOffset - (int) (sy * 20.0), 0, maxScroll);
            updateScrollLayout();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private String getKeybindLabel() {
        if (module.keybind == null) return "Keybind: None";
        String name = module.keybind.getTranslatedKeyMessage().getString();
        return "Keybind: " + (name.isEmpty() ? "None" : name);
    }

    private void updateScrollLayout() {
        for (ScrollEntry e : scrollWidgets) {
            int sy = e.baseY - scrollOffset;
            e.widget.setY(sy);
            boolean vis = sy + e.widget.getHeight() > listTop && sy < listBottom;
            e.widget.visible = vis;
            e.widget.active = vis;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme t = Theme.current();

        RenderHelper.fillDoubleGradient(g, 0, 0, width, height,
            0xEB020814, 0xE0041226, 0xF0000A19, 0xF0020E1E);

        RenderHelper.drawGlow(g, panelX, panelY, PANEL_W, panelH, CORNER_R, 0x2E00B4FF);

        RenderHelper.fillRounded(g, panelX, panelY, PANEL_W, panelH, CORNER_R, 0xF5030A19);
        RenderHelper.fillGradientVertical(g, panelX, panelY, PANEL_W, panelH, 0xF5030A19, 0xED020F23);
        RenderHelper.drawGlassOverlay(g, panelX, panelY, PANEL_W, panelH);
        RenderHelper.drawInnerBorder(g, panelX, panelY, PANEL_W, panelH, CORNER_R, 0x295AB4FF);

        g.centeredText(font, "\u2699\u00a7l " + module.getName() + " \u00a77Options", width / 2, panelY - 14, t.white);

        super.extractRenderState(g, mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record ScrollEntry(AbstractWidget widget, int baseY) {}
}
