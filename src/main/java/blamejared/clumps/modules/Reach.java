package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Extends the player's entity interaction range via a mixin
 * on Player.entityInteractionRange().
 */
public class Reach extends ClumpsModule {
    private final Option.DoubleOption reach = new Option.DoubleOption("Reach", 3.0, 1.0, 5.0, 0.1);

    @Override
    public String getName() { return "Reach"; }
    @Override
    public String getDescription() { return "Extend attack and block reach"; }
    @Override
    public String getCategory() { return "Combat"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(reach); }

    public double getReachDistance() { return reach.getValue(); }

    @Override
    public void onTick(Minecraft client) {
        // Mixin handles the logic
    }
}
