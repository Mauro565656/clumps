package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

public class AutoPot extends ClumpsModule {
    private final Option.IntOption minHealth = new Option.IntOption("Min Health", 10, 1, 20, 1);
    private final Option.IntOption switchDelay = new Option.IntOption("Switch Delay", 0, 0, 10, 1);
    private final Option.IntOption throwDelay = new Option.IntOption("Throw Delay", 0, 0, 10, 1);
    private final Option.BoolOption switchBack = new Option.BoolOption("Switch Back", true);
    private final Option.BoolOption lookDown = new Option.BoolOption("Look Down", true);

    private int switchClock = 0;
    private int throwClock = 0;
    private int previousSlot = -1;
    private float previousPitch = 0.0F;
    private boolean storedPitch = false;

    @Override
    public String getName() {
        return "Auto Pot";
    }

    @Override
    public String getDescription() { return "Auto-splash healing potions"; }

    @Override
    public String getCategory() {
        return "Combat";
    }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(minHealth, switchDelay, throwDelay, switchBack, lookDown);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null || client.screen != null) {
            return;
        }
        if (client.player.getHealth() > minHealth.getValue()) {
            restoreState(client);
            return;
        }

        ItemStack held = client.player.getMainHandItem();
        if (!isHealingPotion(held)) {
            if (switchClock++ < switchDelay.getValue()) {
                return;
            }
            int potionSlot = findHealingPotion(client);
            if (potionSlot == -1) {
                return;
            }
            if (previousSlot == -1 && switchBack.getValue()) {
                previousSlot = client.player.getInventory().getSelectedSlot();
            }
            if (!storedPitch && lookDown.getValue()) {
                previousPitch = client.player.getXRot();
                storedPitch = true;
            }
            client.player.getInventory().setSelectedSlot(potionSlot);
            switchClock = 0;
            return;
        }

        if (throwClock++ < throwDelay.getValue()) {
            return;
        }
        if (lookDown.getValue()) {
            client.player.setXRot(90.0F);
        }
        if (client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND) instanceof InteractionResult.Success success
                && success.swingSource() == InteractionResult.SwingSource.CLIENT) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
        throwClock = 0;
    }

    private void restoreState(Minecraft client) {
        switchClock = 0;
        throwClock = 0;
        if (previousSlot != -1 && switchBack.getValue()) {
            client.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
        }
        if (storedPitch) {
            client.player.setXRot(previousPitch);
            storedPitch = false;
        }
    }

    private int findHealingPotion(Minecraft client) {
        for (int i = 0; i < 9; i++) {
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
