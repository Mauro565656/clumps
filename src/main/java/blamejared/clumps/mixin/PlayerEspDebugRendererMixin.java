package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.FriendData;
import blamejared.clumps.modules.Friends;
import blamejared.clumps.modules.PlayerESP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugRenderer.class)
public class PlayerEspDebugRendererMixin {
    private static final int FRIEND_COLOR = 0xFF55FF55;
    private static final int OTHER_COLOR = 0xFFFF5555;

    @Unique
    private static boolean clumps$tracerRendererAdded = false;

    @Inject(method = "refreshRendererList", at = @At("RETURN"))
    private void clumps$addTracerRenderer(CallbackInfo ci) {
        if (clumps$tracerRendererAdded) return;
        clumps$tracerRendererAdded = true;

        DebugRenderer self = (DebugRenderer) (Object) this;
        List<DebugRenderer.SimpleDebugRenderer> renderers = ((DebugRendererAccessor) self).clumps$getRenderers();
        renderers.add((camX, camY, camZ, debug, frustum, partialTick) -> {
            PlayerESP esp = null;
            Friends friends = null;
            for (var module : ClumpsClient.modules) {
                if (module instanceof PlayerESP p && module.enabled) esp = p;
                if (module instanceof Friends f) friends = f;
            }
            if (esp == null || !esp.tracers()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            Vec3 start = mc.player.getEyePosition(partialTick);
            float width = esp.tracerWidth();
            int range = esp.tracerRange();

            for (Player player : mc.level.players()) {
                if (player == mc.player || !player.isAlive()) continue;
                if (mc.player.distanceTo(player) > range) continue;

                int color = OTHER_COLOR;
                if (friends != null) {
                    FriendData fd = friends.getFriendData(player.getGameProfile().name());
                    if (fd != null) {
                        if (!fd.showTracers()) continue;
                        color = FRIEND_COLOR;
                    }
                }

                Vec3 end = player.getEyePosition(partialTick);
                Gizmos.line(start, end, color, width).setAlwaysOnTop();
            }
        });
    }
}
