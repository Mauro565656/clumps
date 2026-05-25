package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AntiWeb extends ClumpsModule {
    private final Option.IntOption range = new Option.IntOption("Range", 4, 1, 6, 1);
    private final Option.IntOption placeDelay = new Option.IntOption("Place Delay", 5, 0, 20, 1);
    private final Option.BoolOption onClick = new Option.BoolOption("On Click", false);
    private final Option.BoolOption holdingBucket = new Option.BoolOption("Holding Bucket", false);
    private final Option.BoolOption whenBreaking = new Option.BoolOption("When Breaking", false);

    private int delayClock = 0;
    private boolean pickupWater = false;
    private int previousSlot = -1;

    @Override
    public String getName() {
        return "AntiWeb";
    }

    @Override
    public String getDescription() { return "Break webs automatically"; }

    @Override
    public String getCategory() {
        return "Combat";
    }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(range, placeDelay, onClick, holdingBucket, whenBreaking);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }

        if (pickupWater) {
            if (client.player.getMainHandItem().is(Items.BUCKET)) {
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                client.player.swing(InteractionHand.MAIN_HAND);
            }
            if (previousSlot != -1) {
                client.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
            pickupWater = false;
            return;
        }

        if (!shouldRun(client)) {
            return;
        }
        if (delayClock++ < placeDelay.getValue()) {
            return;
        }

        BlockPos webPos = findNearbyWeb(client);
        if (webPos == null) {
            return;
        }

        int bucketSlot = findHotbarItem(client, Items.WATER_BUCKET);
        if (bucketSlot == -1) {
            return;
        }

        previousSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(bucketSlot);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(webPos), Direction.UP, webPos, false);
        if (client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hitResult) instanceof InteractionResult.Success success) {
            if (success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                client.player.swing(InteractionHand.MAIN_HAND);
            }
            pickupWater = true;
            delayClock = 0;
        }
    }

    private boolean shouldRun(Minecraft client) {
        if (client.screen != null) {
            return false;
        }
        if (onClick.getValue() && !client.options.keyAttack.isDown()) {
            return false;
        }
        if (holdingBucket.getValue() && !client.player.getMainHandItem().is(Items.WATER_BUCKET)) {
            return false;
        }
        if (whenBreaking.getValue() && client.gameMode.isDestroying()) {
            return false;
        }
        return true;
    }

    private BlockPos findNearbyWeb(Minecraft client) {
        BlockPos center = client.player.blockPosition();
        int r = range.getValue();
        for (int y = -1; y <= 2; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (client.level.getBlockState(pos).is(Blocks.COBWEB)) {
                        return pos;
                    }
                }
            }
        }
        return null;
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
