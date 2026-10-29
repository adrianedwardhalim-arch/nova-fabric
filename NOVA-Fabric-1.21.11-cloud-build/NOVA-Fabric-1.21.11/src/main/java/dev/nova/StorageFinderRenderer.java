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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
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
        // 1.21.11 splits world rendering into extraction and drawing phases.
        // Draw directly into the main framebuffer with a no-depth-test pipeline;
        // this avoids the debug-gizmo collector and buffered layer paths.
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(StorageFinderRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!StorageFinder.enabled || client.world == null || client.player == null) {
            return;
        }

        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        BlockPos center = BlockPos.ofFloored(camera.x, camera.y, camera.z);
        double maxDistanceSquared = StorageFinder.range == -1
                ? Double.POSITIVE_INFINITY
                : (double) StorageFinder.range * StorageFinder.range;

        BufferBuilder builder = new BufferBuilder(
                ALLOCATOR,
                THROUGH_WALLS_LINES.getVertexFormatMode(),
                THROUGH_WALLS_LINES.getVertexFormat()
        );
        int count = 0;
        int color = StorageFinder.highlightColor;

        for (var blockEntity : client.world.getBlockEntities()) {
            BlockPos pos = blockEntity.getPos();
            double dx = pos.getX() + 0.5 - camera.x;
            double dy = pos.getY() + 0.5 - camera.y;
            double dz = pos.getZ() + 0.5 - camera.z;
            if (dx * dx + dy * dy + dz * dz > maxDistanceSquared) {
                continue;
            }
            if (!StorageFinder.isStorage(client.world.getBlockState(pos))) {
                continue;
            }

            appendBox(builder, new Box(pos), camera, color);
            count++;
        }

        if (count == 0) {
            builder.end().close();
            ALLOCATOR.clear();
            return;
        }

        BuiltBuffer built = builder.end();
        draw(client, built);
        ALLOCATOR.clear();
    }

    private static void appendBox(BufferBuilder builder, Box box, Vec3d camera, int color) {
        float x0 = (float) (box.minX - camera.x);
        float y0 = (float) (box.minY - camera.y);
        float z0 = (float) (box.minZ - camera.z);
        float x1 = (float) (box.maxX - camera.x);
        float y1 = (float) (box.maxY - camera.y);
        float z1 = (float) (box.maxZ - camera.z);

        // Twelve edges. Each line is submitted as a pair of vertices.
        line(builder, x0, y0, z0, x1, y0, z0, color, 0, 0, 1);
        line(builder, x0, y0, z1, x1, y0, z1, color, 0, 0, 1);
        line(builder, x0, y1, z0, x1, y1, z0, color, 0, 0, 1);
        line(builder, x0, y1, z1, x1, y1, z1, color, 0, 0, 1);

        line(builder, x0, y0, z0, x0, y1, z0, color, 1, 0, 0);
        line(builder, x1, y0, z0, x1, y1, z0, color, 1, 0, 0);
        line(builder, x0, y0, z1, x0, y1, z1, color, 1, 0, 0);
        line(builder, x1, y0, z1, x1, y1, z1, color, 1, 0, 0);

        line(builder, x0, y0, z0, x0, y0, z1, color, 0, 1, 0);
        line(builder, x1, y0, z0, x1, y0, z1, color, 0, 1, 0);
        line(builder, x0, y1, z0, x0, y1, z1, color, 0, 1, 0);
        line(builder, x1, y1, z0, x1, y1, z1, color, 0, 1, 0);
    }

    private static void line(BufferBuilder builder, float x0, float y0, float z0,
                             float x1, float y1, float z1, int color,
                             float nx, float ny, float nz) {
        builder.vertex(x0, y0, z0).color(color).normal(nx, ny, nz);
        builder.vertex(x1, y1, z1).color(color).normal(nx, ny, nz);
    }

    private static void draw(MinecraftClient client, BuiltBuffer built) {
        try (built) {
            BuiltBuffer.DrawParameters parameters = built.getDrawParameters();
            VertexFormat format = parameters.format();
            int byteCount = parameters.vertexCount() * format.getVertexSize();

            if (vertexBuffer == null || vertexBuffer.size() < byteCount) {
                if (vertexBuffer != null) {
                    vertexBuffer.close();
                }
                vertexBuffer = new MappableRingBuffer(
                        () -> "NOVA StorageFinder vertex buffer",
                        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                        byteCount
                );
            }

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            ByteBuffer source = built.getBuffer();
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
