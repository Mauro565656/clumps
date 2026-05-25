package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

import java.util.List;

public class AutoTotem extends ClumpsModule {
    private final Option.IntOption  delay       = new Option.IntOption("Delay (ticks)",      2, 0, 20, 1);
    private final Option.IntOption  stayOpenFor = new Option.IntOption("Stay Open (ticks)",  0, 0, 20, 1);
    private final Option.BoolOption unnamedOnly = new Option.BoolOption("Unnamed Only",       false);
    private final Option.BoolOption autoDoubleHand = new Option.BoolOption("Auto Double Hand", false);
    private final Option.IntOption  doubleHandSlot = new Option.IntOption("Double Hand Slot", 1, 1, 9, 1);

    private int clock     = -1;
    private int closeClock = -1;
    private boolean weOpenedInv = false;
    private boolean bypassLock = false;
    /** Tracks how many ticks we've been trying to equip without success. */
    private int equipAttemptTicks = 0;
    private static final int MAX_EQUIP_ATTEMPTS = 10;

    @Override
    public String getName() { return "Auto Totem"; }
    @Override
    public String getDescription() { return "Automatically equips totems from inventory"; }
    @Override
    public String getCategory() { return "Combat"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(delay, stayOpenFor, unnamedOnly, autoDoubleHand, doubleHandSlot); }

    private boolean needsTotem(Minecraft client) {
        return client.player.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING;
    }

    private boolean needsDoubleHandTotem(Minecraft client) {
        if (!autoDoubleHand.getValue()) return false;
        return client.player.getInventory().getItem(doubleHandSlot.getValue() - 1).getItem() != Items.TOTEM_OF_UNDYING;
    }

    /** Returns inventory index (0-35) of first eligible totem found, or -1. */
    private int findTotemSlot(Minecraft client) {
        // Prefer main inventory (non-hotbar) first
        for (int i = 9; i < 36; i++) {
            if (isEligibleTotem(client, i)) return i;
        }
        for (int i = 0; i < 9; i++) {
            if (isEligibleTotem(client, i)) return i;
        }
        return -1;
    }

    private boolean isEligibleTotem(Minecraft client, int slot) {
        var stack = client.player.getInventory().getItem(slot);
        if (stack.getItem() != Items.TOTEM_OF_UNDYING) return false;
        if (unnamedOnly.getValue() && stack.getCustomName() != null) return false;
        return true;
    }

    private boolean isInvOpen(Minecraft client) {
        return client.screen instanceof InventoryScreen;
    }

    public boolean shouldLockContainerClicks(Minecraft client) {
        return enabled && client.player != null && isInvOpen(client) && (weOpenedInv || needsTotem(client) || needsDoubleHandTotem(client)) && !bypassLock;
    }

    private void resetState() {
        weOpenedInv = false;
        clock = -1;
        closeClock = -1;
        equipAttemptTicks = 0;
        bypassLock = false;
    }

    private int toMenuSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    private void swapIntoSlot(Minecraft client, int sourceSlot, int targetButton) {
        bypassLock = true;
        client.gameMode.handleContainerInput(client.player.containerMenu.containerId, toMenuSlot(sourceSlot), targetButton, ContainerInput.SWAP, client.player);
        bypassLock = false;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) return;

        if (client.screen != null && !(client.screen instanceof InventoryScreen)) return;

        if (weOpenedInv && !isInvOpen(client)) {
            resetState();
            return;
        }

        if ((needsTotem(client) || needsDoubleHandTotem(client)) && findTotemSlot(client) != -1 && !isInvOpen(client)) {
            client.setScreen(new InventoryScreen(client.player));
            weOpenedInv = true;
            equipAttemptTicks = 0;
        }

        if (!isInvOpen(client)) {
            resetState();
            return;
        }

        if (clock == -1)      clock      = delay.getValue();
        if (closeClock == -1) closeClock = stayOpenFor.getValue();

        if (clock > 0) { clock--; return; }

        // Equip totem via SWAP click — button 40 = offhand slot
        if (needsTotem(client)) {
            int totemSlot = findTotemSlot(client);
            if (totemSlot != -1) {
                // Translate player inventory index -> InventoryMenu slot index
                // hotbar 0-8  -> menu slots 36-44
                // main   9-35 -> menu slots 9-35
                swapIntoSlot(client, totemSlot, 40);
            }

            // Track how long we've been trying — if a plugin blocks the move, give up and close
            equipAttemptTicks++;
            if (equipAttemptTicks >= MAX_EQUIP_ATTEMPTS) {
                // Plugin is blocking this totem slot, close inventory and stop trying
                if (weOpenedInv) {
                    client.setScreen(null);
                }
                resetState();
                return;
            }
        }

        if (!needsTotem(client) && needsDoubleHandTotem(client)) {
            int totemSlot = findTotemSlot(client);
            if (totemSlot != -1) swapIntoSlot(client, totemSlot, doubleHandSlot.getValue() - 1);
        }

        if (!needsTotem(client) && !needsDoubleHandTotem(client) && weOpenedInv) {
            if (closeClock > 0) { closeClock--; return; }
            client.setScreen(null);
            resetState();
        }
    }
}
