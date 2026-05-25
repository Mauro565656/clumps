package blamejared.clumps.gui;

import blamejared.clumps.ClumpsClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Kept for backwards compatibility (KeybindScreen / FriendsScreen still push this).
 * In normal use the Theme panel renders inside ModuleScreen directly.
 * This standalone version matches the Prestige layout with name + description cards.
 */
public class ThemeSelectorScreen extends Screen {

    private final Screen parent;

    private static final int CORNER_R = 18;
    private static final int PANEL_W  = 740;
    private static final int PANEL_H  = 540;
    private static final int COLS     = 3;
    private static final int CARD_W   = 210;
    private static final int CARD_H   = 115;
    private static final int GAP      = 16;

    // Accent colours matching Theme.java
    private static final int[] ACC1 = {
        0xFF00D5FF, 0xFFAA66FF, 0xFFFF6B9D, 0xFFFF8844, 0xFF66FF88, 0xFF6666FF,
        0xFFFF66FF, 0xFF00FFCC, 0xFFFF4444, 0xFF88FF44, 0xFFFFD700, 0xFF66DDFF
    };
    private static final int[] ACC2 = {
        0xFF0088AA, 0xFF6633AA, 0xFFAA3355, 0xFFAA5522, 0xFF33AA55, 0xFF3333AA,
        0xFFAA33AA, 0xFF00AA88, 0xFFAA2222, 0xFF55AA22, 0xFFAA8800, 0xFF3388AA
    };
    // One-line description per theme (same order as Theme.names())
    private static final String[] DESCRIPTIONS = {
        "Glass + cyan, Fristy default",
        "Synthwave violet glow",
        "Pink couture",
        "Molten amber glow",
        "Emerald canopy",
        "Twilight indigo",
        "Neon orchid",
        "Open-water sapphire",
        "Blood-velvet red",
        "Radioactive lime",
        "Luxury gilded glow",
        "Northern lights"
    };

    public ThemeSelectorScreen(Screen parent) {
        super(Component.literal("Select Theme"));
        this.parent = parent;
    }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme t   = Theme.current();
        int px    = (width  - PANEL_W) / 2;
        int py    = (height - PANEL_H) / 2;

        // Background glass gradient
        RenderHelper.fillDoubleGradient(g, 0, 0, width, height,
            0xEB020814, 0xE0041226, 0xF0000A19, 0xF0020E1E);

        // Panel
        RenderHelper.drawGlow(g, px, py, PANEL_W, PANEL_H, CORNER_R, 0x2E00B4FF);
        RenderHelper.fillRounded(g, px, py, PANEL_W, PANEL_H, CORNER_R, 0xF5030A19);
        RenderHelper.fillGradientVertical(g, px, py, PANEL_W, PANEL_H, 0x080C2848, 0x02000000);
        RenderHelper.drawGlassOverlay(g, px, py, PANEL_W, PANEL_H);
        RenderHelper.drawInnerBorder(g, px, py, PANEL_W, PANEL_H, CORNER_R, 0x295AB4FF);

        // Header
        g.centeredText(font, "\u00a7lThemes", width / 2, py + 18, t.white);
        g.centeredText(font, "\u00a77Customize the appearance of your interface", width / 2, py + 32, t.muted);
        g.fill(px + 24, py + 48, px + PANEL_W - 24, py + 49, 0x0CFFFFFF);

        // Grid
        String[] names  = Theme.names();
        int gridW  = COLS * CARD_W + (COLS - 1) * GAP;
        int startX = px + (PANEL_W - gridW) / 2;
        int startY = py + 58;

        for (int i = 0; i < names.length; i++) {
            int col = i % COLS, row = i / COLS;
            int cx2 = startX + col * (CARD_W + GAP);
            int cy2 = startY + row * (CARD_H + GAP);
            boolean sel   = Theme.currentName().equals(names[i]);
            boolean hover = mx >= cx2 && mx <= cx2 + CARD_W && my >= cy2 && my <= cy2 + CARD_H;

            // Card background
            RenderHelper.fillRounded(g, cx2, cy2, CARD_W, CARD_H, 12, 0xF0050C1E);
            RenderHelper.drawGlassOverlay(g, cx2, cy2, CARD_W, CARD_H);

            // Border / glow
            if (sel) {
                RenderHelper.drawInnerBorder(g, cx2, cy2, CARD_W, CARD_H, 12, 0x6600D5FF);
                RenderHelper.drawGlow(g, cx2, cy2, CARD_W, CARD_H, 12, 0x2000B4FF);
            } else if (hover) {
                RenderHelper.drawInnerBorder(g, cx2, cy2, CARD_W, CARD_H, 12, 0x2800D5FF);
            } else {
                RenderHelper.drawInnerBorder(g, cx2, cy2, CARD_W, CARD_H, 12, 0x1478B4FF);
            }

            // Colour strip preview
            int stripX = cx2 + 12, stripY = cy2 + 12, stripW = CARD_W - 24;
            RenderHelper.fillRounded(g, stripX, stripY, stripW, 28, 8, 0xFF0A1428);
            // Left dark accent block
            RenderHelper.fillRounded(g, stripX, stripY, 14, 28, 8, ACC2[i]);
            g.fill(stripX + 7, stripY, stripX + 14, stripY + 28, ACC2[i]);
            // Main accent
            g.fill(stripX + 14, stripY, stripX + stripW / 2, stripY + 28, ACC1[i]);
            // Dark right half
            g.fill(stripX + stripW / 2, stripY, stripX + stripW, stripY + 28, 0xFF0A1428);

            // GLASS tag (top-right of strip)
            int tagX = stripX + stripW - 42, tagY = stripY + 6;
            RenderHelper.fillRounded(g, tagX, tagY, 38, 16, 3, 0x1F78DCFF);
            g.text(font, "\u00a77GLASS", tagX + 4, tagY + 4, 0xD9B4F0FF);

            // Theme name
            g.text(font, names[i], cx2 + 12, cy2 + 48, sel ? 0xFFFFFFFF : 0xBFFFFFFF);

            // Description
            String desc = i < DESCRIPTIONS.length ? DESCRIPTIONS[i] : "color theme";
            g.text(font, "\u00a77" + desc, cx2 + 12, cy2 + 61, 0x80FFFFFF);

            // Selected checkmark
            if (sel) {
                g.text(font, "\u2713", cx2 + CARD_W - 16, cy2 + 48, ACC1[i]);
            }
        }

        // Back hint
        g.centeredText(font, "\u00a77Click a theme to apply \u00b7 ESC to go back",
            width / 2, py + PANEL_H - 14, t.muted);

        super.extractRenderState(g, mx, my, delta);
    }

    // ── Input ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean active) {
        double mx = event.x(), my = event.y();
        if (event.button() != 0) return super.mouseClicked(event, active);

        int px     = (width  - PANEL_W) / 2;
        int py     = (height - PANEL_H) / 2;
        String[] names  = Theme.names();
        int gridW  = COLS * CARD_W + (COLS - 1) * GAP;
        int startX = px + (PANEL_W - gridW) / 2;
        int startY = py + 58;

        for (int i = 0; i < names.length; i++) {
            int col = i % COLS, row = i / COLS;
            int cx2 = startX + col * (CARD_W + GAP);
            int cy2 = startY + row * (CARD_H + GAP);
            if (mx >= cx2 && mx <= cx2 + CARD_W && my >= cy2 && my <= cy2 + CARD_H) {
                Theme.setCurrent(names[i]);
                ClumpsClient.saveConfig();
                minecraft.setScreen(parent);
                return true;
            }
        }
        return super.mouseClicked(event, active);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
