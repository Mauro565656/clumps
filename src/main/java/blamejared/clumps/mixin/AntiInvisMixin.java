package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.AntiInvis;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class AntiInvisMixin {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void clumps$antiInvis(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object) this;
        for (var m : ClumpsClient.modules) {
            if (m instanceof AntiInvis ai && m.enabled) {
                if (ai.isRevealed(self)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }
}
