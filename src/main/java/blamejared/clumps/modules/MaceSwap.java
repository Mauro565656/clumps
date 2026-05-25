package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public class MaceSwap extends ClumpsModule {
    private final Option.BoolOption enableWindBurst = new Option.BoolOption("Wind Burst", true);
    private final Option.BoolOption enableBreach = new Option.BoolOption("Breach", true);
    private final Option.BoolOption onlySword = new Option.BoolOption("Only Sword", false);
    private final Option.BoolOption onlyAxe = new Option.BoolOption("Only Axe", false);
    private final Option.BoolOption switchBack = new Option.BoolOption("Switch Back", true);
    private final Option.IntOption switchDelay = new Option.IntOption("Switch Delay", 0, 0, 20, 1);

    private boolean switching = false;
    private int previousSlot = -1;
    private int switchClock = 0;

    @Override
    public String getName() {
        return "Mace Swap";
    }

    @Override
    public String getDescription() { return "Auto-swap to mace when falling"; }

    @Override
    public String getCategory() {
        return "Mace";
    }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(enableWindBurst, enableBreach, onlySword, onlyAxe, switchBack, switchDelay);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || !switching) {
            return;
        }
        if (!switchBack.getValue()) {
            resetState();
            return;
        }
        if (switchClock++ < switchDelay.getValue()) {
            return;
        }
        if (previousSlot != -1) {
            setSlot(client, previousSlot);
        }
        resetState();
    }

    public void onStartAttack(Minecraft client) {
        if (client.player == null || client.level == null || !isValidWeapon(client.player.getMainHandItem().getItem())) {
            return;
        }
        int maceSlot = findMaceSlot(client);
        if (maceSlot == -1 || maceSlot == client.player.getInventory().getSelectedSlot()) {
            return;
        }
        previousSlot = client.player.getInventory().getSelectedSlot();
        setSlot(client, maceSlot);
        switching = true;
        switchClock = 0;
    }

    private boolean isValidWeapon(Item item) {
        boolean sword = item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD
                || item == Items.IRON_SWORD || item == Items.STONE_SWORD
                || item == Items.WOODEN_SWORD || item == Items.GOLDEN_SWORD;
        boolean axe = item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE
                || item == Items.IRON_AXE || item == Items.STONE_AXE
                || item == Items.WOODEN_AXE || item == Items.GOLDEN_AXE;
        if (onlySword.getValue() && onlyAxe.getValue()) {
            return sword || axe;
        }
        if (onlySword.getValue()) {
            return sword;
        }
        if (onlyAxe.getValue()) {
            return axe;
        }
        return sword || axe;
    }

    private int findMaceSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).is(Items.MACE)) {
                return i;
            }
        }
        return -1;
    }

    private void setSlot(Minecraft client, int slot) {
        client.player.getInventory().setSelectedSlot(slot);
        client.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private void resetState() {
        switching = false;
        switchClock = 0;
        previousSlot = -1;
    }
}
