package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AntiInvis extends ClumpsModule {

    @Override
    public String getName() { return "Anti Invis"; }
    @Override
    public String getDescription() { return "Reveal invisible players"; }
    @Override
    public String getCategory() { return "Visual"; }

    public boolean isRevealed(Entity entity) {
        return enabled && entity instanceof Player && entity.isInvisible();
    }

    public void onDisable(Minecraft client) {}
}
