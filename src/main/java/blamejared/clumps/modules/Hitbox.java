package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Expands player hitboxes via a mixin on Entity.getPickRadius().
 * The mixin in HitboxMixin handles the actual expansion.
 */
public class Hitbox extends ClumpsModule {
    private final Option.DoubleOption expand = new Option.DoubleOption("Expand", 0.5, 0.0, 2.0, 0.1);

    @Override
    public String getName() { return "Hitbox"; }

    @Override
    public String getDescription() { return "Expand player hitboxes"; }

    @Override
    public String getCategory() { return "Combat"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(expand); }

    public float getExpansion() { return (float) expand.getValue().doubleValue(); }

    @Override
    public void onTick(Minecraft client) {
        // Mixin handles the logic
    }
}
