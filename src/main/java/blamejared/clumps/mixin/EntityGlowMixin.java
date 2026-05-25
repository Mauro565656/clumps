package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.FriendData;
import blamejared.clumps.modules.Friends;
import blamejared.clumps.modules.PlayerESP;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class EntityGlowMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void clumps$forceGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player player)) return;
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player != null && entity == mc.player) return;

        Friends friends = null;
        boolean espEnabled = false;
        for (var m : ClumpsClient.modules) {
            if (m instanceof Friends f) friends = f;
            if (m instanceof PlayerESP && m.enabled) espEnabled = true;
        }

        if (friends != null) {
            FriendData fd = friends.getFriendData(player.getGameProfile().name());
            if (fd != null) {
                if (fd.showEsp()) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        if (espEnabled) {
            cir.setReturnValue(true);
        }
    }
}
