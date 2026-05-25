package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Automated respawn anchor combo (based on Lucid's AnchorMacro):
 * - Only activates while holding right click
 * - If looking at an uncharged anchor: switch to glowstone, charge it
 * - If looking at a charged anchor: switch to explode slot, explode it
 * - Supports "Only Own" to only interact with anchors you placed
 * - Supports "Only Charge" to skip the explode step
 */
public class Anchorer extends ClumpsModule {
    private final Option.IntOption switchDelay    = new Option.IntOption("Switch Delay",       0, 0, 20, 1);
    private final Option.IntOption glowstoneDelay = new Option.IntOption("Glow Delay",         0, 0, 20, 1);
    private final Option.IntOption explodeDelay   = new Option.IntOption("Explode Delay",      0, 0, 20, 1);
    private final Option.IntOption explodeSlot    = new Option.IntOption("Explode Slot",        1, 1, 9, 1);
    private final Option.BoolOption onlyOwn       = new Option.BoolOption("Only Own",           false);
    private final Option.BoolOption onlyCharge    = new Option.BoolOption("Only Charge",        false);

    private int switchClock    = 0;
    private int glowstoneClock = 0;
    private int explodeClock   = 0;

    /** Tracks anchor positions placed by the player while this module is active. */
    private final Set<BlockPos> ownedAnchors = new HashSet<>();

    @Override
    public String getName() { return "Anchorer"; }
    @Override
    public String getDescription() { return "Auto-place and charge respawn anchors"; }
    @Override
    public String getCategory() { return "Combat"; }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(switchDelay, glowstoneDelay, explodeDelay, explodeSlot, onlyOwn, onlyCharge);
    }

    private void setSlot(Minecraft client, int slot) {
        client.player.getInventory().setSelectedSlot(slot);
        client.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private int findHotbar(Minecraft client, net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).getItem() == item) return i;
        }
        return -1;
    }

    /** Called from ClumpsClient when the player places a respawn anchor. */
    public void onBlockPlaced(BlockPos pos, Minecraft client) {
        ownedAnchors.add(pos.immutable());
    }

    public void trackPlaceTarget(BlockPos pos) {}
    public BlockPos getLastPlaceTarget() { return null; }

    private boolean isAnchorCharged(Minecraft client, BlockPos pos) {
        var state = client.level.getBlockState(pos);
        return state.is(Blocks.RESPAWN_ANCHOR) && state.getValue(RespawnAnchorBlock.CHARGE) > 0;
    }

    private boolean isAnchorNotCharged(Minecraft client, BlockPos pos) {
        var state = client.level.getBlockState(pos);
        return state.is(Blocks.RESPAWN_ANCHOR) && state.getValue(RespawnAnchorBlock.CHARGE) == 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;
        if (client.screen != null) return;

        // Only activate while holding right click
        if (!client.options.keyUse.isDown()) {
            switchClock = 0;
            glowstoneClock = 0;
            explodeClock = 0;
            return;
        }

        if (!(client.hitResult instanceof BlockHitResult bhr)) return;
        BlockPos targetPos = bhr.getBlockPos();

        if (!client.level.getBlockState(targetPos).is(Blocks.RESPAWN_ANCHOR)) return;

        // Only Own check
        if (onlyOwn.getValue() && !ownedAnchors.contains(targetPos)) return;

        // ── Uncharged anchor: switch to glowstone and charge ──
        if (isAnchorNotCharged(client, targetPos)) {
            // Need to switch to glowstone first
            if (!client.player.getMainHandItem().is(Items.GLOWSTONE)) {
                int glowSlot = findHotbar(client, Items.GLOWSTONE);
                if (glowSlot == -1) return;

                if (switchClock < switchDelay.getValue()) { switchClock++; return; }
                switchClock = 0;
                setSlot(client, glowSlot);
                return;
            }

            // Holding glowstone, charge the anchor
            if (glowstoneClock < glowstoneDelay.getValue()) { glowstoneClock++; return; }
            glowstoneClock = Math.max(1, glowstoneDelay.getValue());
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, bhr);
            return;
        }

        // ── Charged anchor: switch to explode slot and detonate ──
        if (isAnchorCharged(client, targetPos)) {
            if (onlyCharge.getValue()) return;

            int slot = explodeSlot.getValue() - 1; // Convert 1-9 to 0-8
            if (client.player.getInventory().getSelectedSlot() != slot) {
                if (switchClock < switchDelay.getValue()) { switchClock++; return; }
                switchClock = 0;
                setSlot(client, slot);
                return;
            }

            // On the correct slot, explode
            if (explodeClock < explodeDelay.getValue()) { explodeClock++; return; }
            explodeClock = Math.max(1, explodeDelay.getValue());
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, bhr);
            ownedAnchors.remove(targetPos);
        }
    }
}
