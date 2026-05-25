package blamejared.clumps.gui;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.BlockESP;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BlockESPCustomBlocksScreen extends Screen {
    private final BlockESP blockESP;
    private final Screen parent;
    private final List<ScrollEntry> scrollWidgets = new ArrayList<>();
    private EditBox blockIdInput;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public BlockESPCustomBlocksScreen(BlockESP blockESP, Screen parent) {
        super(Component.literal("Block ESP Custom Blocks"));
        this.blockESP = blockESP;
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.scrollWidgets.clear();

        blockIdInput = new EditBox(this.font, this.width / 2 - 100, 20, 200, 20, Component.literal("minecraft:obsidian"));
        blockIdInput.setHint(Component.literal("namespace:block"));
        blockIdInput.setMaxLength(128);
        this.addRenderableWidget(blockIdInput);

        this.addRenderableWidget(Button.builder(
                Component.literal("Add Block"),
                btn -> {
                    if (blockESP.addCustomBlockId(blockIdInput.getValue())) {
                        blockIdInput.setValue("");
                        ClumpsClient.saveConfig();
                        this.init();
                    }
                }
        ).bounds(this.width / 2 - 100, 45, 200, 20).build());

        int y = 80;
        List<String> customBlocks = new ArrayList<>(blockESP.getCustomBlockIds());
        for (String blockId : customBlocks) {
            AbstractWidget removeButton = this.addRenderableWidget(Button.builder(
                    Component.literal("Remove " + blockId),
                    btn -> {
                        blockESP.removeCustomBlockId(blockId);
                        ClumpsClient.saveConfig();
                        this.init();
                    }
            ).bounds(this.width / 2 - 100, y, 200, 20).build());
            this.scrollWidgets.add(new ScrollEntry(removeButton, y));
            y += 24;
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> this.minecraft.setScreen(parent)
        ).bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());

        maxScroll = Math.max(0, y - (height - 35));
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
        updateScrollLayout();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll > 0) {
            scrollOffset = Math.clamp(scrollOffset - (int) (scrollY * 20.0), 0, maxScroll);
            updateScrollLayout();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateScrollLayout() {
        int viewportTop = 80;
        int viewportBottom = height - 35;

        for (ScrollEntry entry : scrollWidgets) {
            int scrolledY = entry.baseY - scrollOffset;
            AbstractWidget widget = entry.widget;
            widget.setY(scrolledY);

            boolean visible = scrolledY + widget.getHeight() > viewportTop && scrolledY < viewportBottom;
            widget.visible = visible;
            widget.active = visible;
        }
    }

    private record ScrollEntry(AbstractWidget widget, int baseY) {}
}
