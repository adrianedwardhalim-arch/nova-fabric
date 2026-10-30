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
    private static final Matrix4f IDENTITY = new Matrix4f();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static MappableRingBuffer vertexBuffer;

    private StorageFinderRenderer() {
    }

    public static void register() {
        // Keep the proven build #21 outline renderer; add a separate no-depth pass
        // so the same block shapes are also visible through solid walls.
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(StorageFinderRenderer::render);
    }

    private static void render(WorldRenderContext context) {
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

        // Original build #21 renderer, preserved unchanged for the normal visible outline.
        var matrices = context.matrices();
        var lines = context.consumers().getBuffer(RenderLayers.LINES);
        BlockPos.Mutable pos = new BlockPos.Mutable();

        // A second buffer draws the same block outline edges with depth testing disabled.
        BufferBuilder throughWallBuilder = new BufferBuilder(
                ALLOCATOR,
                THROUGH_WALLS_LINES.getVertexFormatMode(),
                THROUGH_WALLS_LINES.getVertexFormat()
        );
        int matchedBlocks = 0;
        int color = StorageFinder.highlightColor;

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

                    double rx = pos.getX() - cameraPos.x;
                    double ry = pos.getY() - cameraPos.y;
                    double rz = pos.getZ() - cameraPos.z;

                    // Build #21 depth-tested outline, unchanged.
                    VertexRendering.drawOutline(
                            matrices, lines, shape,
                            rx, ry, rz,
                            color, 2.0F
                    );

                    // Match the exact VoxelShape edges, but submit them to a no-depth pipeline.
                    shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
                        float ax = (float) (rx + x1);
                        float ay = (float) (ry + y1);
                        float az = (float) (rz + z1);
                        float bx = (float) (rx + x2);
                        float by = (float) (ry + y2);
                        float bz = (float) (rz + z2);
                        float nx = Math.abs(x2 - x1) > 1.0E-7 ? 1.0F : 0.0F;
                        float ny = Math.abs(y2 - y1) > 1.0E-7 ? 1.0F : 0.0F;
                        float nz = Math.abs(z2 - z1) > 1.0E-7 ? 1.0F : 0.0F;

                        throughWallBuilder.vertex(IDENTITY, ax, ay, az).color(color).normal(nx, ny, nz).lineWidth(2.0F);
                        throughWallBuilder.vertex(IDENTITY, bx, by, bz).color(color).normal(nx, ny, nz).lineWidth(2.0F);
                    });
                    matchedBlocks++;
                }
            }
        }

        if (matchedBlocks == 0) {
            BuiltBuffer empty = throughWallBuilder.endNullable();
            if (empty != null) {
                empty.close();
            }
            ALLOCATOR.clear();
            return;
        }

        BuiltBuffer built = throughWallBuilder.end();
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
