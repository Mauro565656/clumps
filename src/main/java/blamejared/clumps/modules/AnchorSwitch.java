package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Whenever you place a respawn anchor, automatically switches to glowstone to
 * charge it, then switches back to the anchor slot after it's charged.
 * Mirrors the ClickCrystals AnchorSwitch behavior.
 */
public class AnchorSwitch extends ClumpsModule {

    private final Option.BoolOption autoBack = new Option.BoolOption("Auto Switch Back", true);
    private final Option.IntOption backDelay = new Option.IntOption("Back Delay (ticks)", 2, 0, 20, 1);

    private int anchorSlot = -1;
    private int backTimer  = 0;
    private boolean awaitingBack = false;
    private int swapCooldown = 0;

    @Override
    public String getName() { return "Anchor Switch"; }
    @Override
    public String getDescription() { return "Auto-switch to crystals after placing anchors"; }
    @Override
    public String getCategory() { return "Combat"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(autoBack, backDelay); }

    private int findHotbar(Minecraft client, net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    private void setSlot(Minecraft client, int slot) {
        client.player.getInventory().setSelectedSlot(slot);
        client.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    /** Called from ClumpsClient.UseBlockCallback when an anchor is placed. */
    public void onAnchorPlaced(Minecraft client) {
        if (!enabled || client.player == null) return;
        if (swapCooldown > 0) return;

        int glowSlot = findHotbar(client, Items.GLOWSTONE);
        if (glowSlot == -1) return;

        anchorSlot = client.player.getInventory().getSelectedSlot();
        awaitingBack = false;
        backTimer = 0;

        setSlot(client, glowSlot);
        swapCooldown = 2;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (swapCooldown > 0) swapCooldown--;
        if (!awaitingBack && anchorSlot == -1) return;

        // Check if we are holding glowstone and looking at an anchor — if the anchor is
        // now charged (or we started charging), begin the back-swap countdown.
        if (!awaitingBack && anchorSlot != -1) {
            // Check if the anchor we're looking at is charged
            if (client.hitResult instanceof BlockHitResult bhr) {
                var state = client.level.getBlockState(bhr.getBlockPos());
                if (state.is(Blocks.RESPAWN_ANCHOR) && state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
                    if (autoBack.getValue()) {
                        awaitingBack = true;
                        backTimer    = backDelay.getValue();
                    } else {
                        anchorSlot = -1;
                    }
                }
            }
            // Also start back-swap if we're no longer holding glowstone (charge happened)
            if (!client.player.getMainHandItem().is(Items.GLOWSTONE)) {
                if (autoBack.getValue() && anchorSlot != -1) {
                    awaitingBack = true;
                    backTimer    = backDelay.getValue();
                } else {
                    anchorSlot = -1;
                }
            }
        }

        if (awaitingBack) {
            if (backTimer-- > 0) return;
            if (anchorSlot >= 0 && anchorSlot < 9) {
                setSlot(client, anchorSlot);
            }
            anchorSlot   = -1;
            awaitingBack = false;
            backTimer    = 0;
        }
    }
}
