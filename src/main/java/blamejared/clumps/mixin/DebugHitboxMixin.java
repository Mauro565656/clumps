package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.Hitbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Expands the AABB used when rendering F3+B debug hitboxes for living entities
 * (visual only — collision is unchanged).
 *
 * The actual hitbox cuboid is rendered in the private showHitboxes() method at
 * bytecode offset 35 (ordinal=0). emitGizmos() only has frustum-check and label
 * calls — NOT the render call — so the redirect must target showHitboxes.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class DebugHitboxMixin {

    @Redirect(
            method = "showHitboxes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;",
                    ordinal = 0
            )
    )
    private AABB clumps$expandDebugHitbox(Entity entity) {
        AABB box = entity.getBoundingBox();
        if (!(entity instanceof LivingEntity)) return box;
        if (entity == Minecraft.getInstance().player) return box;

        Hitbox hitbox = getEnabledHitbox();
        if (hitbox == null) return box;

        return box.inflate(hitbox.getExpansion());
    }

    private static Hitbox getEnabledHitbox() {
        for (var m : ClumpsClient.modules) {
            if (m instanceof Hitbox h && m.enabled) return h;
        }
        return null;
    }
}
