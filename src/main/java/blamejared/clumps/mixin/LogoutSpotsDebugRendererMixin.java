package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.LogoutSpots;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugRenderer.class)
public class LogoutSpotsDebugRendererMixin {
    private static final int BOX_COLOR = 0xCCFF00FF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    @Inject(method = "refreshRendererList", at = @At("RETURN"))
    private void clumps$addLogoutSpotsRenderer(CallbackInfo ci) {
        DebugRenderer self = (DebugRenderer) (Object) this;
        List<DebugRenderer.SimpleDebugRenderer> renderers = ((DebugRendererAccessor) self).clumps$getRenderers();
        renderers.add((camX, camY, camZ, debug, frustum, partialTick) -> {
            LogoutSpots module = null;
            for (var m : ClumpsClient.modules) {
                if (m instanceof LogoutSpots logoutSpots && m.enabled) {
                    module = logoutSpots;
                    break;
                }
            }
            if (module == null) return;

            for (LogoutSpots.Entry entry : module.getEntries()) {
                if (module.showBox()) {
                    Gizmos.cuboid(entry.box(), GizmoStyle.stroke(BOX_COLOR, 2.0f)).setAlwaysOnTop();
                }
                Vec3 pos = entry.pos().add(0.0, entry.box().getYsize() + 0.4, 0.0);
                Gizmos.billboardText(entry.name() + " " + entry.health() + "hp", pos, net.minecraft.gizmos.TextGizmo.Style.forColor(TEXT_COLOR).withScale(module.scale())).setAlwaysOnTop();
            }
        });
    }
}
