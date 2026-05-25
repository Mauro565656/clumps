package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.List;

public class ClickCrystals extends ClumpsModule {

    @Override
    public String getName() { return "Click Crystals"; }
    @Override
    public String getDescription() { return "Remove end crystals clientside on attack"; }
    @Override
    public String getCategory() { return "Combat"; }

    @Override
    public List<blamejared.clumps.Option<?>> getOptions() { return List.of(); }

    @Override
    public void onTick(Minecraft client) {
        // Logic is handled by ClickCrystalsMixin — this module just provides the enabled flag.
    }

    public boolean shouldRemove(Minecraft client, EndCrystal crystal) {
        return enabled && client.player != null && crystal != null;
    }

    public void removeClientside(EndCrystal crystal) {
        crystal.remove(Entity.RemovalReason.KILLED);
        crystal.onClientRemoval();
    }
}
