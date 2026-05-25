package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.Trajectories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugRenderer.class)
public class TrajectoriesMixin {

    private static final int LINE_COLOR   = 0xFFFFA500; // orange
    private static final int HIT_BLOCK    = 0xFFFF4444; // red
    private static final int HIT_ENTITY   = 0xFF44FF44; // green

    @Inject(method = "refreshRendererList", at = @At("RETURN"))
    private void clumps$addTrajectoriesRenderer(CallbackInfo ci) {
        DebugRenderer self = (DebugRenderer)(Object) this;
        List<DebugRenderer.SimpleDebugRenderer> renderers = ((DebugRendererAccessor) self).clumps$getRenderers();
        renderers.add((camX, camY, camZ, debug, frustum, partialTick) -> {
            Trajectories traj = null;
            for (var m : ClumpsClient.modules) {
                if (m instanceof Trajectories t && m.enabled) { traj = t; break; }
            }
            if (traj == null) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            var stack = mc.player.getMainHandItem();
            if (stack.isEmpty() || !Trajectories.isProjectileItem(stack)) {
                stack = mc.player.getOffhandItem();
                if (stack.isEmpty() || !Trajectories.isProjectileItem(stack)) return;
            }

            Trajectories.TrajectoryResult result = traj.simulate(mc, mc.player, stack);
            List<Vec3> points = result.points();
            if (points.size() < 2) return;

            // Draw trajectory line
            for (int i = 1; i < points.size(); i++) {
                Vec3 a = points.get(i - 1);
                Vec3 b = points.get(i);
                Gizmos.line(a, b, LINE_COLOR, 2.0f).setAlwaysOnTop();
            }

            // Draw hit marker
            if (traj.showHit() && result.hit() != null) {
                if (result.hit() instanceof BlockHitResult blockHit) {
                    Gizmos.cuboid(blockHit.getBlockPos(), GizmoStyle.stroke(HIT_BLOCK, 2.0f)).setAlwaysOnTop();
                } else if (result.hit() instanceof EntityHitResult entityHit) {
                    var entity = entityHit.getEntity();
                    Gizmos.cuboid(entity.getBoundingBox(), GizmoStyle.stroke(HIT_ENTITY, 2.0f)).setAlwaysOnTop();
                    // Billboard text if it's a living entity (shows health)
                    if (entity instanceof LivingEntity living) {
                        Gizmos.billboardTextOverMob(entity, 0,
                                String.format("%.1fhp", living.getHealth()), HIT_ENTITY, 1.0f);
                    }
                }

                // Dot at impact point
                Gizmos.point(points.get(points.size() - 1), HIT_BLOCK, 4.0f).setAlwaysOnTop();
            }
        });
    }
}
