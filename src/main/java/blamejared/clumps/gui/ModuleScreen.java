package blamejared.clumps.gui;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.ClumpsModule;
import blamejared.clumps.modules.Friends;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ModuleScreen extends Screen {

    private static final String[] CATEGORIES = {"Combat", "Mace", "Misc", "Movement", "Spear", "Visual"};
    private static final String[] CAT_ICONS  = {"\u2694", "\u2692", "\u22EE", "\u27A4", "\u25C6", "\u25CB"};
    private static final String[] GEN_LABELS = {"\u2699 Settings", "\u2726 Theme", "\u25A0 Configs", "\u2661 Socials", "\u2328 Keybinds"};

    private static final int CORNER_R       = 18;
    private static final int SIDEBAR_W      = 210;
    private static final int NAV_H          = 36;
    private static final int NAV_GAP        = 3;
    private static final int CARD_H         = 60;
    private static final int CARD_GAP       = 5;
    private static final int TOGGLE_W       = 36;
    private static final int TOGGLE_H       = 18;
    private static final int BASE_W         = 955;
    private static final int BASE_H         = 635;
    private static final int SIDEBAR_PAD_X  = 14;
    private static final int SIDEBAR_PAD_TOP = 20;

    private String selectedCategory = CATEGORIES[0];
    private String searchText       = "";
    private boolean searchFocused   = false;
    private int scrollOffset        = 0;
    private int maxScroll           = 0;

    private int wx, wy, ww, wh;
    private int cx, cy, cw, ch;
    private int sbX, sbY, sbW, sbH;
    private int navStartY, genStartY, div1Y, div2Y;
    private final List<ModuleSlot> slots = new ArrayList<>();

    public ModuleScreen() { super(Component.literal("Fristy Client")); }

    @Override
    protected void init() {
        ww = Math.min(BASE_W, (int)(width  * 0.90f)); wh = Math.min(BASE_H, (int)(height * 0.90f));
        ww = Math.max(ww, 700);                        wh = Math.max(wh, 500);
        wx = (width - ww) / 2;  wy = (height - wh) / 2;
        cx = wx + s(SIDEBAR_W); cy = wy; cw = ww - s(SIDEBAR_W); ch = wh;
        sbW = s(200); sbH = s(26);
        sbX = cx + cw - s(20) - sbW; sbY = cy + s(20);
        div1Y     = wy + s(SIDEBAR_PAD_TOP) + s(44) + s(8);
        navStartY = div1Y + s(14);
        div2Y     = navStartY + CATEGORIES.length * (s(NAV_H) + s(NAV_GAP)) + s(8);
        genStartY = div2Y + s(14);
        rebuildSlots();
    }

    private int s(int v) { return Math.max(1, (int)(v * ((float)ww / BASE_W))); }

    private List<ClumpsModule> getFilteredModules() {
        List<ClumpsModule> r = new ArrayList<>();
        String q = searchText.toLowerCase().trim();
        for (ClumpsModule m : ClumpsClient.modules) {
            if (!m.getCategory().equals(selectedCategory)) continue;
            if (!q.isEmpty() && !m.getName().toLowerCase().contains(q)) continue;
            r.add(m);
        }
        return r;
    }

    private void rebuildSlots() {
        slots.clear();
        int cardW = cw - s(22) * 2, y = getCardStartY();
        for (ClumpsModule m : getFilteredModules()) {
            slots.add(new ModuleSlot(m, cx + s(22), y, cardW, s(CARD_H)));
            y += s(CARD_H) + s(CARD_GAP);
        }
        maxScroll    = Math.max(0, y - (cy + ch - s(12)));
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
    }

    private int getCardStartY() { return cy + s(70); }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx >= cx && mx <= cx + cw && my >= getCardStartY() && my <= cy + ch && maxScroll > 0) {
            scrollOffset = (int) Math.clamp(scrollOffset - sy * (s(CARD_H) + s(CARD_GAP)), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean active) {
        double mx = event.x(), my = event.y();
        int btn = event.button();
        searchFocused = (mx >= sbX && mx <= sbX + sbW && my >= sbY && my <= sbY + sbH);
        int navW = s(SIDEBAR_W) - s(SIDEBAR_PAD_X) * 2, nl = wx + s(SIDEBAR_PAD_X);

        if (btn == 0) {
            // Category nav
            for (int i = 0; i < CATEGORIES.length; i++) {
                int ny = navStartY + i * (s(NAV_H) + s(NAV_GAP));
                if (mx >= nl && mx <= nl + navW && my >= ny && my <= ny + s(NAV_H)) {
                    if (!CATEGORIES[i].equals(selectedCategory)) {
                        selectedCategory = CATEGORIES[i]; scrollOffset = 0; searchText = ""; rebuildSlots();
                    }
                    return true;
                }
            }
            // General nav
            for (int i = 0; i < GEN_LABELS.length; i++) {
                int ny = genStartY + i * (s(NAV_H) + s(NAV_GAP));
                if (mx >= nl && mx <= nl + navW && my >= ny && my <= ny + s(NAV_H)) {
                    if      (i == 1) minecraft.setScreen(new ThemeSelectorScreen(this));
                    else if (i == 3) minecraft.setScreen(new FriendsScreen(this));
                    else if (i == 4) minecraft.setScreen(new KeybindScreen(this));
                    return true;
                }
            }
            // Module cards - left click = toggle
            for (ModuleSlot sl : slots) {
                int sy = sl.y - scrollOffset;
                if (sy + s(CARD_H) < getCardStartY() || sy > cy + ch) continue;
                int tX = sl.x + sl.w - s(12) - s(TOGGLE_W), tY = sy + (s(CARD_H) - s(TOGGLE_H)) / 2;
                if (mx >= tX && mx <= tX + s(TOGGLE_W) && my >= tY && my <= tY + s(TOGGLE_H)) {
                    ClumpsClient.toggleModule(sl.module, minecraft); return true;
                }
                if (mx >= sl.x && mx <= sl.x + sl.w && my >= sy && my <= sy + s(CARD_H)) {
                    ClumpsClient.toggleModule(sl.module, minecraft); return true;
                }
            }
        }
        if (btn == 1) {
            // Right click = options
            for (ModuleSlot sl : slots) {
                int sy = sl.y - scrollOffset;
                if (sy + s(CARD_H) < getCardStartY() || sy > cy + ch) continue;
                if (mx >= sl.x && mx <= sl.x + sl.w && my >= sy && my <= sy + s(CARD_H)) {
                    if (sl.module instanceof Friends) minecraft.setScreen(new FriendsScreen(this));
                    else minecraft.setScreen(new ModuleOptionsScreen(sl.module, this));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, active);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchFocused) {
            if (event.key() == 259 && !searchText.isEmpty()) { searchText = searchText.substring(0, searchText.length()-1); scrollOffset=0; rebuildSlots(); return true; }
            if (event.key() == 256) { searchFocused = false; return true; }
        }
        if (event.key() == 258) { searchFocused = !searchFocused; return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused && event.isAllowedChatCharacter()) {
            searchText += event.codepointAsString(); scrollOffset = 0; rebuildSlots(); return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme t = Theme.current();
        int navW = s(SIDEBAR_W) - s(SIDEBAR_PAD_X) * 2, nl = wx + s(SIDEBAR_PAD_X);

        // 1. Glass background
        RenderHelper.fillDoubleGradient(g, 0, 0, width, height, 0xEB020814, 0xE0041226, 0xF0000A19, 0xF0020E1E);

        // 2. Window
        RenderHelper.drawGlow(g, wx, wy, ww, wh, CORNER_R, 0x2E00B4FF);
        RenderHelper.fillRounded(g, wx, wy, ww, wh, CORNER_R, 0xF5030A19);
        RenderHelper.fillGradientVertical(g, wx, wy, ww, wh, 0x080C2848, 0x02000000);
        RenderHelper.drawGlassOverlay(g, wx, wy, ww, wh);
        RenderHelper.drawInnerBorder(g, wx, wy, ww, wh, CORNER_R, 0x295AB4FF);

        // 3. Sidebar
        RenderHelper.fillRounded(g, wx, wy, s(SIDEBAR_W), wh, CORNER_R, 0xE2020816);
        RenderHelper.drawGlassOverlay(g, wx, wy, s(SIDEBAR_W), wh);
        RenderHelper.drawInnerBorder(g, wx, wy, s(SIDEBAR_W), wh, CORNER_R, 0x1A78B4FF);
        g.fill(wx + s(SIDEBAR_W)-1, wy + s(20), wx + s(SIDEBAR_W), wy + wh - s(20), 0x2200B4FF);

        // 4. Brand
        int lx = wx + s(SIDEBAR_PAD_X), ly = wy + s(SIDEBAR_PAD_TOP);
        g.text(font, "\u25C6", lx, ly + s(4), t.primary);
        g.text(font, "\u00a7lFristy \u00a7r\u00a7lClient", lx + s(12), ly, t.white);
        g.text(font, "\u00a77BETA RELEASE", lx, ly + s(18), t.muted);
        g.fill(wx + s(SIDEBAR_PAD_X), div1Y, wx + s(SIDEBAR_W) - s(SIDEBAR_PAD_X), div1Y + 1, 0x0CFFFFFF);

        // 5. MODULES section label + nav
        g.text(font, "\u00a77MODULES", nl, navStartY - s(12), t.muted);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int ny = navStartY + i * (s(NAV_H) + s(NAV_GAP));
            boolean sel   = CATEGORIES[i].equals(selectedCategory);
            boolean hover = mx >= nl && mx <= nl + navW && my >= ny && my <= ny + s(NAV_H);
            drawNavItem(g, nl, ny, navW, CAT_ICONS[i] + " " + CATEGORIES[i], sel, hover, t);
            int cnt = 0;
            for (ClumpsModule m : ClumpsClient.modules) if (m.getCategory().equals(CATEGORIES[i])) cnt++;
            String cs = String.valueOf(cnt);
            g.text(font, cs, nl + navW - font.width(cs) - s(6), ny + (s(NAV_H)-9)/2, sel ? t.primary : 0x55FFFFFF);
        }

        // 6. GENERAL section label + nav
        g.fill(wx + s(SIDEBAR_PAD_X), div2Y, wx + s(SIDEBAR_W) - s(SIDEBAR_PAD_X), div2Y + 1, 0x0CFFFFFF);
        g.text(font, "\u00a77GENERAL", nl, genStartY - s(12), t.muted);
        for (int i = 0; i < GEN_LABELS.length; i++) {
            int ny = genStartY + i * (s(NAV_H) + s(NAV_GAP));
            boolean hover = mx >= nl && mx <= nl + navW && my >= ny && my <= ny + s(NAV_H);
            drawNavItem(g, nl, ny, navW, GEN_LABELS[i], false, hover, t);
        }

        // 7. Theme indicator bottom of sidebar
        String tn = Theme.currentName();
        g.text(font, "\u00a77Theme  \u00a7f" + tn, nl, wy + wh - s(18), t.white);

        // 8. Content header
        int hx = cx + s(22), hy = cy + s(18);
        int total = 0, enabled = 0;
        for (ClumpsModule m : ClumpsClient.modules) {
            if (m.getCategory().equals(selectedCategory)) { total++; if (m.enabled) enabled++; }
        }
        g.text(font, "\u00a7l" + selectedCategory + " Modules", hx, hy, t.white);
        g.text(font, total + " modules \u00b7 " + enabled + " enabled", hx, hy + s(14), t.muted);
        int sepY = hy + s(34);
        g.fill(hx, sepY, cx + cw - s(22), sepY + 1, 0x0CFFFFFF);

        // 9. Search box
        boolean sbHov = mx >= sbX && mx <= sbX + sbW && my >= sbY && my <= sbY + sbH;
        RenderHelper.fillRounded(g, sbX, sbY, sbW, sbH, 8, 0xE8050C1C);
        RenderHelper.drawInnerBorder(g, sbX, sbY, sbW, sbH, 8, searchFocused ? 0x3300B4FF : sbHov ? 0x2278B4FF : 0x1478B4FF);
        if (searchFocused) RenderHelper.drawGlow(g, sbX, sbY, sbW, sbH, 8, 0x1200B4FF);
        g.text(font, "\u2315", sbX + s(8), sbY + (sbH-9)/2, 0xBFFFFFFF);
        g.text(font, searchText.isEmpty() ? "\u00a77Search modules..." : searchText,
            sbX + s(22), sbY + (sbH-9)/2, searchText.isEmpty() ? 0x70FFFFFF : 0xFFFFFFFF);
        if (searchFocused && !searchText.isEmpty())
            g.fill(sbX + s(22) + font.width(searchText), sbY + s(5), sbX + s(23) + font.width(searchText), sbY + sbH - s(5), 0xFFFFFFFF);

        // 10. Module cards
        int cardStartY = getCardStartY();
        g.enableScissor(cx, cardStartY, cx + cw, cy + ch - s(8));
        rebuildSlots();
        for (ModuleSlot sl : slots) {
            int sy = sl.y - scrollOffset;
            if (sy + s(CARD_H) < cardStartY || sy > cy + ch - s(8)) continue;
            boolean hover = mx >= sl.x && mx <= sl.x + sl.w && my >= sy && my <= sy + s(CARD_H);
            // Card
            RenderHelper.fillRounded(g, sl.x, sy, sl.w, s(CARD_H), 9, 0xF0050C1E);
            RenderHelper.fillGradientVertical(g, sl.x, sy, sl.w, s(CARD_H), 0x06142840, 0x02000000);
            RenderHelper.drawGlassOverlay(g, sl.x, sy, sl.w, s(CARD_H));
            if (hover) RenderHelper.drawGlow(g, sl.x, sy, sl.w, s(CARD_H), 9, 0x1800B4FF);
            RenderHelper.drawInnerBorder(g, sl.x, sy, sl.w, s(CARD_H), 9, hover ? 0x2A00B4FF : 0x1478B4FF);
            // Enabled stripe
            if (sl.module.enabled) g.fill(sl.x+2, sy+s(8), sl.x+3, sy+s(CARD_H)-s(8), t.primary);
            // Text
            g.text(font, sl.module.getName(), sl.x+s(14), sy+s(13), t.white);
            String desc = sl.module.getDescription();
            int dMax = sl.w - s(150);
            if (font.width(desc) > dMax) desc = font.plainSubstrByWidth(desc, dMax - font.width("...")) + "...";
            g.text(font, desc, sl.x+s(14), sy+s(29), t.muted);
            // Chevron
            g.text(font, "\u203A", sl.x+sl.w-s(12)-s(TOGGLE_W)-s(18), sy+(s(CARD_H)-9)/2, 0x70FFFFFF);
            // Toggle
            int tX = sl.x+sl.w-s(12)-s(TOGGLE_W), tY = sy+(s(CARD_H)-s(TOGGLE_H))/2;
            if (sl.module.enabled) {
                RenderHelper.fillRounded(g, tX, tY, s(TOGGLE_W), s(TOGGLE_H), 9, 0xF200D2FF);
                RenderHelper.fillRounded(g, tX, tY, s(TOGGLE_W), s(TOGGLE_H), 9, 0x18FFFFFF);
                RenderHelper.fillRounded(g, tX+s(TOGGLE_W)-s(TOGGLE_H)+1, tY+2, s(TOGGLE_H)-4, s(TOGGLE_H)-4, 7, 0xFFFFFFFF);
            } else {
                RenderHelper.fillRounded(g, tX, tY, s(TOGGLE_W), s(TOGGLE_H), 9, 0x22FFFFFF);
                RenderHelper.fillRounded(g, tX+2, tY+2, s(TOGGLE_H)-4, s(TOGGLE_H)-4, 7, 0xFFFFFFFF);
            }
        }
        g.disableScissor();
        if (slots.isEmpty()) g.centeredText(font, "\u00a77No modules found", cx+cw/2, cardStartY+s(40), 0x80FFFFFF);
        // Scrollbar
        if (maxScroll > 0) {
            int viewH = cy+ch-s(12)-cardStartY, sbH2 = Math.max(s(20), viewH*viewH/(viewH+maxScroll));
            int sbY2 = cardStartY + (int)((long)scrollOffset*(viewH-sbH2)/Math.max(1,maxScroll));
            g.fill(cx+cw-s(5), cardStartY, cx+cw-s(2), cy+ch-s(12), 0x10FFFFFF);
            g.fill(cx+cw-s(5), sbY2, cx+cw-s(2), sbY2+sbH2, 0x3000D5FF);
        }
    }

    private void drawNavItem(GuiGraphicsExtractor g, int x, int y, int w, String label, boolean sel, boolean hover, Theme t) {
        if (sel) {
            RenderHelper.fillRounded(g, x, y, w, s(NAV_H), 8, 0x3200A0FF);
            RenderHelper.fillGradientHorizontal(g, x, y, w, s(NAV_H), 0x3200A0FF, 0x1800DCFF);
            RenderHelper.drawGlow(g, x, y, w, s(NAV_H), 8, 0x1000B4FF);
            g.fill(x, y+s(6), x+2, y+s(NAV_H)-s(6), t.primary);
        } else if (hover) {
            RenderHelper.fillRounded(g, x, y, w, s(NAV_H), 8, 0x0AFFFFFF);
        }
        g.text(font, label, x+s(10), y+(s(NAV_H)-9)/2, sel || hover ? 0xFFFFFFFF : 0xBFFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
    private record ModuleSlot(ClumpsModule module, int x, int y, int w, int h) {}
}
