
package dev.nova.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public abstract class RenderPipelinesAccessor {
    @Accessor("RENDERTYPE_LINES_SNIPPET")
    public static native RenderPipeline.Snippet nova$getLinesSnippet();

    @Invoker("register")
    public static native RenderPipeline nova$register(
            RenderPipeline pipeline
    );
}
