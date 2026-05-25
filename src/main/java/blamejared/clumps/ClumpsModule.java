package blamejared.clumps;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public abstract class ClumpsModule {

    public boolean enabled = false;

    public KeyMapping keybind;

    public String getId() {
        return getName().toLowerCase().replace(" ", "_");
    }

    public abstract String getName();

    public String getDescription() {
        return "No description";
    }

    public String getCategory() {
        return "Misc";
    }

    public void onTick(Minecraft client) {}

    public java.util.List<Option<?>> getOptions() {
        return java.util.Collections.emptyList();
    }
}
