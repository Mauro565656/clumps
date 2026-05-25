package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

/**
 * When holding an axe and attacking a blocking player:
 *  - Uses gameMode.attack() each tick while the shield is up (triggers real axe hits, applying shield-disable)
 *  - Once shield breaks, switches to density mace, hits once, then switches back
 */
public class ShieldSlammer extends ClumpsModule {

    private final Option.BoolOption requireDensity = new Option.BoolOption("Require Density", true);
    private final Option.BoolOption onlyOnShield   = new Option.BoolOption("Only On Shield", true);
    private final Option.BoolOption autoSwitchBack = new Option.BoolOption("Auto Switch Back", true);
    private final Option.IntOption  swapBackDelay  = new Option.IntOption("Swap Back Delay", 2, 0, 10, 1);

    private boolean wasBlocking   = false;
    private int     maceHitTimer  = 0;
    private int     prevAxeSlot   = -1;

    @Override public String getName()     { return "Shield Slammer"; }
    @Override public String getDescription() { return "Mace + shield slam combo"; }
    @Override public String getCategory() { return "Combat"; }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(requireDensity, onlyOnShield, autoSwitchBack, swapBackDelay);
    }

    private int findMaceSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (!stack.is(Items.MACE)) continue;
            if (requireDensity.getValue() && !hasDensityEnchant(stack)) continue;
            return i;
        }
        return -1;
    }

    private boolean hasDensityEnchant(ItemStack stack) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) return false;
        for (var holder : enchants.keySet()) {
            if (holder.is(Enchantments.DENSITY)) return true;
        }
        return false;
    }

    private void setSlot(Minecraft client, int slot) {
        client.player.getInventory().setSelectedSlot(slot);
        client.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private boolean isHoldingAxe(Minecraft client) {
        return client.player.getMainHandItem().is(ItemTags.AXES);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;

        // Mace follow-up: switch to mace, attack once, switch back
        if (maceHitTimer > 0) {
            maceHitTimer--;
            if (maceHitTimer == 1) {
                int maceSlot = findMaceSlot(client);
                if (maceSlot != -1 && client.hitResult instanceof EntityHitResult ehr
                        && ehr.getEntity() instanceof Player target) {
                    setSlot(client, maceSlot);
                    client.gameMode.attack(client.player, target);
                }
            } else if (maceHitTimer == 0) {
                if (autoSwitchBack.getValue() && prevAxeSlot != -1) {
                    setSlot(client, prevAxeSlot);
                    prevAxeSlot = -1;
                }
            }
            return;
        }

        if (!isHoldingAxe(client)) { wasBlocking = false; return; }
        if (!client.options.keyAttack.isDown()) { wasBlocking = false; return; }

        if (!(client.hitResult instanceof EntityHitResult ehr)) { wasBlocking = false; return; }
        if (!(ehr.getEntity() instanceof Player target)) { wasBlocking = false; return; }

        boolean blocking = target.isBlocking();

        if (onlyOnShield.getValue() && !blocking) { wasBlocking = false; return; }

        // Shield just broke — queue mace follow-up
        if (wasBlocking && !blocking) {
            wasBlocking = false;
            int maceSlot = findMaceSlot(client);
            if (maceSlot != -1) {
                prevAxeSlot  = client.player.getInventory().getSelectedSlot();
                // 2 ticks for slot switch + N ticks delay before auto-switch back
                maceHitTimer = 2 + swapBackDelay.getValue();
            }
            return;
        }

        wasBlocking = blocking;

        // Spam axe attacks via gameMode.attack() so the server applies proper
        // axe damage including the shield-disable bonus.
        client.gameMode.attack(client.player, target);
    }
}
