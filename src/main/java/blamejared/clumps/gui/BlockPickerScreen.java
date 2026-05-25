package blamejared.clumps.gui;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.BlockESP;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Searchable, scrollable grid of every registered block.
 * Each cell shows the block's item icon + short name.
 * Click to toggle on/off for Block ESP.
 */
public class BlockPickerScreen extends Screen {

    private static final int CELL_W    = 56;
    private static final int CELL_H    = 52;  // 16 icon + gap + 2 text lines
    private static final int HEADER_H  = 44;
    private static final int FOOTER_H  = 28;
    private static final int PAD       = 6;

    private final BlockESP blockESP;
    private final Screen   parent;

    private EditBox searchBox;
    private int scrollOffset = 0;
    private final List<Entry> filtered = new ArrayList<>();

    // layout — computed in init
    private int listLeft, listRight, listTop, listBottom;
    private int cols;

    public BlockPickerScreen(BlockESP blockESP, Screen parent) {
        super(Component.literal("Block Picker"));
        this.blockESP = blockESP;
        this.parent   = parent;
    }

    // ── Init ──────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        listLeft   = PAD;
        listRight  = width  - PAD;
        listTop    = HEADER_H;
        listBottom = height - FOOTER_H;
        cols = Math.max(1, (listRight - listLeft) / CELL_W);

