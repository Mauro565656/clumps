package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

/**
 * Automatically attacks players when the crosshair is over them.
 * Respects weapon attack speed cooldowns.
 */
public class TriggerBot extends ClumpsModule {
    private final Option.IntOption swordDelay = new Option.IntOption("Sword Delay (ms)", 550, 0, 1000, 10);
    private final Option.IntOption axeDelay   = new Option.IntOption("Axe Delay (ms)",   800, 0, 1000, 10);
    private final Option.BoolOption onlyLeftClick = new Option.BoolOption("On Left Click", false);
    private final Option.BoolOption checkShield   = new Option.BoolOption("Check Shield",  false);
    private final Option.BoolOption allEntities   = new Option.BoolOption("All Entities",  false);

    private long lastAttackTime = 0;

    @Override
    public String getName() { return "Trigger Bot"; }
    @Override
    public String getDescription() { return "Auto-attack when aiming at entities"; }
    @Override
    public String getCategory() { return "Combat"; }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(swordDelay, axeDelay, onlyLeftClick, checkShield, allEntities);
    }

    private boolean isAxe(Item item) {
        return item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE
                || item == Items.IRON_AXE || item == Items.STONE_AXE
                || item == Items.WOODEN_AXE || item == Items.GOLDEN_AXE;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (client.screen != null) return;

        // On Left Click check
        if (onlyLeftClick.getValue() && !client.options.keyAttack.isDown()) return;

        // Must be looking at an entity
        if (!(client.hitResult instanceof EntityHitResult ehr)) return;
        var target = ehr.getEntity();

        // Only players check (unless All Entities)
        if (!allEntities.getValue() && !(target instanceof Player)) return;
        if (target == client.player) return;

        // Check Shield - don't attack if blocking and facing us
        if (checkShield.getValue() && target instanceof Player p && p.isBlocking()) return;

        // Determine delay based on held weapon
        Item held = client.player.getMainHandItem().getItem();
        int delayMs;
        if (isAxe(held)) {
            delayMs = axeDelay.getValue();
        } else {
            delayMs = swordDelay.getValue();
        }

        long now = System.currentTimeMillis();
        if (now - lastAttackTime < delayMs) return;

        lastAttackTime = now;
        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
    }
}
