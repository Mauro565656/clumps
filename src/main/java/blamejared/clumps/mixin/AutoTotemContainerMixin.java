package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.AutoTotem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class AutoTotemContainerMixin {
    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void clumps$lockAutoTotemScreen(int containerId, int slotNum, int buttonNum, ContainerInput clickType, Player player, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        for (var module : ClumpsClient.modules) {
            if (module instanceof AutoTotem autoTotem && autoTotem.shouldLockContainerClicks(mc)) {
                ci.cancel();
                return;
            }
        }
    }
}
