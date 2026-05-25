package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.ClickCrystals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class ClickCrystalsMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void clumps$clientCrystalRemove(Player player, Entity target, CallbackInfo ci) {
        if (!(target instanceof EndCrystal crystal)) return;
        Minecraft mc = Minecraft.getInstance();
        for (var m : ClumpsClient.modules) {
            if (m instanceof ClickCrystals cc && cc.shouldRemove(mc, crystal)) {
                crystal.remove(Entity.RemovalReason.KILLED);
                crystal.onClientRemoval();
                return;
            }
        }
    }
}
