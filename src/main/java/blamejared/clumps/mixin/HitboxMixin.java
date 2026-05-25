package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.Hitbox;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class HitboxMixin {

    @Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true)
    private void clumps$expandPickRadius(CallbackInfoReturnable<Float> cir) {
        Hitbox hitbox = getEnabledHitbox();
        Entity self = (Entity) (Object) this;
        if (hitbox != null && self instanceof LivingEntity && self != Minecraft.getInstance().player) {
            cir.setReturnValue(cir.getReturnValue() + hitbox.getExpansion());
        }
    }

    private Hitbox getEnabledHitbox() {
        for (var m : ClumpsClient.modules) {
            if (m instanceof Hitbox hitbox && m.enabled) {
                return hitbox;
            }
        }
        return null;
    }
}
