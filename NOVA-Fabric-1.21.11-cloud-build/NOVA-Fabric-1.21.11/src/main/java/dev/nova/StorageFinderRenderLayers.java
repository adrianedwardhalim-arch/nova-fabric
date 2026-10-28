package dev.nova;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.vertex.VertexFormat;

public final class StorageFinderRenderLayers {
    private static final RenderPipeline THROUGH_WALLS_PIPELINE =
            RenderPipelines.register(
                    RenderPipeline.builder()
                            .withLocation(Identifier.of(
                                    "nova", "pipeline/storage_finder_lines_through_walls"))
                            .withVertexShader("core/rendertype_lines")
                            .withFragmentShader("core/rendertype_lines")
                            .withVertexFormat(
                                    VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH,
                                    VertexFormat.DrawMode.LINES)
                            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                            .withDepthWrite(false)
                            .withBlend(BlendFunction.TRANSLUCENT)
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
