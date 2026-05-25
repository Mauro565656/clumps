package blamejared.clumps.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;

/**
 * .vclip &lt;blocks&gt; — teleports the player vertically by the given number
 * of blocks by sending a burst of movement packets in a single tick.
 *
 * Translated from the Meteor reference:
 *   - Uses Mojang mappings (ServerboundMovePlayerPacket.Pos).
 *   - Instead of the old "OnGroundOnly" no-delta packet (which doesn't exist
 *     by that name in modern mappings), sends "Pos" packets at the current
 *     position, which is equivalent for Paper's packet-counting logic.
 *   - For vehicles, falls back to direct setPos() since the vehicle move
 *     packet shape changes across versions; avoids a compile-time dep.
 *
 * Note: Paper's PaperClip / VaultClip trick relied on 1.8-era movement
 * handling and old packet-per-tick limits. Whether it still works on MC 26.1
 * servers is entirely dependent on the anticheat; this command is provided
 * because the user asked for it, not as a guarantee it bypasses anything.
 */
public class VClipCommand extends ChatCommand {

    public VClipCommand() {
        super("vclip",
                "Clip through blocks vertically.",
                "<blocks>");
    }

    @Override
    public boolean run(Minecraft client, String[] args) {
        if (args.length < 1) return false;
        LocalPlayer player = client.player;
        if (player == null) {
            error(client, "No player.");
            return true;
        }

        double blocks;
        try {
            blocks = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            error(client, "'" + args[0] + "' is not a number.");
            return true;
        }

        // Paper allows ~10 blocks per move packet; we can send ~20 per tick
        // before being kicked. Beyond 200 blocks, fall back to a single packet.
        int packetsRequired = (int) Math.ceil(Math.abs(blocks / 10.0));
        if (packetsRequired > 20 || packetsRequired < 1) {
            packetsRequired = 1;
        }

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        boolean onGround = player.onGround();
        boolean hCollide = player.horizontalCollision;

        if (player.isPassenger()) {
            // Vehicle path: skip the packet trick (vehicle move packet shape is
            // unstable across versions) and just reposition the vehicle.
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                vehicle.setPos(vehicle.getX(), vehicle.getY() + blocks, vehicle.getZ());
            }
            info(client, "VClip " + blocks + " blocks (vehicle).");
            return true;
        }

        // Send (packetsRequired - 1) "no-delta" packets at the current position.
        // For Paper's counter these all count as move packets in the same tick.
        for (int i = 0; i < packetsRequired - 1; i++) {
            player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y, z, onGround, hCollide));
        }
        // Final packet: actual displacement.
        player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y + blocks, z, onGround, hCollide));
        player.setPos(x, y + blocks, z);

        info(client, "VClip " + blocks + " blocks.");
        return true;
    }
}
