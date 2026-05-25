package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;

import java.util.List;

public class PlayerESP extends ClumpsModule {
    private final Option.BoolOption tracers = new Option.BoolOption("Tracers", false);
    private final Option.DoubleOption tracerWidth = new Option.DoubleOption("Tracer Width", 1.5, 0.5, 5.0, 0.5);
    private final Option.IntOption tracerRange = new Option.IntOption("Tracer Range", 128, 16, 256, 8);

    @Override
    public String getName() { return "Player ESP"; }

    @Override
    public String getDescription() { return "Shows player glow outlines and tracer lines"; }

    @Override
    public String getCategory() { return "Visual"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(tracers, tracerWidth, tracerRange); }

    @Override
    public void onTick(Minecraft client) {}

    public boolean tracers() { return tracers.getValue(); }
    public float tracerWidth() { return tracerWidth.getValue().floatValue(); }
    public int tracerRange() { return tracerRange.getValue(); }
}
