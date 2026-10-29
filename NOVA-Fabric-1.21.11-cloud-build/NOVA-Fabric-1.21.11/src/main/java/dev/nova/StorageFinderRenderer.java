package dev.nova;


import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.BufferAllocator;\nimport java.util.OptionalDouble;\nimport java.util.OptionalInt;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class StorageFinderRenderer {
    private static final RenderPipeline THROUGH_WALLS_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of("nova", "pipeline/storage_finder_lines_through_walls"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );

    private static final BufferAllocator ALLOCATOR = new BufferAllocator(1 << 20);

    private StorageFinderRenderer() {}

    public static void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(StorageFinderRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!StorageFinder.enabled || client.world == null || client.player == null) return;

        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        double maxDistanceSquared = StorageFinder.range == -1
                ? Double.POSITIVE_INFINITY
                : (double) StorageFinder.range * StorageFinder.range;

        var matrices = context.matrices();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

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
            if (dx * dx + dy * dy + dz * dz > maxDistanceSquared) continue;
            if (!StorageFinder.isStorage(client.world.getBlockState(pos))) continue;
            appendBox(builder, positionMatrix, pos, color);
            count++;
        }
        matrices.pop();

        if (count == 0) {
            BuiltBuffer empty = builder.endNullable();
            if (empty != null) empty.close();
            ALLOCATOR.clear();
            return;
        }

        BuiltBuffer built = builder.end();
        draw(client, built);
        ALLOCATOR.clear();
    }

    private static void appendBox(BufferBuilder b, Matrix4f m, BlockPos p, int color) {
        float x=p.getX(), y=p.getY(), z=p.getZ(), X=x+1, Y=y+1, Z=z+1;
        line(b,m,x,y,z,X,y,z,color,0,0,1);
        line(b,m,x,y,Z,X,y,Z,color,0,0,1);
        line(b,m,x,Y,z,X,Y,z,color,0,0,1);
        line(b,m,x,Y,Z,X,Y,Z,color,0,0,1);
        line(b,m,x,y,z,x,Y,z,color,1,0,0);
        line(b,m,X,y,z,X,Y,z,color,1,0,0);
        line(b,m,x,y,Z,x,Y,Z,color,1,0,0);
        line(b,m,X,y,Z,X,Y,Z,color,1,0,0);
        line(b,m,x,y,z,x,y,Z,color,0,1,0);
        line(b,m,X,y,z,X,y,Z,color,0,1,0);
        line(b,m,x,Y,z,x,Y,Z,color,0,1,0);
        line(b,m,X,Y,z,X,Y,Z,color,0,1,0);
    }

    private static void line(BufferBuilder b, Matrix4f m,
                             float x0,float y0,float z0,float x1,float y1,float z1,
                             int color,float nx,float ny,float nz) {
        b.vertex(m,x0,y0,z0).color(color).normal(nx,ny,nz);
        b.vertex(m,x1,y1,z1).color(color).normal(nx,ny,nz);
    }

    private static void draw(MinecraftClient client, BuiltBuffer built) {
        try (built) {
            BuiltBuffer.DrawParameters parameters = built.getDrawParameters();
            VertexFormat format = parameters.format();
            int byteCount = parameters.vertexCount() * format.getVertexSize();

            // Upload vertices into a short-lived GPU buffer for this render pass.
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            com.mojang.blaze3d.buffers.GpuBuffer vertices = RenderSystem.getDevice().createBuffer(
                    () -> "NOVA StorageFinder vertices",
                    com.mojang.blaze3d.buffers.GpuBuffer.USAGE_VERTEX | com.mojang.blaze3d.buffers.GpuBuffer.USAGE_MAP_WRITE,
                    byteCount
            );
            try {
                java.nio.ByteBuffer source = built.getBuffer();
                try (var mapped = encoder.mapBuffer(vertices.slice(0, source.remaining()), false, true)) {
                    org.lwjgl.system.MemoryUtil.memCopy(source, mapped.data());
                }

                RenderSystem.AutoStorageIndexBuffer indices =
                        RenderSystem.getSequentialBuffer(THROUGH_WALLS_LINES.getVertexFormatMode());
                var dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                        RenderSystem.getModelViewMatrix(), new org.joml.Vector4f(1f, 1f, 1f, 1f),
                        new org.joml.Vector3f(), new Matrix4f()
                );
                var framebuffer = client.getFramebuffer();
                try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> "NOVA StorageFinder through-wall outlines",
                        framebuffer.getColorAttachmentView(), OptionalInt.empty(),
                        framebuffer.getDepthAttachmentView(), OptionalDouble.empty())) {
                    pass.setPipeline(THROUGH_WALLS_LINES);
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", dynamicTransforms);
                    pass.setVertexBuffer(0, vertices);
                    pass.setIndexBuffer(indices.getBuffer(parameters.indexCount()), indices.type());
                    pass.drawIndexed(0, 0, parameters.indexCount(), 1);
                }
            } finally {
                vertices.close();
            }
        }
    }
}
