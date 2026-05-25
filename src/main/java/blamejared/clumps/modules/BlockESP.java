package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

public class BlockESP extends ClumpsModule {

    private final Option.IntOption range = new Option.IntOption("Range", 50, 8, 128, 4);

    public final List<BlockPos> highlighted = new ArrayList<>();
    private List<String> customBlockIds = new ArrayList<>();

    @Override
    public String getName() { return "Block ESP"; }

    @Override
    public String getDescription() { return "Highlight specific blocks through walls"; }

    @Override
    public String getCategory() { return "Visual"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(range); }

    public List<String> getCustomBlockIds() {
        return customBlockIds;
    }

    public void setCustomBlockIds(List<String> ids) {
        this.customBlockIds = new ArrayList<>(ids);
    }

    public boolean addCustomBlockId(String id) {
        if (id == null || id.isBlank()) return false;
        if (customBlockIds.contains(id)) return false;
        customBlockIds.add(id);
        return true;
    }

    public boolean removeCustomBlockId(String id) {
        return customBlockIds.remove(id);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.level == null || client.player == null) return;

        highlighted.clear();

        int r = range.getValue();
        BlockPos origin = client.player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-r, -r, -r),
                origin.offset(r, r, r))) {

            String id = BuiltInRegistries.BLOCK.getKey(
                    client.level.getBlockState(pos).getBlock()
            ).toString();

            if (customBlockIds.contains(id)) {
                highlighted.add(pos.immutable());
            }
        }
    }
}
