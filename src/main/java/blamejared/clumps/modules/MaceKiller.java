package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

/**
 * Spoofs the player's position upward before attacking with a mace to
 * maximize fall-distance damage. Uses block-spoof: sends position packets
 * going up then back down in steps.
 *
 * Height is configurable via a slider in the options screen.
 */
public class MaceKiller extends ClumpsModule {
    private final Option.IntOption height = new Option.IntOption("Spoof Height", 10, 1, 50, 1);

    @Override
    public String getName() { return "Mace Killer"; }
    @Override
    public String getDescription() { return "Spoof fall distance for mace damage"; }
    @Override
    public String getCategory() { return "Mace"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(height); }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (!(client.hitResult instanceof EntityHitResult ehr)) return;

        Entity target = ehr.getEntity();

        // Only activate when holding a mace and clicking attack
        if (!client.player.getMainHandItem().is(Items.MACE)) return;
        if (!client.options.keyAttack.isDown()) return;

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        float yaw = client.player.getYRot();
        float pitch = client.player.getXRot();
        boolean onGround = client.player.onGround();

        int spoofBlocks = height.getValue();

        // Send position packets going UP in steps of ~10 blocks (server limit per packet)
        double step = 10.0;
        double currentY = y;
        double targetY = y + spoofBlocks;
        while (currentY < targetY) {
            currentY = Math.min(currentY + step, targetY);
            client.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    x, currentY, z, yaw, pitch, false, client.player.horizontalCollision
            ));
        }

        // Send position packets going DOWN back to the player
        while (currentY > y + 0.0001) {
            currentY = Math.max(currentY - step, y + 0.0001);
            client.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    x, currentY, z, yaw, pitch, false, client.player.horizontalCollision
            ));
        }

        // Attack the target
        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
    }
}
