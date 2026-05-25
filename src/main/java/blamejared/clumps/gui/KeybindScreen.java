package blamejared.clumps.gui;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.ClumpsModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class KeybindScreen extends Screen {

    private final Screen parent;
    private static final int CORNER_R       = 18;
    private static final int SIDEBAR_W      = 210;
    private static final int BASE_W         = 955;
    private static final int BASE_H         = 635;
    private static final int SIDEBAR_PAD_X  = 14;
    private static final int SIDEBAR_PAD_TOP = 20;
    private static final int ROW_H          = 46;

    private boolean waitingForGuiKey = false;
    private ClumpsModule waitingModule = null;
    private int scrollOffset = 0, maxScroll = 0;

    private int wx, wy, ww, wh, cx, cy, cw, ch;

    public KeybindScreen(Screen parent) {
        super(Component.literal("Keybinds"));
        this.parent = parent;
    }

    private int s(int v) { return Math.max(1, (int)(v * ((float)ww / BASE_W))); }

    @Override
    protected void init() {
        ww = Math.min(BASE_W, (int)(width  * 0.90f)); wh = Math.min(BASE_H, (int)(height * 0.90f));
        ww = Math.max(ww, 700);                        wh = Math.max(wh, 500);
        wx = (width-ww)/2; wy = (height-wh)/2;
        cx = wx+s(SIDEBAR_W); cy = wy; cw = ww-s(SIDEBAR_W); ch = wh;
        int totalRows = 1 + ClumpsClient.modules.size();
        int contentH  = s(62) + totalRows * s(ROW_H) + s(10);
        maxScroll = Math.max(0, contentH - ch);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx >= cx && maxScroll > 0) {
            scrollOffset = Math.clamp(scrollOffset - (int)(sy * s(ROW_H)), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean active) {
        double mx = event.x(), my = event.y();
        if (event.button() != 0) return super.mouseClicked(event, active);
        int rx = cx+s(22), rowW = cw-s(44);
        int baseY = cy + s(62) - scrollOffset;
        int ry = baseY;

        // GUI key row
        if (mx >= rx && mx <= rx+rowW && my >= ry && my <= ry+s(38)) {
            int editX = rx+rowW-s(80);
            if (mx >= editX) { waitingForGuiKey = true; waitingModule = null; }
            else { ClumpsClient.guiKey.setKey(InputConstants.UNKNOWN); KeyMapping.resetMapping(); ClumpsClient.saveConfig(); }
            return true;
        }
        ry += s(ROW_H);

        for (ClumpsModule m : ClumpsClient.modules) {
            if (mx >= rx && mx <= rx+rowW && my >= ry && my <= ry+s(38)) {
                int editX = rx+rowW-s(80);
                if (mx >= editX) { waitingModule = m; waitingForGuiKey = false; }
                else if (m.keybind != null) { m.keybind.setKey(InputConstants.UNKNOWN); KeyMapping.resetMapping(); ClumpsClient.saveConfig(); }
                return true;
            }
            ry += s(ROW_H);
        }
        return super.mouseClicked(event, active);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (waitingForGuiKey) {
            if (event.key() != 256) { ClumpsClient.guiKey.setKey(InputConstants.getKey(event)); KeyMapping.resetMapping(); ClumpsClient.saveConfig(); }
            waitingForGuiKey = false; return true;
        }
        if (waitingModule != null && waitingModule.keybind != null) {
            waitingModule.keybind.setKey(event.key() == 256 ? InputConstants.UNKNOWN : InputConstants.getKey(event));
            KeyMapping.resetMapping(); ClumpsClient.saveConfig(); waitingModule = null; return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme t = Theme.current();
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
        int lx = wx+s(SIDEBAR_PAD_X), ly = wy+s(SIDEBAR_PAD_TOP);
        g.text(font, "\u25C6", lx, ly+s(4), t.primary);
        g.text(font, "\u00a7lFristy \u00a7r\u00a7lClient", lx+s(12), ly, t.white);
        g.text(font, "\u00a77BETA RELEASE", lx, ly+s(18), t.muted);
        // Back link in sidebar
        int div1 = wy+s(SIDEBAR_PAD_TOP)+s(44)+s(8);
        g.fill(lx, div1, wx+s(SIDEBAR_W)-s(SIDEBAR_PAD_X), div1+1, 0x0CFFFFFF);
        boolean backHov = mx >= lx && mx <= lx+s(SIDEBAR_W)-s(SIDEBAR_PAD_X)*2 && my >= div1+s(14) && my <= div1+s(14)+s(36);
        if (backHov) RenderHelper.fillRounded(g, lx, div1+s(14), s(SIDEBAR_W)-s(SIDEBAR_PAD_X)*2, s(36), 8, 0x0AFFFFFF);
        g.text(font, "\u2190 Back", lx+s(10), div1+s(14)+(s(36)-9)/2, backHov ? 0xFFFFFFFF : 0xBFFFFFFF);

        // Content header
        int hx = cx+s(22), hy = cy+s(18);
        g.text(font, "\u2328 \u00a7lKeybinds", hx, hy, t.white);
        g.text(font, "\u00a77Manage and edit module keybindings", hx, hy+s(13), t.muted);
        g.fill(hx, hy+s(32), cx+cw-s(22), hy+s(33), 0x0CFFFFFF);

        // Rows
        int rx = cx+s(22), rowW = cw-s(44);
        int baseY = cy + s(62) - scrollOffset;
        g.enableScissor(cx, cy+s(56), cx+cw, cy+ch-s(8));

        // GUI key row
        String guiK = waitingForGuiKey ? "Press a key..." : ClumpsClient.guiKey.getTranslatedKeyMessage().getString();
        if (guiK.isEmpty()) guiK = "None";
        drawKeybindRow(g, mx, my, rx, baseY, rowW, "Open Menu", "GUI", guiK, waitingForGuiKey, t);
        int ry = baseY + s(ROW_H);

        for (ClumpsModule m : ClumpsClient.modules) {
            boolean waiting = waitingModule == m;
            String kn = m.keybind == null ? "None" : m.keybind.getTranslatedKeyMessage().getString();
            if (kn.isEmpty()) kn = "None";
            drawKeybindRow(g, mx, my, rx, ry, rowW, m.getName(), m.getCategory(), waiting ? "Press a key..." : kn, waiting, t);
            ry += s(ROW_H);
        }
        g.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int viewH = ch, sbH = Math.max(s(20), viewH*viewH/(viewH+maxScroll));
            int sbY = cy + (int)((long)scrollOffset*(viewH-sbH)/Math.max(1,maxScroll));
            g.fill(cx+cw-s(5), cy+s(56), cx+cw-s(2), cy+ch-s(8), 0x10FFFFFF);
            g.fill(cx+cw-s(5), sbY, cx+cw-s(2), sbY+sbH, 0x3000D5FF);
        }
    }

    private void drawKeybindRow(GuiGraphicsExtractor g, int mx, int my,
                                int rx, int ry, int rowW,
                                String name, String sub, String key, boolean waiting, Theme t) {
        int rh = s(38);
        if (ry + rh < cy + s(56) || ry > cy + ch - s(8)) return;
        boolean hover = mx>=rx && mx<=rx+rowW && my>=ry && my<=ry+rh;
        RenderHelper.fillRounded(g, rx, ry, rowW, rh, 7, 0xF0050C1E);
        RenderHelper.drawGlassOverlay(g, rx, ry, rowW, rh);
        RenderHelper.drawInnerBorder(g, rx, ry, rowW, rh, 7, hover ? 0x2A00B4FF : 0x1478B4FF);
        g.text(font, name, rx+s(12), ry+(rh-18)/2, t.white);
        g.text(font, "\u00a77" + sub, rx+s(12)+font.width(name)+s(6), ry+(rh-18)/2+s(9), t.muted);
        // Key badge
        int bw = font.width(key)+s(14), bx = rx+rowW-bw-s(88), by2 = ry+(rh-s(16))/2;
        RenderHelper.fillRounded(g, bx, by2, bw, s(16), 4, waiting ? 0x2200D5FF : 0x18FFFFFF);
        RenderHelper.drawInnerBorder(g, bx, by2, bw, s(16), 4, waiting ? 0x4400D5FF : 0x2278B4FF);
        g.text(font, "\u00a77"+key, bx+s(7), by2+s(4), waiting ? t.primary : 0xBFFFFFFF);
        // Edit / Clear buttons
        int editX = rx+rowW-s(82);
        drawSmallBtn(g, mx, my, editX,      ry+(rh-s(18))/2, s(36), s(18), "Edit",  t);
        drawSmallBtn(g, mx, my, editX+s(42), ry+(rh-s(18))/2, s(36), s(18), "Clear", t);
    }

    private void drawSmallBtn(GuiGraphicsExtractor g, int mx, int my, int bx, int by, int bw, int bh, String label, Theme t) {
        boolean h = mx>=bx && mx<=bx+bw && my>=by && my<=by+bh;
        RenderHelper.fillRounded(g, bx, by, bw, bh, 5, h ? 0x2200B4FF : 0x10FFFFFF);
        RenderHelper.drawInnerBorder(g, bx, by, bw, bh, 5, h ? 0x4400D5FF : 0x1878B4FF);
        g.text(font, label, bx+(bw-font.width(label))/2, by+(bh-9)/2, h ? t.primary : 0xBFFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
}
