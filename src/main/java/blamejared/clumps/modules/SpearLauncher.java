package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SpearLauncher extends ClumpsModule {
    private final Option.IntOption height   = new Option.IntOption("Spoof Height", 20, 1, 50, 1);
    private final Option.IntOption velocity = new Option.IntOption("Velocity", 12, 1, 30, 1);

    @Override
    public String getName() { return "Spear Launcher"; }
    @Override
    public String getDescription() { return "Launch with mace"; }
    @Override
    public String getCategory() { return "Spear"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(height, velocity); }

    @Override
    public void onTick(Minecraft client) {}

    public void onStartAttack(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;
        ItemStack heldStack = client.player.getMainHandItem();
        if (!isSupportedSpear(heldStack)) return;
        if (!heldStack.isItemEnabled(client.level.enabledFeatures())) return;
        if (client.player.cannotAttackWithItem(heldStack, 0)) return;
        if (!heldStack.has(DataComponents.PIERCING_WEAPON)) return;

        double x     = client.player.getX();
        double y     = client.player.getY();
        double z     = client.player.getZ();
        float  yaw   = client.player.getYRot();
        float  pitch = client.player.getXRot();

        int    spoofBlocks = height.getValue();
        double step        = 10.0;
        double currentY    = y;
        double targetY     = y + spoofBlocks;
        while (currentY < targetY) {
            currentY = Math.min(currentY + step, targetY);
            client.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    x, currentY, z, yaw, pitch, false, client.player.horizontalCollision
            ));
        }
        double verticalBoost = velocity.getValue() / 10.0D;
        client.player.setDeltaMovement(client.player.getDeltaMovement().add(0.0D, verticalBoost, 0.0D));
    }

    private boolean isSupportedSpear(ItemStack heldStack) {
        Item item = heldStack.getItem();
        String id = item.getDescriptionId();
        return id.endsWith(".diamond_spear") || id.endsWith(".netherite_spear");
    }
}
