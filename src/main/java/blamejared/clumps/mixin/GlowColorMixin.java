package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.FriendData;
import blamejared.clumps.modules.Friends;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes friends appear with a bright green glow outline.
 * getTeamColor() is what the outline renderer reads when drawing the glow effect.
 */
@Mixin(Entity.class)
public class GlowColorMixin {

    private static final int GREEN = 0x55FF55;

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void clumps$friendGreenGlow(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        for (var m : ClumpsClient.modules) {
            if (m instanceof Friends friends) {
                FriendData fd = friends.getFriendData(player.getGameProfile().name());
                if (fd != null && fd.showEsp()) {
                    cir.setReturnValue(GREEN);
                    return;
                }
            }
        }
    }
}
