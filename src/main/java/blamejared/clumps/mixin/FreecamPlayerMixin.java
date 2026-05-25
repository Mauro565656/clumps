package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.Freecam;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class FreecamPlayerMixin {

    @Shadow private boolean crouching;

    // Only block movement inputs — do NOT cancel aiStep so gravity/physics still run.
    @Inject(method = "applyInput", at = @At("HEAD"), cancellable = true)
    private void clumps$freezeInput(CallbackInfo ci) {
        if (isFreecamActive()) ci.cancel();
    }

    // Prevent the player model from visually sneaking while freecam is active.
    @Inject(method = "aiStep", at = @At("RETURN"))
    private void clumps$freezeSneak(CallbackInfo ci) {
        if (isFreecamActive()) {
            this.crouching = false;
        }
    }

    private static boolean isFreecamActive() {
        for (var m : ClumpsClient.modules) {
            if (m instanceof Freecam f && f.enabled) return true;
        }
        return false;
    }
}
