package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

public class AutoPotRefill extends ClumpsModule {
    private final Option.IntOption mode = new Option.IntOption("Mode", 0, 0, 1, 1);
    private final Option.IntOption delay = new Option.IntOption("Delay", 0, 0, 10, 1);
    private int clock = 0;

    @Override
    public String getName() {
        return "Auto Pot Refill";
    }

    @Override
    public String getDescription() { return "Refill hotbar potion slots"; }

    @Override
    public String getCategory() {
        return "Combat";
    }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(mode, delay);
    }

    @Override
    public void onTick(Minecraft client) {
        if (!(client.screen instanceof InventoryScreen) || client.player == null || client.gameMode == null) {
            return;
        }
        if (clock++ < delay.getValue()) {
            return;
        }

        int sourceSlot = findInventoryPotion(client);
        int hotbarSlot = findEmptyHotbarSlot(client);
        if (sourceSlot == -1 || hotbarSlot == -1) {
            return;
        }

        int menuSourceSlot = sourceSlot < 9 ? sourceSlot + 36 : sourceSlot;
        client.gameMode.handleContainerInput(client.player.containerMenu.containerId, menuSourceSlot, hotbarSlot, ContainerInput.SWAP, client.player);
        clock = 0;
    }

    private int findEmptyHotbarSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int findInventoryPotion(Minecraft client) {
        for (int i = 9; i < 36; i++) {
            if (isHealingPotion(client.player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isHealingPotion(ItemStack stack) {
        if (!stack.is(Items.SPLASH_POTION)) {
            return false;
        }
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        return name.contains("healing") || name.contains("health");
    }
}
