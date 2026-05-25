package blamejared.clumps.gui;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.FriendData;
import blamejared.clumps.modules.Friends;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FriendsScreen extends Screen {

    private final Screen parent;
    private Friends friends;
    private EditBox nameInput;
    private int scrollOffset = 0;
    private FriendData editingFriend = null;

    private static final int ROW_H = 42;
    private static final int PANEL_W = 520;
    private static final int CORNER_R = 18;

    public FriendsScreen(Screen parent) {
        super(Component.literal("Friends"));
        this.parent = parent;
        for (var m : ClumpsClient.modules) {
            if (m instanceof Friends f) { this.friends = f; break; }
        }
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        clearWidgets();

        int cx = width / 2;
        int panelX = cx - PANEL_W / 2;

        nameInput = addRenderableWidget(new EditBox(
                font, panelX + 24, 68, 260, 20, Component.literal("Player name")));
        nameInput.setHint(Component.literal("Enter player name..."));
        nameInput.setMaxLength(48);
        nameInput.setBordered(false);
        nameInput.setTextColor(0xFFFFFFFF);
        nameInput.setSuggestion("Enter player name...");

        addRenderableWidget(Button.builder(
                Component.literal("Add Friend"),
                btn -> {
                    if (friends != null && friends.addFriend(nameInput.getValue())) {
                        ClumpsClient.saveConfig();
                        nameInput.setValue("");
                        rebuildWidgets();
                    }
                }
        ).bounds(panelX + 295, 67, 90, 22).build());

        if (friends != null) {
            int y = 105;
            int rowW = PANEL_W - 48;

            for (int i = scrollOffset; i < friends.getFriends().size() && y < height - 60; i++) {
                FriendData fd = friends.getFriends().get(i);
                int fy = y;
                boolean isEditing = editingFriend == fd;

                addRenderableWidget(Button.builder(
                        Component.literal("\u2716"),
                        btn -> {
                            friends.removeFriend(fd.getName());
                            ClumpsClient.saveConfig();
                            rebuildWidgets();
                        }
                ).bounds(panelX + 24, fy, 22, 22).build());

                int labelX = panelX + 54;
                int labelW = isEditing ? 100 : 200;
                addRenderableWidget(Button.builder(
                        Component.literal(fd.getName()),
                        btn -> {
                            editingFriend = isEditing ? null : fd;
                            rebuildWidgets();
                        }
                ).bounds(labelX, fy, labelW, 22).build());

                if (isEditing) {
                    int optX = panelX + 165;
                    addRenderableWidget(Button.builder(
                            Component.literal("Atk: " + (fd.isAttackable() ? "ON" : "OFF")),
                            btn -> { fd.setAttackable(!fd.isAttackable()); ClumpsClient.saveConfig(); rebuildWidgets(); }
                    ).bounds(optX, fy, 55, 22).build());

                    addRenderableWidget(Button.builder(
                            Component.literal("Trc: " + (fd.showTracers() ? "ON" : "OFF")),
                            btn -> { fd.setShowTracers(!fd.showTracers()); ClumpsClient.saveConfig(); rebuildWidgets(); }
                    ).bounds(optX + 60, fy, 55, 22).build());

                    addRenderableWidget(Button.builder(
                            Component.literal("ESP: " + (fd.showEsp() ? "ON" : "OFF")),
                            btn -> { fd.setShowEsp(!fd.showEsp()); ClumpsClient.saveConfig(); rebuildWidgets(); }
                    ).bounds(optX + 120, fy, 55, 22).build());
                }

                y += ROW_H;
            }
        }

        addRenderableWidget(Button.builder(
                Component.literal("\u2190 Back"),
                btn -> minecraft.setScreen(parent)
        ).bounds(width / 2 - 50, height - 30, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (friends != null) {
            int maxVisible = (height - 160) / ROW_H;
            int maxScroll = Math.max(0, friends.getFriends().size() - maxVisible);
            if (maxScroll > 0) {
                scrollOffset = Math.clamp(scrollOffset - (int) sy, 0, maxScroll);
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Theme t = Theme.current();

        RenderHelper.fillDoubleGradient(g, 0, 0, width, height,
            0xEB020814, 0xE0041226, 0xF0000A19, 0xF0020E1E);

        int cx = width / 2;
        int px = cx - PANEL_W / 2;
        int py = 28;
        int ph = height - 70;

        RenderHelper.drawGlow(g, px, py, PANEL_W, ph, CORNER_R, 0x2E00B4FF);

        RenderHelper.fillRounded(g, px, py, PANEL_W, ph, CORNER_R, 0xF5030A19);
        RenderHelper.fillGradientVertical(g, px, py, PANEL_W, ph, 0xF5030A19, 0xED020F23);
        RenderHelper.drawGlassOverlay(g, px, py, PANEL_W, ph);
        RenderHelper.drawInnerBorder(g, px, py, PANEL_W, ph, CORNER_R, 0x295AB4FF);

        g.centeredText(font, "\u2661\u00a7l Friends", cx, 18, t.white);
        g.centeredText(font, "\u00a77Manage your friends list", cx, 36, 0x8CBFFFFF);

        g.fill(px + 24, 96, px + PANEL_W - 24, 97, 0x0CFFFFFF);

        if (friends != null && friends.getFriends().isEmpty()) {
            g.centeredText(font, "\u00a77No friends added yet", cx, 140, 0x8CBFFFFF);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
