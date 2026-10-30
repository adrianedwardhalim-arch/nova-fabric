package dev.nova;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public final class StorageFinderRenderer {
    private static final RenderPipeline THROUGH_WALLS_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of("nova", "pipeline/storage_finder_lines_through_walls"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );

    private static final BufferAllocator ALLOCATOR = new BufferAllocator(1 << 20);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static MappableRingBuffer vertexBuffer;

    private StorageFinderRenderer() {
    }

    public static void register() {
        // The proven build #21 renderer remains its own, unchanged depth-tested pass.
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(StorageFinderRenderer::renderNormalOutlines);

        // Fabric's 1.21.11 custom-pipeline example performs extraction and the
        // no-depth draw at BEFORE_TRANSLUCENT, not BEFORE_DEBUG_RENDER.
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(StorageFinderRenderer::renderThroughWalls);
    }

    /**
     * Original build #21 outline path. Keep this depth-tested pass separate
     * from the custom no-depth pass below.
     */
    private static void renderNormalOutlines(WorldRenderContext context) {
        if (!StorageFinder.enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        var camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        BlockPos center = camera.getBlockPos();
        int horizontalRadius = StorageFinder.range == -1 ? 64 : StorageFinder.range;
        int verticalRadius = StorageFinder.range == -1 ? 32 : StorageFinder.range;

        var matrices = context.matrices();
        var lines = context.consumers().getBuffer(RenderLayers.LINES);
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = client.world.getBlockState(pos);

                    if (!StorageFinder.isStorage(state)) {
                        continue;
                    }

                    var shape = state.getOutlineShape(client.world, pos);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    VertexRendering.drawOutline(
                            matrices,
                            lines,
                            shape,
                            pos.getX() - cameraPos.x,
                            pos.getY() - cameraPos.y,
                            pos.getZ() - cameraPos.z,
                            StorageFinder.highlightColor,
                            2.0F
                    );
                }
            }
        }
    }

    /**
     * Fabric 1.21.11 custom-pipeline pattern: extract world-space geometry
     * with the camera-translated pose stack, then draw it with a no-depth pipeline.
     */
    private static void renderThroughWalls(WorldRenderContext context) {
        if (!StorageFinder.enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        var camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        BlockPos center = camera.getBlockPos();
        int horizontalRadius = StorageFinder.range == -1 ? 64 : StorageFinder.range;
        int verticalRadius = StorageFinder.range == -1 ? 32 : StorageFinder.range;
        int color = StorageFinder.highlightColor;

        var matrices = context.matrices();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        BufferBuilder builder = new BufferBuilder(
                ALLOCATOR,
                THROUGH_WALLS_LINES.getVertexFormatMode(),
                THROUGH_WALLS_LINES.getVertexFormat()
        );

        int matchedBlocks = 0;
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        try {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int y = -verticalRadius; y <= verticalRadius; y++) {
                    for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                        pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                        BlockState state = client.world.getBlockState(pos);

                        if (!StorageFinder.isStorage(state)) {
                            continue;
                        }

                        var shape = state.getOutlineShape(client.world, pos);
                        if (shape.isEmpty()) {
                            continue;
                        }

                        final double worldX = pos.getX();
                        final double worldY = pos.getY();
                        final double worldZ = pos.getZ();

                        shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
                            float dx = (float) (x2 - x1);
                            float dy = (float) (y2 - y1);
                            float dz = (float) (z2 - z1);

                            // rendertype_lines expands each line from a pair of vertices
                            // sharing the start position, using Normal as the line direction.
                            builder.vertex(positionMatrix,
                                            (float) (worldX + x1),
                                            (float) (worldY + y1),
                                            (float) (worldZ + z1))
                                    .color(color)
                                    .normal(dx, dy, dz)
                                    .lineWidth(2.0F);
                            builder.vertex(positionMatrix,
                                            (float) (worldX + x1),
                                            (float) (worldY + y1),
                                            (float) (worldZ + z1))
                                    .color(color)
                                    .normal(dx, dy, dz)
                                    .lineWidth(2.0F);
                        });
                        matchedBlocks++;
                    }
                }
            }
        } finally {
            matrices.pop();
        }

        if (matchedBlocks == 0) {
            BuiltBuffer empty = builder.endNullable();
            if (empty != null) {
                empty.close();
            }
            ALLOCATOR.clear();
            return;
        }

        BuiltBuffer built = builder.end();
        try {
            drawThroughWalls(client, built);
        } finally {
            ALLOCATOR.clear();
        }
    }

    private static void drawThroughWalls(MinecraftClient client, BuiltBuffer built) {
        try (built) {
            BuiltBuffer.DrawParameters parameters = built.getDrawParameters();
            VertexFormat format = parameters.format();
            ByteBuffer source = built.getBuffer();
            int byteCount = parameters.vertexCount() * format.getVertexSize();

            if (vertexBuffer == null || vertexBuffer.size() < byteCount) {
                if (vertexBuffer != null) {
                    vertexBuffer.close();
                }
                vertexBuffer = new MappableRingBuffer(
                        () -> "NOVA StorageFinder through-wall vertex buffer",
                        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                        byteCount
                );
            }

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            try (GpuBuffer.MappedView mapped = encoder.mapBuffer(
                    vertexBuffer.getBlocking().slice(0, source.remaining()), false, true)) {
                MemoryUtil.memCopy(source, mapped.data());
            }

            RenderSystem.ShapeIndexBuffer indices =
                    RenderSystem.getSequentialBuffer(THROUGH_WALLS_LINES.getVertexFormatMode());
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().write(
                    RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX
            );

            var framebuffer = client.getFramebuffer();
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "NOVA StorageFinder through-wall outlines",
                    framebuffer.getColorAttachmentView(), OptionalInt.empty(),
                    framebuffer.getDepthAttachmentView(), OptionalDouble.empty())) {
                pass.setPipeline(THROUGH_WALLS_LINES);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setVertexBuffer(0, vertexBuffer.getBlocking());
                pass.setIndexBuffer(indices.getIndexBuffer(parameters.indexCount()), indices.getIndexType());
                pass.drawIndexed(0, 0, parameters.indexCount(), 1);
            }

            vertexBuffer.rotate();
        }
    }
}
