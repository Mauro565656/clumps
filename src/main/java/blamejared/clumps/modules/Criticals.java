package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.List;

public class Criticals extends ClumpsModule {
    private final Option.IntOption mode = new Option.IntOption("Mode", 2, 0, 2, 1);

    @Override
    public String getName() {
        return "Criticals";
    }

    @Override
    public String getDescription() { return "Guarantee critical hits on attacks"; }

    @Override
    public String getCategory() {
        return "Combat";
    }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(mode);
    }

    @Override
    public void onTick(Minecraft client) {
    }

    public void onStartAttack(Minecraft client) {
        if (client.player == null || client.level == null || !canCrit(client)) {
            return;
        }

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        boolean horizontalCollision = client.player.horizontalCollision;

        if (mode.getValue() == 0) {
            client.player.jumpFromGround();
            return;
        }
        client.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625D, z, false, horizontalCollision));
        client.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, horizontalCollision));
    }

    private boolean canCrit(Minecraft client) {
        return client.player.onGround()
                && !client.player.isInWater()
                && !client.player.onClimbable()
                && !client.player.isPassenger();
    }
}
