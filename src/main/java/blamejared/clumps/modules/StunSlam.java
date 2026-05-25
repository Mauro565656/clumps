package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

/**
 * When you attack a blocking player while holding an axe:
 *  1. The axe attack goes through normally (shield-disable happens via axe damage)
 *  2. Immediately after, switches to a Density mace for the follow-up hit
 *  3. Auto-switches back to the axe after swapBackDelay ticks
 */
public class StunSlam extends ClumpsModule {

    private final Option.BoolOption requireDensity = new Option.BoolOption("Require Density", true);
    private final Option.BoolOption onlyOnShield   = new Option.BoolOption("Only on Shield", true);
    private final Option.BoolOption autoSwitchBack = new Option.BoolOption("Auto Switch Back", true);
    private final Option.IntOption  swapBackDelay  = new Option.IntOption("Swap Back Delay", 2, 0, 20, 1);

    private int     backTimer      = 0;
    private boolean awaitingBack   = false;
    private int     storedAxeSlot  = -1;
    private int     pendingTargetId = -1;

    @Override public String getName()     { return "Stun Slam"; }
    @Override public String getDescription() { return "Stun and slam combo"; }
    @Override public String getCategory() { return "Combat"; }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(requireDensity, onlyOnShield, autoSwitchBack, swapBackDelay);
    }

    /**
     * Called from MinecraftAttackMixin just before the attack is processed.
     * The axe attack fires normally (breaking the shield). We then swap to the
     * mace so the NEXT attack or the follow-up in onTick hits with the mace.
     */
    public void onStartAttack(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) return;
        if (awaitingBack) return;

        // Must be holding an axe
        if (!client.player.getMainHandItem().is(ItemTags.AXES)) return;

        // Target must be a shield-blocking player (if onlyOnShield is on)
        if (!(client.crosshairPickEntity instanceof LivingEntity target)) return;
        if (onlyOnShield.getValue()) {
            if (!(target instanceof Player p) || !p.isBlocking()) return;
        }

        int maceSlot = findMaceSlot(client);
        if (maceSlot == -1) return;

        int currentSlot = client.player.getInventory().getSelectedSlot();

        // Schedule the slot switch for AFTER the axe swing sends (next tick),
        // so the axe hit lands first and disables the shield.
        if (autoSwitchBack.getValue()) {
            awaitingBack  = true;
            backTimer     = swapBackDelay.getValue() + 1;
            storedAxeSlot = currentSlot;
        }

        // Switch to mace one tick after attack — handled in onTick via timer
        // We store maceSlot for onTick; start a short countdown
        this.pendingMaceSlot = maceSlot;
        this.maceSwapTimer   = 1;
        this.pendingTargetId = target.getId();
    }

    private int pendingMaceSlot = -1;
    private int maceSwapTimer   = 0;

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) return;

        // Swap to mace after axe hit
        if (maceSwapTimer > 0) {
            maceSwapTimer--;
            if (maceSwapTimer == 0 && pendingMaceSlot >= 0) {
                setSlot(client, pendingMaceSlot);
                if (client.level != null && client.level.getEntity(pendingTargetId) instanceof LivingEntity target
                    && target.isAlive() && client.player.distanceTo(target) <= 4.5f) {
                    client.gameMode.attack(client.player, target);
                    client.player.swing(client.player.getUsedItemHand());
                }
                pendingMaceSlot = -1;
                pendingTargetId = -1;
            }
        }

        // Swap back to axe after delay
        if (!awaitingBack) return;
        if (backTimer-- > 0) return;

        if (storedAxeSlot >= 0) {
            setSlot(client, storedAxeSlot);
        }
        awaitingBack  = false;
        storedAxeSlot = -1;
        backTimer     = 0;
    }

    private int findMaceSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (!stack.is(Items.MACE)) continue;
            if (requireDensity.getValue()) {
                if (!hasDensityEnchant(stack)) continue;
            }
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
}
