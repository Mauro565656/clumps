package blamejared.clumps.gui;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.ClumpsClient;
import blamejared.clumps.Option;
import blamejared.clumps.modules.BlockESP;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
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

    // Prestige-style: full sidebar + wide content panel, not a floating popup
    private static final int SIDEBAR_W      = 210;
    private static final int CORNER_R       = 18;
    private static final int BASE_W         = 955;
    private static final int BASE_H         = 635;
    private static final int SIDEBAR_PAD_X  = 14;
    private static final int SIDEBAR_PAD_TOP = 20;
    private static final int ROW_H          = 42;
    private static final int SLIDER_W_FRAC  = 55; // % of row width for slider
    private static final int BOX_W          = 80;

    private final ClumpsModule module;
    private final Screen parent;
    private final List<ScrollEntry> scrollWidgets = new ArrayList<>();
    private Button keybindButton;
    private boolean waitingForKeybind = false;
    private int scrollOffset = 0, maxScroll = 0;

    // Computed layout
    private int wx, wy, ww, wh;
    private int cx, cy, cw, ch;
    private int listTop, listBottom;
    private int panelX; // sidebar left

    public ModuleOptionsScreen(ClumpsModule module, Screen parent) {
        super(Component.literal(module.getName() + " Options"));
        this.module = module;
        this.parent = parent;
    }

    private int s(int v) { return Math.max(1, (int)(v * ((float)ww / BASE_W))); }

    @Override
    protected void init() {
        ww = Math.min(BASE_W, (int)(width  * 0.90f)); wh = Math.min(BASE_H, (int)(height * 0.90f));
        ww = Math.max(ww, 700);                        wh = Math.max(wh, 500);
        wx = (width - ww) / 2; wy = (height - wh) / 2;
        cx = wx + s(SIDEBAR_W); cy = wy; cw = ww - s(SIDEBAR_W); ch = wh;
        listTop    = cy + s(62);
        listBottom = cy + ch - s(12);
        panelX     = wx;

        clearWidgets();
        scrollWidgets.clear();

        int rx = cx + s(22), rowW = cw - s(44);
        int y = listTop + s(4);

        // Keybind row
        keybindButton = addRenderableWidget(Button.builder(
                Component.literal(getKeybindLabel()),
                btn -> { waitingForKeybind = true; btn.setMessage(Component.literal("Press a key...")); }
        ).bounds(rx, y, rowW, s(30)).build());
        scrollWidgets.add(new ScrollEntry(keybindButton, y));
        y += s(ROW_H);

        // Options
        for (Option<?> opt : module.getOptions()) {
            if (opt instanceof Option.IntOption io)     { y = addIntRow(y, io, rx, rowW); }
            else if (opt instanceof Option.DoubleOption dbl) { y = addDblRow(y, dbl, rx, rowW); }
            else if (opt instanceof Option.BoolOption bo) {
                AbstractWidget btn = addRenderableWidget(Button.builder(
                        Component.literal(bo.getName() + ": " + bo.getDisplayValue()),
                        b -> { bo.next(); ClumpsClient.saveConfig(); b.setMessage(Component.literal(bo.getName() + ": " + bo.getDisplayValue())); }
                ).bounds(rx, y, rowW, s(30)).build());
                scrollWidgets.add(new ScrollEntry(btn, y));
                y += s(ROW_H);
            } else {
                final Option<?> o = opt;
                AbstractWidget btn = addRenderableWidget(Button.builder(
                        Component.literal(o.getName() + ": " + o.getDisplayValue()),
                        b -> { o.next(); ClumpsClient.saveConfig(); b.setMessage(Component.literal(o.getName() + ": " + o.getDisplayValue())); }
                ).bounds(rx, y, rowW, s(30)).build());
                scrollWidgets.add(new ScrollEntry(btn, y));
                y += s(ROW_H);
            }
        }

        if (module instanceof BlockESP esp) {
            AbstractWidget btn = addRenderableWidget(Button.builder(
                    Component.literal("Pick Blocks..."),
                    b -> minecraft.setScreen(new BlockPickerScreen(esp, this))
            ).bounds(rx, y, rowW, s(30)).build());
            scrollWidgets.add(new ScrollEntry(btn, y));
            y += s(ROW_H);
        }

        maxScroll    = Math.max(0, y - listBottom);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
        updateScrollLayout();

        // Back button — fixed position outside scissor area
        addRenderableWidget(Button.builder(
                Component.literal("\u2190 Back"),
                btn -> minecraft.setScreen(parent)
        ).bounds(cx + s(22), cy + s(20), s(60), s(24)).build());
    }

    private int addIntRow(int y, Option.IntOption io, int rx, int rowW) {
        int min = io.getMin(), max = io.getMax();
        int sw = rowW - BOX_W - s(6), bx = rx + sw + s(6);
        double init = (double)(io.getValue() - min) / (max - min);
        EditBox box = new EditBox(font, bx, y, BOX_W, s(30), Component.literal(io.getName()));
        box.setMaxLength(8); box.setValue(String.valueOf(io.getValue()));
        box.setBordered(false); box.setTextColor(0xFFFFFFFF);
        AbstractSliderButton slider = new AbstractSliderButton(rx, y, sw, s(30),
                Component.literal(io.getName() + ": " + io.getValue()), init) {
            @Override protected void updateMessage() { setMessage(Component.literal(io.getName() + ": " + io.getValue())); }
            @Override protected void applyValue() {
                io.setValue(Mth.clamp(min + Math.round((float)(value*(max-min))), min, max));
                ClumpsClient.saveConfig(); updateMessage(); box.setValue(String.valueOf(io.getValue()));
            }
        };
        box.setResponder(txt -> { try { io.setValue(Mth.clamp(Integer.parseInt(txt.trim()), min, max)); ClumpsClient.saveConfig(); slider.setMessage(Component.literal(io.getName()+": "+io.getValue())); } catch (NumberFormatException ignored) {} });
        addRenderableWidget(slider); addRenderableWidget(box);
        scrollWidgets.add(new ScrollEntry(slider, y)); scrollWidgets.add(new ScrollEntry(box, y));
        return y + s(ROW_H);
    }

    private int addDblRow(int y, Option.DoubleOption dbl, int rx, int rowW) {
        double min = dbl.getMin(), max = dbl.getMax();
        int wb = 100, sw = rowW - wb - s(6), bx = rx + sw + s(6);
        double init = max == min ? 0.0 : Mth.clamp((dbl.getValue()-min)/(max-min), 0.0, 1.0);
        EditBox box = new EditBox(font, bx, y, wb, s(30), Component.literal(dbl.getName()));
        box.setMaxLength(32); box.setValue(dbl.getDisplayValue());
        box.setBordered(false); box.setTextColor(0xFFFFFFFF);
        AbstractSliderButton slider = new AbstractSliderButton(rx, y, sw, s(30),
                Component.literal(dbl.getName() + ": " + dbl.getDisplayValue()), init) {
            @Override protected void updateMessage() { setMessage(Component.literal(dbl.getName() + ": " + dbl.getDisplayValue())); }
            @Override protected void applyValue() {
                double raw = min + value*(max-min), step = dbl.getStep();
                double snapped = step > 0 ? Math.round(raw/step)*step : raw;
                snapped = Math.max(min, Math.min(max, snapped));
                dbl.setValue(snapped); ClumpsClient.saveConfig(); updateMessage(); box.setValue(dbl.getDisplayValue());
            }
        };
        box.setResponder(txt -> { String t2=txt.trim(); if(t2.isEmpty()||t2.equals("-")||t2.equals(".")||t2.equals("-.")) return;
            try { double p=Double.parseDouble(t2); if(!Double.isFinite(p)) return; dbl.setValue(Math.max(min,Math.min(max,p))); ClumpsClient.saveConfig(); slider.setMessage(Component.literal(dbl.getName()+": "+dbl.getDisplayValue())); } catch(NumberFormatException ignored){} });
        addRenderableWidget(slider); addRenderableWidget(box);
        scrollWidgets.add(new ScrollEntry(slider, y)); scrollWidgets.add(new ScrollEntry(box, y));
        return y + s(ROW_H);
    }

    private void updateScrollLayout() {
        for (ScrollEntry e : scrollWidgets) {
            int sy = e.baseY - scrollOffset;
            e.widget.setY(sy);
            boolean vis = sy + e.widget.getHeight() > listTop && sy < listBottom;
            e.widget.visible = vis; e.widget.active = vis;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (waitingForKeybind && module.keybind != null) {
            waitingForKeybind = false;
            module.keybind.setKey(event.key() == 256 ? InputConstants.UNKNOWN : InputConstants.getKey(event));
            KeyMapping.resetMapping(); ClumpsClient.saveConfig();
            keybindButton.setMessage(Component.literal(getKeybindLabel()));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (maxScroll > 0 && mx >= cx) {
            scrollOffset = Math.clamp(scrollOffset - (int)(sy * s(20)), 0, maxScroll);
            updateScrollLayout(); return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private String getKeybindLabel() {
        if (module.keybind == null) return "Keybind: None";
        String n = module.keybind.getTranslatedKeyMessage().getString();
        return "Keybind: " + (n.isEmpty() ? "None" : n);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme t = Theme.current();

        // Glass background
        RenderHelper.fillDoubleGradient(g, 0, 0, width, height, 0xEB020814, 0xE0041226, 0xF0000A19, 0xF0020E1E);

        // Window
        RenderHelper.drawGlow(g, wx, wy, ww, wh, CORNER_R, 0x2E00B4FF);
        RenderHelper.fillRounded(g, wx, wy, ww, wh, CORNER_R, 0xF5030A19);
        RenderHelper.fillGradientVertical(g, wx, wy, ww, wh, 0x080C2848, 0x02000000);
        RenderHelper.drawGlassOverlay(g, wx, wy, ww, wh);
        RenderHelper.drawInnerBorder(g, wx, wy, ww, wh, CORNER_R, 0x295AB4FF);

        // Sidebar
        RenderHelper.fillRounded(g, wx, wy, s(SIDEBAR_W), wh, CORNER_R, 0xE2020816);
        RenderHelper.drawGlassOverlay(g, wx, wy, s(SIDEBAR_W), wh);
        RenderHelper.drawInnerBorder(g, wx, wy, s(SIDEBAR_W), wh, CORNER_R, 0x1A78B4FF);
        g.fill(wx+s(SIDEBAR_W)-1, wy+s(20), wx+s(SIDEBAR_W), wy+wh-s(20), 0x2200B4FF);

        // Sidebar brand
        int lx = wx+s(SIDEBAR_PAD_X), ly = wy+s(SIDEBAR_PAD_TOP);
        g.text(font, "\u25C6", lx, ly+s(4), t.primary);
        g.text(font, "\u00a7lFristy \u00a7r\u00a7lClient", lx+s(12), ly, t.white);
        g.text(font, "\u00a77BETA RELEASE", lx, ly+s(18), t.muted);
        int div1 = wy+s(SIDEBAR_PAD_TOP)+s(44)+s(8);
        g.fill(lx, div1, wx+s(SIDEBAR_W)-s(SIDEBAR_PAD_X), div1+1, 0x0CFFFFFF);
        // Show module name in sidebar
        g.text(font, "\u00a77Current module:", lx, div1+s(14), t.muted);
        g.text(font, "\u00a7l" + module.getName(), lx, div1+s(26), t.white);

        // Content header — Prestige style title + subtitle
        int hx = cx+s(22), hy = cy+s(18);
        g.text(font, "\u2699 \u00a7l" + module.getName() + " Options", hx, hy, t.white);
        g.text(font, "\u00a77Right-click to reset \u00b7 Scroll for more options", hx, hy+s(13), t.muted);
        int sepY = hy+s(32);
        g.fill(hx, sepY, cx+cw-s(22), sepY+1, 0x0CFFFFFF);

        // Scissored options area
        g.enableScissor(cx, listTop, cx+cw, listBottom);
        super.extractRenderState(g, mx, my, delta);
        g.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int viewH = listBottom-listTop, sbH = Math.max(s(20), viewH*viewH/(viewH+maxScroll));
            int sbY = listTop + (int)((long)scrollOffset*(viewH-sbH)/Math.max(1,maxScroll));
            g.fill(cx+cw-s(5), listTop, cx+cw-s(2), listBottom, 0x10FFFFFF);
            g.fill(cx+cw-s(5), sbY, cx+cw-s(2), sbY+sbH, 0x3000D5FF);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    private record ScrollEntry(AbstractWidget widget, int baseY) {}
}
