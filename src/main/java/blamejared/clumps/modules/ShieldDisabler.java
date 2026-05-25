package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public class ShieldDisabler extends ClumpsModule {
    private final Option.IntOption hitDelay = new Option.IntOption("Hit Delay", 0, 0, 20, 1);
    private final Option.IntOption switchDelay = new Option.IntOption("Switch Delay", 0, 0, 20, 1);
    private final Option.BoolOption switchBack = new Option.BoolOption("Switch Back", true);
    private final Option.BoolOption stun = new Option.BoolOption("Stun", false);
    private final Option.BoolOption clickOnly = new Option.BoolOption("Click Only", false);
    private final Option.BoolOption holdAxe = new Option.BoolOption("Hold Axe", false);

    private int previousSlot = -1;
    private int hitClock = 0;
    private int switchClock = 0;

    @Override
    public String getName() {
        return "Shield Disabler";
    }

    @Override
    public String getDescription() { return "Disable enemy shields"; }

    @Override
    public String getCategory() {
        return "Combat";
    }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(hitDelay, switchDelay, switchBack, stun, clickOnly, holdAxe);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null) {
            return;
        }
        if (clickOnly.getValue() && !client.options.keyAttack.isDown()) {
            switchBack(client);
            return;
        }
        if (!(client.hitResult instanceof EntityHitResult hitResult) || !(hitResult.getEntity() instanceof Player target) || !target.isBlocking()) {
            switchBack(client);
            return;
        }
        if (holdAxe.getValue() && !isAxe(client.player.getMainHandItem().getItem())) {
            switchBack(client);
            return;
        }

        if (previousSlot == -1) {
            previousSlot = client.player.getInventory().getSelectedSlot();
        }

        int axeSlot = findAxeSlot(client);
        if (axeSlot == -1) {
            return;
        }
        if (!isAxe(client.player.getMainHandItem().getItem())) {
            setSlot(client, axeSlot);
        }
        if (switchClock++ < switchDelay.getValue()) {
            return;
        }
        if (hitClock++ < hitDelay.getValue()) {
            return;
        }

        attack(client, target);
        if (stun.getValue()) {
            attack(client, target);
        }
        hitClock = 0;
        switchClock = 0;
    }

    private void attack(Minecraft client, Player target) {
        client.player.connection.send(new ServerboundAttackPacket(target.getId()));
        client.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
    }

    private void switchBack(Minecraft client) {
        hitClock = 0;
        switchClock = 0;
        if (previousSlot != -1 && switchBack.getValue() && client.player != null) {
            setSlot(client, previousSlot);
        }
        previousSlot = -1;
    }

    private void setSlot(Minecraft client, int slot) {
        client.player.getInventory().setSelectedSlot(slot);
        client.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private int findAxeSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            if (isAxe(client.player.getInventory().getItem(i).getItem())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isAxe(Item item) {
        return item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE
                || item == Items.IRON_AXE || item == Items.STONE_AXE
                || item == Items.WOODEN_AXE || item == Items.GOLDEN_AXE;
    }
}
