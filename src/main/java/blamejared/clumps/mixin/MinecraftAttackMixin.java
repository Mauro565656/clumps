package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.Criticals;
import blamejared.clumps.modules.FriendData;
import blamejared.clumps.modules.Friends;
import blamejared.clumps.modules.MaceSwap;
import blamejared.clumps.modules.SpearKiller;
import blamejared.clumps.modules.SpearLauncher;
import blamejared.clumps.modules.StunSlam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftAttackMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void clumps$prepareAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = (Minecraft) (Object) this;

        // Block attacking friends (per-friend setting)
        Entity target = client.crosshairPickEntity;
        if (target instanceof Player targetPlayer) {
            for (var module : ClumpsClient.modules) {
                if (module instanceof Friends friends) {
                    FriendData fd = friends.getFriendData(targetPlayer.getGameProfile().name());
                    if (fd != null && !fd.isAttackable()) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }

        for (var module : ClumpsClient.modules) {
            if (module instanceof SpearKiller spearKiller && module.enabled) {
                spearKiller.onStartAttack(client);
            }
            if (module instanceof SpearLauncher spearLauncher && module.enabled) {
                spearLauncher.onStartAttack(client);
            }
            if (module instanceof Criticals criticals && module.enabled) {
                criticals.onStartAttack(client);
            }
            if (module instanceof MaceSwap maceSwap && module.enabled) {
                maceSwap.onStartAttack(client);
            }
            if (module instanceof StunSlam stunSlam && module.enabled) {
                stunSlam.onStartAttack(client);
            }
        }
    }
}
