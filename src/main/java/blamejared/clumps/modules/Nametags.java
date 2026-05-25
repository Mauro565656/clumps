package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;

import java.util.List;

public class Nametags extends ClumpsModule {

    private final Option.BoolOption showHealth    = new Option.BoolOption("Show Health", true);
    private final Option.BoolOption showDistance  = new Option.BoolOption("Show Distance", true);
    private final Option.BoolOption showPing      = new Option.BoolOption("Show Ping", true);
    private final Option.BoolOption showGamemode  = new Option.BoolOption("Show Gamemode", false);
    private final Option.BoolOption showItems     = new Option.BoolOption("Show Items", true);
    private final Option.BoolOption showArmor     = new Option.BoolOption("Show Armor", true);
    private final Option.BoolOption throughWalls  = new Option.BoolOption("Through Walls", true);
    private final Option.DoubleOption scale       = new Option.DoubleOption("Scale", 1.0, 0.5, 3.0, 0.1);
    private final Option.DoubleOption range       = new Option.DoubleOption("Range", 128.0, 16.0, 256.0, 8.0);

    @Override public String getName()     { return "Nametags"; }
    @Override public String getDescription() { return "Enhanced player nametags with health, ping, and items"; }
    @Override     public String getCategory() { return "Visual"; }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(showHealth, showDistance, showPing, showGamemode, showItems, showArmor, throughWalls, scale, range);
    }

    @Override
    public void onTick(Minecraft client) {
        // Rendering is handled by NametagsMixin.
    }

    public boolean showHealth()   { return showHealth.getValue(); }
    public boolean showDistance() { return showDistance.getValue(); }
    public boolean showPing()     { return showPing.getValue(); }
    public boolean showGamemode() { return showGamemode.getValue(); }
    public boolean showItems()    { return showItems.getValue(); }
    public boolean showArmor()    { return showArmor.getValue(); }
    public boolean throughWalls() { return throughWalls.getValue(); }
    public float scale()          { return scale.getValue().floatValue(); }
    public double range()         { return range.getValue(); }
}
