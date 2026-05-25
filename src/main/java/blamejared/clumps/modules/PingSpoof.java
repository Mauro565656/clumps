package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Delays keepalive packet responses to spoof a higher ping.
 * The mixin in PingSpoofMixin handles the packet interception.
 */
public class PingSpoof extends ClumpsModule {
    private final Option.IntOption ping = new Option.IntOption("Ping (ms)", 200, 0, 1000, 50);

    @Override
    public String getName() { return "Ping Spoof"; }
    @Override
    public String getDescription() { return "Spoof your ping/latency"; }
    @Override
    public String getCategory() { return "Misc"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(ping); }

    public int getDelay() { return ping.getValue(); }

    @Override
    public void onTick(Minecraft client) {
        // Mixin handles the logic
    }
}
