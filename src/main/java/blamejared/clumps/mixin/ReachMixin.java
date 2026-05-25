package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.Reach;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class ReachMixin {

    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void clumps$extendEntityReach(CallbackInfoReturnable<Double> cir) {
        Player self = (Player)(Object) this;
        if (self == Minecraft.getInstance().player) {
            for (var m : ClumpsClient.modules) {
                if (m instanceof Reach reach && m.enabled) {
                    cir.setReturnValue(reach.getReachDistance());
                    return;
                }
            }
        }
    }

    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void clumps$extendBlockReach(CallbackInfoReturnable<Double> cir) {
        Player self = (Player)(Object) this;
        if (self == Minecraft.getInstance().player) {
            for (var m : ClumpsClient.modules) {
                if (m instanceof Reach reach && m.enabled) {
                    cir.setReturnValue(reach.getReachDistance());
                    return;
                }
            }
        }
    }
}