        searchBox = this.addRenderableWidget(new EditBox(
                this.font, width / 2 - 100, 22, 200, 16,
                Component.literal("Search")));
        searchBox.setHint(Component.literal("Search blocks..."));
        searchBox.setMaxLength(64);
        searchBox.setResponder(s -> { scrollOffset = 0; rebuildFilter(); });

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                btn -> this.minecraft.setScreen(parent)
        ).bounds(width / 2 - 40, height - FOOTER_H + 4, 80, 20).build());

        rebuildFilter();
    }

    private void rebuildFilter() {
        filtered.clear();
        String q = searchBox == null ? "" : searchBox.getValue().toLowerCase().trim();
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            Block block = entry.getValue();
            if (block == Blocks.AIR) continue;
            String id    = entry.getKey().identifier().toString();
            String label = id.startsWith("minecraft:") ? id.substring(10) : id;
            if (!q.isEmpty() && !label.contains(q) && !id.contains(q)) continue;
            // get item representation
            var item = block.asItem();
            ItemStack stack = item == net.minecraft.world.item.Items.AIR
                    ? ItemStack.EMPTY
                    : new ItemStack(item);
            filtered.add(new Entry(id, label, stack));
        }
        filtered.sort((a, b) -> a.id.compareTo(b.id));
    }

    // ── Input ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx >= listLeft && mx <= listRight && my >= listTop && my <= listBottom) {
            int rows     = (int) Math.ceil((double) filtered.size() / cols);
            int contentH = rows * CELL_H;
            int viewH    = listBottom - listTop;
            int maxScroll = Math.max(0, contentH - viewH);
            scrollOffset  = Math.clamp(scrollOffset - (int)(sy * CELL_H), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        double mx = event.x();
        double my = event.y();
        if (event.button() == 0
                && mx >= listLeft && mx < listRight
                && my >= listTop  && my < listBottom) {
            int col = (int)((mx - listLeft) / CELL_W);
            int row = (int)((my - listTop + scrollOffset) / CELL_H);
            int idx = row * cols + col;
            if (idx >= 0 && idx < filtered.size()) {
                String id = filtered.get(idx).id;
                if (blockESP.getCustomBlockIds().contains(id)) {
                    blockESP.removeCustomBlockId(id);
                } else {
                    blockESP.addCustomBlockId(id);
                }
                ClumpsClient.saveConfig();
                return true;
            }
        }
        return super.mouseClicked(event, bl);
    }

    // ── Render ─────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        // ── Background ────────────────────────────────────────────────────
        g.fill(0, 0, width, height, 0xFF0A0A12);

        // ── Header ────────────────────────────────────────────────────────
        g.fill(0, 0, width, HEADER_H, 0xFF0D0D1A);
        g.fill(0, HEADER_H - 1, width, HEADER_H, 0xFF3355CC);
        g.centeredText(font, "§lBlock Picker", width / 2, 7, 0xFFFFFFFF);
        String selCount = blockESP.getCustomBlockIds().size() + " selected";
        g.text(font, selCount, PAD + 4, 8, 0xFF888888);

        // ── Footer ────────────────────────────────────────────────────────
        g.fill(0, height - FOOTER_H, width, height, 0xFF0D0D1A);
        g.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, 0xFF3355CC);

        // ── Cells ─────────────────────────────────────────────────────────
        int rows = (int) Math.ceil((double) filtered.size() / cols);
        for (int r = 0; r < rows; r++) {
            int cellY = listTop + r * CELL_H - scrollOffset;
            if (cellY + CELL_H < listTop || cellY >= listBottom) continue;

            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= filtered.size()) break;
                Entry entry   = filtered.get(idx);
                int   cellX   = listLeft + c * CELL_W;
                boolean sel   = blockESP.getCustomBlockIds().contains(entry.id);
                boolean hover = mx >= cellX && mx < cellX + CELL_W
                             && my >= cellY && my < cellY + CELL_H;

                // Cell background
                int bg = sel   ? 0xFF1A3828
                       : hover ? 0xFF1C1C32
                               : 0xFF111120;
                g.fill(cellX, cellY, cellX + CELL_W, cellY + CELL_H, bg);

                // Border
                if (sel) {
                    g.outline(cellX, cellY, cellX + CELL_W, cellY + CELL_H, 0xFF44CC66);
                } else if (hover) {
                    g.outline(cellX, cellY, cellX + CELL_W, cellY + CELL_H, 0xFF4455CC);
                }

                // Icon (16×16 item icon centred in top area of cell)
                if (!entry.stack.isEmpty()) {
                    int iconX = cellX + (CELL_W - 16) / 2;
                    int iconY = cellY + 4;
                    g.fakeItem(entry.stack, iconX, iconY);
                } else {
                    // Fallback: coloured square
                    g.fill(cellX + (CELL_W-12)/2, cellY + 6, cellX + (CELL_W+12)/2, cellY + 18, 0xFF888888);
                }

                // Label — up to 2 lines of 7 chars each
                String label = entry.label;
                String line1, line2 = "";
                int maxChars = (CELL_W - 4) / font.width("m");  // roughly chars that fit
                if (font.width(label) <= CELL_W - 4) {
                    line1 = label;
                } else {
                    // split at underscore near midpoint, or hard cut
                    int mid = label.length() / 2;
                    int splitAt = -1;
                    for (int i = mid; i >= 1; i--) {
                        if (label.charAt(i) == '_') { splitAt = i; break; }
                    }
                    if (splitAt < 0) {
                        for (int i = mid; i < label.length(); i++) {
                            if (label.charAt(i) == '_') { splitAt = i; break; }
                        }
                    }
                    if (splitAt > 0) {
                        line1 = label.substring(0, splitAt);
                        line2 = label.substring(splitAt + 1);
                    } else {
                        // hard cut
                        int cut = label.length();
                        while (cut > 1 && font.width(label.substring(0, cut)) > CELL_W - 4) cut--;
                        line1 = label.substring(0, cut);
                        line2 = label.substring(cut);
                    }
                    // Truncate line2 if still too wide
                    if (font.width(line2) > CELL_W - 4) {
                        int cut = line2.length();
                        while (cut > 1 && font.width(line2.substring(0, cut - 1) + "…") > CELL_W - 4) cut--;
                        line2 = line2.substring(0, cut - 1) + "…";
                    }
                }

                int textColor = sel ? 0xFF55FF55 : hover ? 0xFFDDDDFF : 0xFFAAAAAA;
                int textY1 = cellY + 24;
                int textY2 = cellY + 34;

                // Centre text in cell
                g.text(font, line1, cellX + (CELL_W - font.width(line1)) / 2, textY1, textColor);
                if (!line2.isEmpty()) {
                    g.text(font, line2, cellX + (CELL_W - font.width(line2)) / 2, textY2, textColor);
                }
            }
        }

        // ── Scrollbar ─────────────────────────────────────────────────────
        int totalRows = (int) Math.ceil((double) filtered.size() / cols);
        int contentH  = totalRows * CELL_H;
        int viewH     = listBottom - listTop;
        if (contentH > viewH) {
            int sbH   = Math.max(16, viewH * viewH / contentH);
            int sbY   = listTop + (int)((long) scrollOffset * (viewH - sbH) / Math.max(1, contentH - viewH));
            g.fill(width - 5, listTop, width, listBottom, 0xFF1A1A2E);
            g.fill(width - 5, sbY,     width, sbY + sbH,  0xFF3355CC);
        }

        super.extractRenderState(g, mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record Entry(String id, String label, ItemStack stack) {}
}
