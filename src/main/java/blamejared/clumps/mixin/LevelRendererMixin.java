package blamejared.clumps.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Placeholder — no injections needed here in MC 26.1.
 * Block ESP is rendered via DebugHitboxMixin (piggybacks on the gizmo context).
 * Debug hitbox expansion is also handled by DebugHitboxMixin.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
}
