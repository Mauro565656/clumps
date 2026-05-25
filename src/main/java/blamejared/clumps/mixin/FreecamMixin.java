package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FreecamMixin {

    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void clumps$overrideCamera(float partialTicks, CallbackInfo ci) {
        for (var m : ClumpsClient.modules) {
            if (m instanceof Freecam fc && m.enabled && fc.isInitialized()) {
                this.detached = true;
                setPosition(new Vec3(
                    fc.getX(partialTicks),
                    fc.getY(partialTicks),
                    fc.getZ(partialTicks)
                ));
                setRotation(fc.getYaw(partialTicks), fc.getPitch(partialTicks));
                return;
            }
        }
    }
}
