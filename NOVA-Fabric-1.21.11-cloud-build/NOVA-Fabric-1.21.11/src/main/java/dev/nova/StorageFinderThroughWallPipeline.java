
package dev.nova;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import dev.nova.mixin.RenderPipelinesAccessor;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

public final class StorageFinderThroughWallPipeline {
    public static final RenderPipeline LINES_THROUGH_WALLS =
            RenderPipelinesAccessor.nova$register(
                    RenderPipeline.builder(
                            RenderPipelinesAccessor.nova$getLinesSnippet()
                    )
                    .withLocation(Identifier.of(
                            "nova",
                            "pipeline/storage_finder_through_walls"
                    ))
                    .withDepthTestFunction(
                            DepthTestFunction.NO_DEPTH_TEST
                    )
                    .build()
            );

    private StorageFinderThroughWallPipeline() {
    }
}
