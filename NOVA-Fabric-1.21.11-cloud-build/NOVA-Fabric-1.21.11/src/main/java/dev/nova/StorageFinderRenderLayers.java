package dev.nova;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

public final class StorageFinderRenderLayers {
    private static final RenderPipeline THROUGH_WALLS_PIPELINE =
            RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                            .withLocation(Identifier.of(
                                    "nova", "pipeline/storage_finder_lines_through_walls"))
                            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                            .withDepthWrite(false)
                            .build()
            );

    public static final RenderLayer THROUGH_WALLS = RenderLayer.of(
            "nova_storage_finder_through_walls",
            RenderSetup.builder(THROUGH_WALLS_PIPELINE)
                    .expectedBufferSize(1536)
                    .build()
    );

    private StorageFinderRenderLayers() {
    }
}
