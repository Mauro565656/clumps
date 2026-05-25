package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class AutoDrain extends ClumpsModule {
    private final Option.IntOption range = new Option.IntOption("Range", 5, 1, 10, 1);
    private final Option.IntOption liquidType = new Option.IntOption("Liquid Type", 2, 0, 2, 1);
    private final Option.IntOption drainMode = new Option.IntOption("Drain Mode", 0, 0, 1, 1);
    private final Option.BoolOption autoActivate = new Option.BoolOption("Auto Activate", false);
    private final Option.IntOption checkDelay = new Option.IntOption("Check Delay", 5, 1, 20, 1);

    private boolean active = false;
    private int clock = 0;
    private boolean lastTogglePress = false;
    private final List<BlockPos> liquidBlocks = new ArrayList<>();

    @Override
    public String getName() {
        return "Auto Drain";
    }

    @Override
    public String getDescription() { return "Auto-destroy nearby crystals"; }

    @Override
    public String getCategory() {
        return "Combat";
    }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(range, liquidType, drainMode, autoActivate, checkDelay);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }

        boolean pressed = client.options.keyUse.isDown();
        if (pressed && !lastTogglePress) {
            active = !active;
        }
        lastTogglePress = pressed;

        if (!active && !autoActivate.getValue()) {
            return;
        }
        if (clock++ < checkDelay.getValue()) {
            return;
        }
        clock = 0;

        scanForLiquids(client);
        for (BlockPos pos : liquidBlocks) {
            if (drainMode.getValue() == 0) {
                drainWithBlock(client, pos);
            } else {
                drainWithBucket(client, pos);
            }
        }
    }

    private void scanForLiquids(Minecraft client) {
        liquidBlocks.clear();
        BlockPos center = client.player.blockPosition();
        int r = range.getValue();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (isMatchingLiquid(client.level.getBlockState(pos), client.level.getFluidState(pos))) {
                        liquidBlocks.add(pos);
                    }
                }
            }
        }
    }

    private boolean isMatchingLiquid(BlockState state, FluidState fluidState) {
        if (!(state.getBlock() instanceof LiquidBlock) || !fluidState.isSource()) {
            return false;
        }
        if (liquidType.getValue() == 0) {
            return state.is(Blocks.WATER);
        }
        if (liquidType.getValue() == 1) {
            return state.is(Blocks.LAVA);
        }
        return state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }

    private void drainWithBucket(Minecraft client, BlockPos pos) {
        int bucketSlot = findHotbarItem(client, Items.BUCKET);
        if (bucketSlot == -1) {
            return;
        }
        int previousSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(bucketSlot);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hitResult) instanceof InteractionResult.Success success
                && success.swingSource() == InteractionResult.SwingSource.CLIENT) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
        client.player.getInventory().setSelectedSlot(previousSlot);
    }

    private void drainWithBlock(Minecraft client, BlockPos pos) {
        int blockSlot = findSolidBlock(client);
        if (blockSlot == -1) {
            return;
        }
        int previousSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(blockSlot);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hitResult) instanceof InteractionResult.Success success
                && success.swingSource() == InteractionResult.SwingSource.CLIENT) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
        client.player.getInventory().setSelectedSlot(previousSlot);
    }

    private int findSolidBlock(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            Item item = client.player.getInventory().getItem(i).getItem();
            if (item instanceof BlockItem blockItem) {
                if (blockItem.getBlock() != Blocks.WATER && blockItem.getBlock() != Blocks.LAVA && blockItem.getBlock() != Blocks.AIR) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findHotbarItem(Minecraft client, Item item) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).is(item)) {
                return i;
            }
        }
        return -1;
    }
}
