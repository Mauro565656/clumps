package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.BlockESP;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Registers a custom SimpleDebugRenderer that draws Block ESP outlines through walls.
 * setAlwaysOnTop() disables depth testing so blocks show through terrain.
 */
@Mixin(DebugRenderer.class)
public class DebugRendererInitMixin {

    private static final GizmoStyle ESP_STYLE = GizmoStyle.stroke(0xFFFF2020, 2.0f);

    @Inject(method = "refreshRendererList", at = @At("RETURN"))
    private void clumps$addBlockESPRenderer(CallbackInfo ci) {
        DebugRenderer self = (DebugRenderer) (Object) this;
        List<DebugRenderer.SimpleDebugRenderer> renderers = ((DebugRendererAccessor) self).clumps$getRenderers();
        renderers.add((camX, camY, camZ, debug, frustum, partialTick) -> {
            BlockESP esp = null;
            for (var m : ClumpsClient.modules) {
                if (m instanceof BlockESP b && b.enabled) {
                    esp = b;
                    break;
                }
            }
            if (esp == null || esp.highlighted.isEmpty()) return;
            for (var pos : esp.highlighted) {
                Gizmos.cuboid(pos, ESP_STYLE).setAlwaysOnTop();
            }
        });
    }
}
