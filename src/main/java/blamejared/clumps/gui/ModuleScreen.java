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
    private static final String[] CAT_ICONS = {"\u2694", "\u2692", "\u22EE", "\u27A4", "\u25C6", "\u25CB"};

    private static final int CORNER_R = 18;
    private static final int SIDEBAR_W = 210;
    private static final int NAV_H = 32;
    private static final int NAV_GAP = 2;
    private static final int CARD_H = 60;
    private static final int CARD_GAP = 6;
    private static final int TOGGLE_W = 36;
    private static final int TOGGLE_H = 18;
    private static final int BASE_W = 955;
    private static final int BASE_H = 635;

    private static final int SIDEBAR_PAD_X = 16;
    private static final int SIDEBAR_PAD_TOP = 18;

    private String selectedCategory = CATEGORIES[0];
    private String searchText = "";
    private boolean searchFocused = false;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private int wx, wy, ww, wh;
    private int cx, cy, cw, ch;
    private int sbX, sbY, sbW, sbH;
    private int navStartY;
    private int generalStartY;
    private int sidebarBrandEnd;
    private final List<ModuleSlot> slots = new ArrayList<>();

    public ModuleScreen() {
        super(Component.literal("Fristy Client"));
    }

    @Override
    protected void init() {
        ww = Math.min(BASE_W, (int)(width * 0.88f));
        wh = Math.min(BASE_H, (int)(height * 0.88f));
        ww = Math.max(ww, 700);
        wh = Math.max(wh, 500);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
        cx = wx + s(SIDEBAR_W);
        cy = wy;
        cw = ww - s(SIDEBAR_W);
        ch = wh;

        sbX = cx + cw - s(200);
        sbY = cy + s(18);
        sbW = s(200);
        sbH = s(28);

        int brandBottom = wy + s(SIDEBAR_PAD_TOP) + s(38);
        navStartY = brandBottom + s(12);
        int dividerY = navStartY + s(12) + CATEGORIES.length * (s(NAV_H) + s(NAV_GAP));
        sidebarBrandEnd = brandBottom;
        generalStartY = dividerY + s(10);
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
        List<ClumpsModule> modules = getFilteredModules();
        int cardW = cw - s(24) * 2;
        int y = getCardStartY();
        for (ClumpsModule m : modules) {
            slots.add(new ModuleSlot(m, cx + s(24), y, cardW, s(CARD_H)));
            y += s(CARD_H) + s(CARD_GAP);
        }
        maxScroll = Math.max(0, y - (cy + ch - s(16)));
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
    }

    private int getCardStartY() { return cy + s(72); }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx >= cx && mx <= cx + cw && my >= cy + s(60) && my <= cy + ch) {
            if (maxScroll > 0) {
                scrollOffset = (int)Math.clamp(scrollOffset - sy * (s(CARD_H) + s(CARD_GAP)), 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean active) {
        double mx = event.x();
        double my = event.y();
        int btn = event.button();

        searchFocused = mx >= sbX && mx <= sbX + sbW && my >= sbY && my <= sbY + sbH;

        int navItemW = s(SIDEBAR_W) - s(SIDEBAR_PAD_X) * 2;

        if (btn == 0) {
            int navSectionTop = navStartY + s(12);
            for (int i = 0; i < CATEGORIES.length; i++) {
                int ny = navSectionTop + i * (s(NAV_H) + s(NAV_GAP));
                if (mx >= wx + s(SIDEBAR_PAD_X) && mx <= wx + s(SIDEBAR_PAD_X) + navItemW && my >= ny && my <= ny + s(NAV_H)) {
                    if (!CATEGORIES[i].equals(selectedCategory)) {
                        selectedCategory = CATEGORIES[i];
                        scrollOffset = 0;
                        searchText = "";
                        rebuildSlots();
                    }
                    return true;
                }
            }

            String[] bottomItems = {"Settings", "Socials", "Configs", "Theme", "Keybinds"};
            int generalSectionTop = generalStartY + s(12);
            for (int i = 0; i < bottomItems.length; i++) {
                int ny = generalSectionTop + i * (s(NAV_H) + s(NAV_GAP));
                if (mx >= wx + s(SIDEBAR_PAD_X) && mx <= wx + s(SIDEBAR_PAD_X) + navItemW && my >= ny && my <= ny + s(NAV_H)) {
                    if (i == 3) minecraft.setScreen(new ThemeSelectorScreen(this));
                    else if (i == 1) minecraft.setScreen(new FriendsScreen(this));
                    else if (i == 4) minecraft.setScreen(new KeybindScreen(this));
                    return true;
                }
            }

            for (ModuleSlot slot : slots) {
                int sy = slot.y - scrollOffset;
                if (sy + s(CARD_H) < getCardStartY() || sy > cy + ch) continue;
                int tX = slot.x + slot.w - s(14) - s(TOGGLE_W);
                int tY = sy + (s(CARD_H) - s(TOGGLE_H)) / 2;
                if (mx >= tX && mx <= tX + s(TOGGLE_W) && my >= tY && my <= tY + s(TOGGLE_H)) {
                    ClumpsClient.toggleModule(slot.module, minecraft);
                    return true;
                }
                if (mx >= slot.x && mx <= slot.x + slot.w && my >= sy && my <= sy + s(CARD_H)) {
                    ClumpsClient.toggleModule(slot.module, minecraft);
                    return true;
                }
            }
        }

        if (btn == 1) {
            for (ModuleSlot slot : slots) {
                int sy = slot.y - scrollOffset;
                if (sy + s(CARD_H) < getCardStartY() || sy > cy + ch) continue;
                if (mx >= slot.x && mx <= slot.x + slot.w && my >= sy && my <= sy + s(CARD_H)) {
                    if (slot.module instanceof Friends) minecraft.setScreen(new FriendsScreen(this));
                    else minecraft.setScreen(new ModuleOptionsScreen(slot.module, this));
                    return true;
                }
            }
        }

        return super.mouseClicked(event, active);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchFocused) {
            if (event.key() == 259) {
                if (!searchText.isEmpty()) { searchText = searchText.substring(0, searchText.length() - 1); scrollOffset = 0; rebuildSlots(); }
                return true;
            }
            if (event.key() == 256) { searchFocused = false; return true; }
        }
        if (event.key() == 258) { searchFocused = !searchFocused; return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused && event.isAllowedChatCharacter()) {
            String s = event.codepointAsString();
            if (!s.isEmpty()) { searchText += s; scrollOffset = 0; rebuildSlots(); }
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme t = Theme.current();
        int navItemW = s(SIDEBAR_W) - s(SIDEBAR_PAD_X) * 2;

        RenderHelper.fillDoubleGradient(g, 0, 0, width, height,
            0xEB020814, 0xE0041226, 0xF0000A19, 0xF0020E1E);

        RenderHelper.drawGlow(g, wx, wy, ww, wh, CORNER_R, 0x2E00B4FF);
        RenderHelper.fillRounded(g, wx, wy, ww, wh, CORNER_R, 0xF5030A19);
        RenderHelper.fillGradientVertical(g, wx, wy, ww, wh, 0xF5030A19, 0xED020F23);
        RenderHelper.drawGlassOverlay(g, wx, wy, ww, wh);
        RenderHelper.drawInnerBorder(g, wx, wy, ww, wh, CORNER_R, 0x295AB4FF);
        RenderHelper.drawInnerBorder(g, wx, wy, ww, wh, CORNER_R, 0x08000000);

        RenderHelper.fillRounded(g, wx, wy, s(SIDEBAR_W), wh, 16, 0xE0020816);
        RenderHelper.drawGlassOverlay(g, wx, wy, s(SIDEBAR_W), wh);
        RenderHelper.drawInnerBorder(g, wx, wy, s(SIDEBAR_W), wh, 16, 0x2100B4FF);
        g.fill(wx + s(SIDEBAR_W) - 1, wy + s(24), wx + s(SIDEBAR_W), wy + wh - s(24), 0x2100B4FF);

        // Brand
        int logoX = wx + s(SIDEBAR_PAD_X);
        int logoY = wy + s(SIDEBAR_PAD_TOP);
        g.text(font, "\u25C6", logoX, logoY, t.primary);
        g.text(font, "\u00a7lFristy ", logoX + s(10), logoY, t.white);
        int fnW = font.width("\u00a7lFristy \u00a7l");
        g.text(font, "\u00a7lClient", logoX + s(10) + font.width("\u00a7lFristy "), logoY, t.primary);

        String subtitle = "BETA RELEASE";
        g.text(font, "\u00a77" + subtitle, logoX + s(2), logoY + s(12), 0xA6FFFFFF);

        int brandBottom = logoY + s(38);
        g.fill(wx + s(SIDEBAR_PAD_X), brandBottom, wx + s(SIDEBAR_W) - s(SIDEBAR_PAD_X), brandBottom + 1, 0x0CFFFFFF);

        // MODULES label
        int sectionLabelY = brandBottom + s(12);
        g.text(font, "\u00a77MODULES", wx + s(SIDEBAR_PAD_X), sectionLabelY, 0x8CBFFFFF);

        // Category nav items
        int navSectionStart = sectionLabelY + s(12);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int ny = navSectionStart + i * (s(NAV_H) + s(NAV_GAP));
            boolean sel = CATEGORIES[i].equals(selectedCategory);
            boolean hover = mx >= wx + s(SIDEBAR_PAD_X) && mx <= wx + s(SIDEBAR_PAD_X) + navItemW && my >= ny && my <= ny + s(NAV_H);

            if (sel) {
                RenderHelper.fillRounded(g, wx + s(SIDEBAR_PAD_X), ny, navItemW, s(NAV_H), 7, 0x4200A0FF);
                g.fill(wx + s(SIDEBAR_PAD_X), ny + (int)(s(NAV_H) * 0.2f), wx + s(SIDEBAR_PAD_X) + 2, ny + (int)(s(NAV_H) * 0.8f), t.primary);
            } else if (hover) {
                RenderHelper.fillRounded(g, wx + s(SIDEBAR_PAD_X), ny, navItemW, s(NAV_H), 7, 0x0FFFFFFF);
            }

            g.text(font, CAT_ICONS[i] + " " + CATEGORIES[i], wx + s(SIDEBAR_PAD_X) + s(8), ny + (s(NAV_H) - 9) / 2,
                sel ? t.white : 0xBFFFFFFF);

            int cnt = 0;
            for (ClumpsModule m : ClumpsClient.modules) if (m.getCategory().equals(CATEGORIES[i])) cnt++;
            String c = String.valueOf(cnt);
            g.text(font, c, wx + s(SIDEBAR_PAD_X) + navItemW - font.width(c) - s(4), ny + (s(NAV_H) - 9) / 2,
                sel ? t.primary : 0x8CBFFFFF);
        }

        // Divider
        int dividerY = navSectionStart + CATEGORIES.length * (s(NAV_H) + s(NAV_GAP)) + s(6);
        g.fill(wx + s(SIDEBAR_PAD_X) - s(2), dividerY, wx + s(SIDEBAR_W) - s(SIDEBAR_PAD_X) + s(2), dividerY + 1, 0x0CFFFFFF);

        // GENERAL label
        int generalLabelY = dividerY + s(10);
        g.text(font, "\u00a77GENERAL", wx + s(SIDEBAR_PAD_X), generalLabelY, 0x8CBFFFFF);

        // General nav items
        String[] bottomItems = {"Settings", "Socials", "Configs", "Theme", "Keybinds"};
        String[] bottomIcons = {"\u2699", "\u2661", "\u25A0", "\u2726", "\u2328"};
        int generalSectionStart = generalLabelY + s(12);
        for (int i = 0; i < bottomItems.length; i++) {
            int ny = generalSectionStart + i * (s(NAV_H) + s(NAV_GAP));
            boolean hover = mx >= wx + s(SIDEBAR_PAD_X) && mx <= wx + s(SIDEBAR_PAD_X) + navItemW && my >= ny && my <= ny + s(NAV_H);
            if (hover) RenderHelper.fillRounded(g, wx + s(SIDEBAR_PAD_X), ny, navItemW, s(NAV_H), 7, 0x0FFFFFFF);
            g.text(font, bottomIcons[i] + " " + bottomItems[i], wx + s(SIDEBAR_PAD_X) + s(8), ny + (s(NAV_H) - 9) / 2,
                hover ? t.white : 0xBFFFFFFF);
        }

        // Theme dots at bottom
        String tn = Theme.currentName();
        g.text(font, "\u00a77Theme  \u00a7f" + tn, wx + s(SIDEBAR_PAD_X), wy + wh - s(22), t.white);

        String[] names = Theme.names();
        int ti = 0;
        for (int i = 0; i < names.length; i++) if (names[i].equals(tn)) ti = i;
        int dotsX = wx + s(SIDEBAR_PAD_X) + font.width("\u00a77Theme  \u00a7f" + tn) + s(6);
        for (int i = 0; i < names.length; i++) {
            g.fill(dotsX + i * 4, wy + wh - s(16), dotsX + i * 4 + 2, wy + wh - s(14),
                i == ti ? t.primary : 0x66FFFFFF);
        }

        // Content area header
        int hx = cx + s(24);
        int hy = cy + s(18);

        String header = selectedCategory + " Modules";
        g.text(font, "\u00a7l" + header, hx, hy, t.white);

        int total = 0, enabled = 0;
        for (ClumpsModule m : ClumpsClient.modules) {
            if (m.getCategory().equals(selectedCategory)) { total++; if (m.enabled) enabled++; }
        }
        g.text(font, total + " modules \u00b7 " + enabled + " enabled", hx, hy + s(14), 0x8CBFFFFF);

        int sepY = hy + s(38);
        g.fill(hx, sepY, cx + cw - s(24), sepY + 1, 0x2100B4FF);

        // Search bar (right-aligned)
        boolean sbHover = mx >= sbX && mx <= sbX + sbW && my >= sbY && my <= sbY + sbH;
        RenderHelper.fillRounded(g, sbX, sbY, sbW, sbH, 8, 0xEB050C1C);
        RenderHelper.drawInnerBorder(g, sbX, sbY, sbW, sbH, 8, searchFocused ? 0x2900B4FF : 0x2100B4FF);
        if (searchFocused) RenderHelper.drawGlow(g, sbX, sbY, sbW, sbH, 8, 0x2900B4FF);

        g.text(font, "\u2315", sbX + s(10), sbY + (sbH - 9) / 2, 0x8CBFFFFF);

        if (!searchText.isEmpty()) {
            g.text(font, searchText, sbX + s(28), sbY + (sbH - 9) / 2, t.white);
            if (searchFocused) {
                int curX = sbX + s(28) + font.width(searchText);
                g.fill(curX, sbY + s(6), curX + 1, sbY + sbH - s(6), t.white);
            }
        } else {
            g.text(font, "\u00a77Search modules...", sbX + s(28), sbY + (sbH - 9) / 2, 0x8CBFFFFF);
        }

        // Module cards
        int cardStartY = getCardStartY();
        g.enableScissor(cx, cardStartY, cx + cw, cy + ch - s(8));

        for (ModuleSlot sl : slots) {
            int sy = sl.y - scrollOffset;
            if (sy + s(CARD_H) < cardStartY || sy > cy + ch - s(8)) continue;
            boolean hover = mx >= sl.x && mx <= sl.x + sl.w && my >= sy && my <= sy + s(CARD_H);

            RenderHelper.fillRounded(g, sl.x, sy, sl.w, s(CARD_H), 8, 0xE80A1428);
            RenderHelper.drawGlassOverlay(g, sl.x, sy, sl.w, s(CARD_H));
            if (hover) RenderHelper.drawGlow(g, sl.x, sy, sl.w, s(CARD_H), 8, 0x2E00B4FF);
            RenderHelper.drawInnerBorder(g, sl.x, sy, sl.w, s(CARD_H), 8, hover ? 0x3800B4FF : 0x2100B4FF);

            if (sl.module.enabled) {
                g.fill(sl.x + 2, sy + s(10), sl.x + 4, sy + s(CARD_H) - s(10), t.primary);
            }

            g.text(font, sl.module.getName(), sl.x + s(14), sy + s(12), t.white);

            String desc = sl.module.getDescription();
            int descMax = sl.w - s(170);
            if (font.width(desc) > descMax) {
                g.text(font, font.plainSubstrByWidth(desc, descMax - font.width("...")) + "...",
                    sl.x + s(14), sy + s(26), 0x8CBFFFFF);
            } else {
                g.text(font, desc, sl.x + s(14), sy + s(26), 0x8CBFFFFF);
            }

            // Chevron arrow
            int chX = sl.x + sl.w - s(14) - s(TOGGLE_W) - s(22);
            boolean chHover = hover && mx >= chX && mx <= chX + s(22) && my >= sy && my <= sy + s(CARD_H);
            if (chHover) RenderHelper.fillRounded(g, chX, sy + (s(CARD_H) - s(22)) / 2, s(22), s(22), 4, 0x0DFFFFFF);
            g.text(font, "\u203A", chX + s(7), sy + (s(CARD_H) - 9) / 2, chHover ? 0xCCFFFFFF : 0x66FFFFFF);

            // Toggle
            int tX = sl.x + sl.w - s(14) - s(TOGGLE_W);
            int tY = sy + (s(CARD_H) - s(TOGGLE_H)) / 2;

            if (sl.module.enabled) {
                RenderHelper.fillRounded(g, tX, tY, s(TOGGLE_W), s(TOGGLE_H), 9, 0xF200D2FF);
                RenderHelper.fillRounded(g, tX, tY, s(TOGGLE_W), s(TOGGLE_H), 9, 0x20FFFFFF);
                int knobX = tX + s(TOGGLE_W) - s(TOGGLE_H) + 1;
                RenderHelper.fillRounded(g, knobX, tY + 2, s(TOGGLE_H) - 4, s(TOGGLE_H) - 4, 7, 0xFFFFFFFF);
            } else {
                RenderHelper.fillRounded(g, tX, tY, s(TOGGLE_W), s(TOGGLE_H), 9, 0x26FFFFFF);
                RenderHelper.fillRounded(g, tX, tY, s(TOGGLE_W), s(TOGGLE_H), 9, 0x08FFFFFF);
                int knobX = tX + 2;
                RenderHelper.fillRounded(g, knobX, tY + 2, s(TOGGLE_H) - 4, s(TOGGLE_H) - 4, 7, 0xFFFFFFFF);
            }
        }

        g.disableScissor();

        if (slots.isEmpty()) {
            g.centeredText(font, "\u00a77No modules found", cx + cw / 2, cardStartY + s(40), 0x8CBFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record ModuleSlot(ClumpsModule module, int x, int y, int w, int h) {}
}
