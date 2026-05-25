package blamejared.clumps.gui;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.ClumpsModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class KeybindScreen extends Screen {

    private final Screen parent;
    private int scrollOffset = 0;
    private boolean waitingForGuiKey = false;

    private static final int ROW_H = 28;
    private static final int PANEL_W = 480;
    private static final int PANEL_PAD = 24;
    private static final int CORNER_R = 18;

    public KeybindScreen(Screen parent) {
        super(Component.literal("Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int cx = width / 2;
        int px = cx - PANEL_W / 2;
        int btnW = PANEL_W - PANEL_PAD * 2;
        int y = 80;
        int btnH = 24;

        addRenderableWidget(Button.builder(
                Component.literal("Open Menu: " + getGuiKeyName()),
                btn -> { waitingForGuiKey = true; btn.setMessage(Component.literal("Press a key...")); }
        ).bounds(px + PANEL_PAD, y, btnW, btnH).build());
        y += ROW_H + 6;

        for (ClumpsModule m : ClumpsClient.modules) {
            String label = m.getName() + ": " + getKeybindName(m);
            int finalY = y;
            addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> {
                        if (m.keybind != null) {
                            m.keybind.setKey(InputConstants.UNKNOWN);
                            KeyMapping.resetMapping();
                            ClumpsClient.saveConfig();
                            init();
                        }
                    }
            ).bounds(px + PANEL_PAD, finalY, btnW, btnH).build());
            y += ROW_H;
        }
    }

    private String getGuiKeyName() {
        return ClumpsClient.guiKey.getTranslatedKeyMessage().getString();
    }

    private String getKeybindName(ClumpsModule m) {
        if (m.keybind == null) return "None";
        String name = m.keybind.getTranslatedKeyMessage().getString();
        return name.isEmpty() ? "None" : name;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (waitingForGuiKey) {
            waitingForGuiKey = false;
            if (event.key() != 256) {
                ClumpsClient.guiKey.setKey(InputConstants.getKey(event));
                KeyMapping.resetMapping();
                ClumpsClient.saveConfig();
            }
            init();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme t = Theme.current();
        int cx = width / 2;
        int px = cx - PANEL_W / 2;
        int py = 30;
        int ph = Math.min(height - 80, ClumpsClient.modules.size() * ROW_H + 120);

        fillDoubleGradient(g, 0, 0, width, height, 0xEB020814, 0xE0041226, 0xF0000A19, 0xF0020E1E);

        RenderHelper.drawGlow(g, px, py, PANEL_W, ph, CORNER_R, t.glow);
        RenderHelper.fillRounded(g, px, py, PANEL_W, ph, CORNER_R, 0xE80A1428);
        RenderHelper.fillGradientVertical(g, px, py, PANEL_W, ph, 0x100C2848, 0x02000000);
        RenderHelper.drawGlassOverlay(g, px, py, PANEL_W, ph);
        RenderHelper.drawInnerBorder(g, px, py, PANEL_W, ph, CORNER_R, 0x2200D5FF);

        g.centeredText(font, "\u00a7lKeybinds", cx, py + 20, t.white);
        g.centeredText(font, "\u00a77Click a keybind to clear it", cx, py + 36, t.gray);

        g.fill(px + PANEL_PAD, py + 56, px + PANEL_W - PANEL_PAD, py + 57, 0x0CFFFFFF);

        super.extractRenderState(g, mx, my, delta);
    }

    private static void fillDoubleGradient(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                            int tl, int tr, int bl, int br) {
        for (int row = 0; row < h; row++) {
            float t = (float) row / h;
            int a1 = (tl >> 24) & 0xFF, r1 = (tl >> 16) & 0xFF, g1 = (tl >> 8) & 0xFF, b1 = tl & 0xFF;
            int a2 = (bl >> 24) & 0xFF, r2 = (bl >> 16) & 0xFF, g2 = (bl >> 8) & 0xFF, b2 = bl & 0xFF;
            int aL = (int)(a1 + (a2 - a1) * t), rL = (int)(r1 + (r2 - r1) * t), gL = (int)(g1 + (g2 - g1) * t), bL = (int)(b1 + (b2 - b1) * t);
            int left = (aL << 24) | (rL << 16) | (gL << 8) | bL;
            a1 = (tr >> 24) & 0xFF; r1 = (tr >> 16) & 0xFF; g1 = (tr >> 8) & 0xFF; b1 = tr & 0xFF;
            a2 = (br >> 24) & 0xFF; r2 = (br >> 16) & 0xFF; g2 = (br >> 8) & 0xFF; b2 = br & 0xFF;
            aL = (int)(a1 + (a2 - a1) * t); rL = (int)(r1 + (r2 - r1) * t); gL = (int)(g1 + (g2 - g1) * t); bL = (int)(b1 + (b2 - b1) * t);
            int right = (aL << 24) | (rL << 16) | (gL << 8) | bL;
            RenderHelper.fillGradientHorizontal(g, x, y + row, w, 1, left, right);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
