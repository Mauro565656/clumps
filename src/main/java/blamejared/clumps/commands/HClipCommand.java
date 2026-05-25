package blamejared.clumps.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * .hclip &lt;blocks&gt; — teleports the player (and vehicle, if any) forward
 * by the given number of blocks in the direction they are facing,
 * potentially clipping through walls if the server doesn't validate movement.
 *
 * Translated from the Meteor Client reference the user supplied; uses Mojang
 * mappings and this mod's conventions (no dependency on Meteor's Command base).
 */
public class HClipCommand extends ChatCommand {

    public HClipCommand() {
        super("hclip",
                "Clip through blocks horizontally in the direction you're facing.",
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

        // Forward vector from yaw only (no pitch — horizontal clip).
        // Equivalent to Meteor's Vec3d.fromPolar(0, yaw).normalize().
        float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0.0, Mth.cos(yawRad)).normalize();

        double dx = forward.x * blocks;
        double dz = forward.z * blocks;

        if (player.isPassenger()) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                vehicle.setPos(vehicle.getX() + dx, vehicle.getY(), vehicle.getZ() + dz);
            }
        }

        player.setPos(player.getX() + dx, player.getY(), player.getZ() + dz);
        info(client, "HClip " + blocks + " blocks.");
        return true;
    }
}
