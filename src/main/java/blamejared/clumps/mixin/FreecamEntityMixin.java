package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class FreecamEntityMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void clumps$redirectTurn(double xo, double yo, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (Object) this != mc.player) return;

        for (var m : ClumpsClient.modules) {
            if (m instanceof Freecam fc && m.enabled && fc.isInitialized()) {
                fc.changeLookDirection(xo * 0.15, yo * 0.15);
                ci.cancel();
                return;
            }
        }
    }
}
