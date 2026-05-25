package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/**
 * When holding left click while looking at a shielding player,
 * sends 5 attack packets per tick (~100 per second) to rapidly
 * drain their shield durability.
 */
public class ShieldDrainer extends ClumpsModule {

    @Override
    public String getName() { return "Shield Drainer"; }
    @Override
    public String getDescription() { return "Drain enemy shields"; }
    @Override
    public String getCategory() { return "Combat"; }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        // Must have attack held (left click)
        if (!client.options.keyAttack.isDown()) return;

        // Must be looking at a player who is blocking
        if (!(client.hitResult instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof Player target)) return;
        if (!target.isBlocking()) return;

        // Send 5 attack packets per tick = 100 cps at 20 tps
        for (int i = 0; i < 5; i++) {
            client.player.connection.send(new ServerboundAttackPacket(target.getId()));
            client.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
    }
}
