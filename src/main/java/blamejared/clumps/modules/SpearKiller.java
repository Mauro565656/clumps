package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class SpearKiller extends ClumpsModule {

    // Mode: 0 = Lunge, 1 = Blink
    private final Option.IntOption mode      = new Option.IntOption("Mode 0=Lunge 1=Blink", 0, 0, 1, 1);
    private final Option.IntOption height    = new Option.IntOption("Spoof Height", 20, 1, 50, 1);
    private final Option.IntOption velocity  = new Option.IntOption("Velocity", 12, 1, 30, 1);
    // Blink options
    private final Option.IntOption blinkDelay = new Option.IntOption("Blink Delay ms", 500, 50, 3000, 50);
    private final Option.BoolOption noFall   = new Option.BoolOption("No Fall", true);
    private final Option.BoolOption aimbot   = new Option.BoolOption("Aimbot (Blink)", false);

    // Blink: buffered packets
    private final List<ServerboundMovePlayerPacket> blinkQueue = new ArrayList<>();
    private boolean blinking = false;
    private int blinkTimer = 0;

    @Override
    public String getName() { return "Spear Killer"; }
    @Override
    public String getDescription() { return "Auto-attack with mace when falling"; }
    @Override
    public String getCategory() { return "Spear"; }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(mode, height, velocity, blinkDelay, noFall, aimbot);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (mode.getValue() == 1 && blinking) {
            blinkTimer++;
            int ticksDelay = blinkDelay.getValue() / 50;
            if (ticksDelay < 1) ticksDelay = 1;
            if (blinkTimer >= ticksDelay) {
                flushBlinkQueue(client);
            }
        }

        // No fall: cancel fall damage velocity when enabled
        if (noFall.getValue() && client.player.fallDistance > 0) {
            // Only suppress when blink is active
            if (mode.getValue() == 1 && blinking) {
                client.player.fallDistance = 0;
            }
        }
    }

    public void onStartAttack(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;
        ItemStack heldStack = client.player.getMainHandItem();
        if (!isSupportedSpear(heldStack)) return;
        if (!heldStack.isItemEnabled(client.level.enabledFeatures())) return;
        if (client.player.cannotAttackWithItem(heldStack, 0)) return;
        if (!heldStack.has(DataComponents.PIERCING_WEAPON)) return;

        if (mode.getValue() == 0) {
            doLunge(client);
        } else {
            doBlink(client);
        }
    }

    private void doLunge(Minecraft client) {
        LocalPlayer player = client.player;
        double x     = player.getX();
        double y     = player.getY();
        double z     = player.getZ();
        float  yaw   = player.getYRot();
        float  pitch = player.getXRot();

        int    spoofBlocks = height.getValue();
        double step        = 10.0;
        double currentY    = y;
        double targetY     = y + spoofBlocks;
        while (currentY < targetY) {
            currentY = Math.min(currentY + step, targetY);
            player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    x, currentY, z, yaw, pitch, false, player.horizontalCollision
            ));
        }

        // Lunge toward look direction
        Vec3 look    = player.getLookAngle();
        double boost = velocity.getValue() / 10.0D;
        player.setDeltaMovement(look.scale(boost).add(0, boost * 0.3, 0));
    }

    private void doBlink(Minecraft client) {
        LocalPlayer player = client.player;

        // Aimbot: look toward nearest target before blink
        if (aimbot.getValue()) {
            Entity target = findNearestTarget(client);
            if (target != null) {
                aimAt(client, target);
            }
        }

        double x     = player.getX();
        double y     = player.getY();
        double z     = player.getZ();
        float  yaw   = player.getYRot();
        float  pitch = player.getXRot();

        // Queue spoofed height packets instead of sending immediately
        blinkQueue.clear();
        int    spoofBlocks = height.getValue();
        double step        = 10.0;
        double currentY    = y;
        double targetY     = y + spoofBlocks;
        while (currentY < targetY) {
            currentY = Math.min(currentY + step, targetY);
            blinkQueue.add(new ServerboundMovePlayerPacket.PosRot(
                    x, currentY, z, yaw, pitch, false, player.horizontalCollision
            ));
        }

        blinking   = true;
        blinkTimer = 0;

        // Apply velocity immediately — the server will see us at height when packets arrive
        double boost = velocity.getValue() / 10.0D;
        player.setDeltaMovement(player.getDeltaMovement().add(0.0D, boost, 0.0D));
    }

    private void flushBlinkQueue(Minecraft client) {
        if (client.player == null) {
            blinkQueue.clear();
            blinking = false;
            blinkTimer = 0;
            return;
        }
        for (var pkt : blinkQueue) {
            client.player.connection.send(pkt);
        }
        blinkQueue.clear();
        blinking   = false;
        blinkTimer = 0;
    }

    private Entity findNearestTarget(Minecraft client) {
        if (client.player == null || client.level == null) return null;
        Entity best  = null;
        double bestD = Double.MAX_VALUE;
        for (var entity : client.level.entitiesForRendering()) {
            if (entity == client.player) continue;
            if (!(entity instanceof Player)) continue;
            double d = entity.distanceToSqr(client.player);
            if (d < bestD) { bestD = d; best = entity; }
        }
        return best;
    }

    private void aimAt(Minecraft client, Entity target) {
        LocalPlayer player = client.player;
        Vec3   from   = player.getEyePosition(1.0f);
        Vec3   to     = target.getBoundingBox().getCenter();
        Vec3   diff   = to.subtract(from);
        double dist   = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float  yaw    = (float)(Mth.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0f;
        float  pitch  = (float)(-(Mth.atan2(diff.y, dist) * (180.0 / Math.PI)));
        player.setYRot(yaw);
        player.setXRot(pitch);
    }

    private boolean isSupportedSpear(ItemStack heldStack) {
        return heldStack.is(net.minecraft.tags.ItemTags.SPEARS);
    }
}
