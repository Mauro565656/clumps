package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Improves item tooltips with additional info: food stats, armor values,
 * enchantment max levels, and attribute modifiers.
 * Rendering is handled by BetterTooltipsMixin.
 */
public class BetterTooltips extends ClumpsModule {

    private final Option.BoolOption showFood       = new Option.BoolOption("Food Stats", true);
    private final Option.BoolOption showArmor      = new Option.BoolOption("Armor Values", true);
    private final Option.BoolOption showAttributes = new Option.BoolOption("Attributes", true);
    private final Option.BoolOption showDurability = new Option.BoolOption("Durability", true);
    private final Option.BoolOption showEnchantMax = new Option.BoolOption("Enchant Max Level", true);
    private final Option.BoolOption showDataSize   = new Option.BoolOption("Data Size", true);

    @Override public String getName()     { return "Better Tooltips"; }
    @Override public String getDescription() { return "Enhanced item tooltips"; }
    @Override public String getCategory() { return "Misc"; }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(showFood, showArmor, showAttributes, showDurability, showEnchantMax, showDataSize);
    }

    @Override
    public void onTick(Minecraft client) {
        // Tooltip injection is handled by BetterTooltipsMixin.
    }

    public boolean showFood()       { return showFood.getValue(); }
    public boolean showArmor()      { return showArmor.getValue(); }
    public boolean showAttributes() { return showAttributes.getValue(); }
    public boolean showDurability() { return showDurability.getValue(); }
    public boolean showEnchantMax() { return showEnchantMax.getValue(); }
    public boolean showDataSize()   { return showDataSize.getValue(); }
}
