package blamejared.clumps.gui;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.FriendData;
import blamejared.clumps.modules.Friends;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class FriendsScreen extends Screen {

    private final Screen parent;
    private Friends friends;

    private static final int CORNER_R       = 18;
    private static final int SIDEBAR_W      = 210;
    private static final int BASE_W         = 955;
    private static final int BASE_H         = 635;
    private static final int SIDEBAR_PAD_X  = 14;
    private static final int SIDEBAR_PAD_TOP = 20;
    private static final int ROW_H          = 40;

    private int scrollOffset = 0, maxScroll = 0;
    private FriendData editingFriend = null;
    private String socialsTab = "friends"; // "all" or "friends"

    private int wx, wy, ww, wh, cx, cy, cw, ch;
    private EditBox nameInput;

    public FriendsScreen(Screen parent) {
        super(Component.literal("Socials"));
        this.parent = parent;
        for (var m : ClumpsClient.modules) if (m instanceof Friends f) { this.friends = f; break; }
    }

    private int s(int v) { return Math.max(1, (int)(v * ((float)ww / BASE_W))); }

    @Override
    protected void init() {
        ww = Math.min(BASE_W, (int)(width  * 0.90f)); wh = Math.min(BASE_H, (int)(height * 0.90f));
        ww = Math.max(ww, 700);                        wh = Math.max(wh, 500);
        wx = (width-ww)/2; wy = (height-wh)/2;
        cx = wx+s(SIDEBAR_W); cy = wy; cw = ww-s(SIDEBAR_W); ch = wh;
        clearWidgets();

        // Add-friend input box
        int inputY = cy + s(102);
        nameInput = addRenderableWidget(new EditBox(font, cx+s(22), inputY, cw-s(44)-s(90), s(24),
                Component.literal("Player name")));
        nameInput.setHint(Component.literal("Enter player name..."));
        nameInput.setMaxLength(48); nameInput.setBordered(false); nameInput.setTextColor(0xFFFFFFFF);

        addRenderableWidget(Button.builder(Component.literal("Add Friend"), btn -> {
            if (friends != null && !nameInput.getValue().isEmpty()) {
                friends.addFriend(nameInput.getValue()); ClumpsClient.saveConfig();
                nameInput.setValue(""); rebuildScroll();
            }
        }).bounds(cx + cw - s(22) - s(86), inputY, s(86), s(24)).build());

        rebuildScroll();
    }

    private void rebuildScroll() {
        int listH = friends == null ? 0 : friends.getFriends().size() * s(ROW_H);
        maxScroll = Math.max(0, listH - (ch - s(140)));
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
    public boolean mouseClicked(MouseButtonEvent event, boolean active) {
        double mx = event.x(), my = event.y();
        if (event.button() != 0) return super.mouseClicked(event, active);

        // Tab clicks
        int tabY = cy + s(62), tabH = s(26);
        int t1x = cx+s(22), t2x = t1x+s(96);
        if (my >= tabY && my <= tabY+tabH) {
            if (mx >= t1x && mx <= t1x+s(90))   { socialsTab = "all";     return true; }
            if (mx >= t2x && mx <= t2x+s(80))    { socialsTab = "friends"; return true; }
        }

        // Friend row clicks (only in friends tab)
        if (socialsTab.equals("friends") && friends != null) {
            int rx = cx+s(22), rowW = cw-s(44);
            int listY = cy + s(136) - scrollOffset;
            for (FriendData fd : friends.getFriends()) {
                if (my >= listY && my <= listY+s(32)) {
                    // Remove button (right side)
                    int delX = rx+rowW-s(60);
                    if (mx >= delX && mx <= delX+s(56)) {
                        friends.removeFriend(fd.getName()); ClumpsClient.saveConfig(); rebuildScroll();
                        return true;
                    }
                    // Toggle editing
                    editingFriend = editingFriend == fd ? null : fd;
                    return true;
                }
                int extraH = editingFriend == fd ? s(30) : 0;
                listY += s(ROW_H) + extraH;
            }
        }
        return super.mouseClicked(event, active);
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
        int div1 = wy+s(SIDEBAR_PAD_TOP)+s(44)+s(8);
        g.fill(lx, div1, wx+s(SIDEBAR_W)-s(SIDEBAR_PAD_X), div1+1, 0x0CFFFFFF);
        boolean backHov = mx >= lx && mx <= lx+s(SIDEBAR_W)-s(SIDEBAR_PAD_X)*2 && my >= div1+s(14) && my <= div1+s(50);
        if (backHov) RenderHelper.fillRounded(g, lx, div1+s(14), s(SIDEBAR_W)-s(SIDEBAR_PAD_X)*2, s(36), 8, 0x0AFFFFFF);
        g.text(font, "\u2190 Back", lx+s(10), div1+s(14)+(s(36)-9)/2, backHov ? 0xFFFFFFFF : 0xBFFFFFFF);

        // Content header
        int hx = cx+s(22), hy = cy+s(18);
        g.text(font, "\u2661 \u00a7lSocials", hx, hy, t.white);
        g.text(font, "\u00a77Manage your friends and connections", hx, hy+s(13), t.muted);
        g.fill(hx, hy+s(32), cx+cw-s(22), hy+s(33), 0x0CFFFFFF);

        // Tabs  (Prestige style)
        int tabY = cy+s(62), tabH = s(26);
        drawTab(g, mx, my, cx+s(22),       tabY, s(90), tabH, "All Players", socialsTab.equals("all"),     t);
        drawTab(g, mx, my, cx+s(22)+s(96), tabY, s(80), tabH, "Friends",     socialsTab.equals("friends"), t);

        // Add-friend input background
        int inputY = cy+s(102);
        RenderHelper.fillRounded(g, cx+s(22), inputY, cw-s(44)-s(90), s(24), 6, 0xE8050C1C);
        RenderHelper.drawInnerBorder(g, cx+s(22), inputY, cw-s(44)-s(90), s(24), 6,
            nameInput != null && nameInput.isFocused() ? 0x3300B4FF : 0x1478B4FF);

        g.fill(cx+s(22), cy+s(132), cx+cw-s(22), cy+s(133), 0x0CFFFFFF);

        // Friend list
        if (socialsTab.equals("friends")) {
            g.enableScissor(cx, cy+s(136), cx+cw, cy+ch-s(8));
            if (friends == null || friends.getFriends().isEmpty()) {
                g.centeredText(font, "\u00a77No friends added yet", cx+cw/2, cy+s(160), 0x80FFFFFF);
            } else {
                int rx = cx+s(22), rowW = cw-s(44);
                int listY = cy+s(136) - scrollOffset;
                for (FriendData fd : friends.getFriends()) {
                    boolean hover = mx>=rx && mx<=rx+rowW && my>=listY && my<=listY+s(32);
                    RenderHelper.fillRounded(g, rx, listY, rowW, s(32), 7, 0xF0050C1E);
                    RenderHelper.drawGlassOverlay(g, rx, listY, rowW, s(32));
                    RenderHelper.drawInnerBorder(g, rx, listY, rowW, s(32), 7, hover ? 0x2A00B4FF : 0x1478B4FF);
                    // Avatar placeholder
                    RenderHelper.fillRounded(g, rx+s(8), listY+s(6), s(20), s(20), 4, 0x2200D5FF);
                    g.text(font, String.valueOf(fd.getName().charAt(0)), rx+s(13), listY+(s(32)-9)/2, t.primary);
                    // Name + stats
                    g.text(font, fd.getName(), rx+s(36), listY+s(6), t.white);
                    g.text(font, "\u00a77ESP:" + (fd.showEsp()?"ON":"OFF") + "  Trc:" + (fd.showTracers()?"ON":"OFF"),
                        rx+s(36), listY+s(18), t.muted);
                    // Delete button
                    int delX = rx+rowW-s(60);
                    boolean delHov = mx>=delX && mx<=delX+s(56) && my>=listY && my<=listY+s(32);
                    RenderHelper.fillRounded(g, delX, listY+s(7), s(56), s(18), 5, delHov ? 0x22FF4444 : 0x10FFFFFF);
                    RenderHelper.drawInnerBorder(g, delX, listY+s(7), s(56), s(18), 5, delHov ? 0x44FF4444 : 0x1878B4FF);
                    g.text(font, "Remove", delX+(s(56)-font.width("Remove"))/2, listY+s(7)+(s(18)-9)/2,
                        delHov ? 0xFFFF6666 : 0xBFFFFFFF);
                    listY += s(ROW_H);
                }
            }
            g.disableScissor();
            if (maxScroll > 0) {
                int viewH = ch-s(140), sbH = Math.max(s(20), viewH*viewH/(viewH+maxScroll));
                int sbY = cy+s(136) + (int)((long)scrollOffset*(viewH-sbH)/Math.max(1,maxScroll));
                g.fill(cx+cw-s(5), cy+s(136), cx+cw-s(2), cy+ch-s(8), 0x10FFFFFF);
                g.fill(cx+cw-s(5), sbY, cx+cw-s(2), sbY+sbH, 0x3000D5FF);
            }
        } else {
            g.centeredText(font, "\u00a77No players in server", cx+cw/2, cy+s(180), 0x80FFFFFF);
        }

        super.extractRenderState(g, mx, my, delta);
    }

    private void drawTab(GuiGraphicsExtractor g, int mx, int my, int tx, int ty, int tw, int th, String label, boolean active, Theme t) {
        boolean h = mx>=tx && mx<=tx+tw && my>=ty && my<=ty+th;
        if (active) {
            RenderHelper.fillRounded(g, tx, ty, tw, th, 7, t.primary);
            g.text(font, label, tx+(tw-font.width(label))/2, ty+(th-9)/2, 0xFF000000);
        } else {
            RenderHelper.fillRounded(g, tx, ty, tw, th, 7, h ? 0x14FFFFFF : 0x0AFFFFFF);
            RenderHelper.drawInnerBorder(g, tx, ty, tw, th, 7, 0x1278B4FF);
            g.text(font, label, tx+(tw-font.width(label))/2, ty+(th-9)/2, h ? 0xFFFFFFFF : 0xBFFFFFFF);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
