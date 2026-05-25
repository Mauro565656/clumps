package blamejared.clumps.mixin;

import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(DebugRenderer.class)
public interface DebugRendererAccessor {
    @Accessor("renderers")
    List<DebugRenderer.SimpleDebugRenderer> clumps$getRenderers();
}
