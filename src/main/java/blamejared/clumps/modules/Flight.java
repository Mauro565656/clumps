package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Allows creative-style flight in survival mode.
 * Sends a small downward nudge every 20 ticks to avoid kick for flying.
 */
public class Flight extends ClumpsModule {
    private final Option.IntOption speed = new Option.IntOption("Speed", 10, 1, 50, 1);
    private final Option.BoolOption antiKick = new Option.BoolOption("Anti Kick", true);

    private int tickCounter = 0;

    @Override
    public String getName() { return "Flight"; }
    @Override
    public String getDescription() { return "Survival flight mode"; }
    @Override
    public String getCategory() { return "Movement"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(speed, antiKick); }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        var abilities = client.player.getAbilities();
        abilities.mayfly = true;
        abilities.flying = true;
        abilities.setFlyingSpeed((float)(speed.getValue() / 100.0));

        if (antiKick.getValue()) {
            tickCounter++;
            if (tickCounter > 20) {
                client.player.setDeltaMovement(
                        client.player.getDeltaMovement().x,
                        -0.04,
                        client.player.getDeltaMovement().z
                );
                tickCounter = 0;
            }
        }
    }

    /** Called when the module is toggled off to restore abilities. */
    public void onDisable(Minecraft client) {
        if (client.player != null) {
            var abilities = client.player.getAbilities();
            abilities.mayfly = false;
            abilities.flying = false;
        }
    }
}
