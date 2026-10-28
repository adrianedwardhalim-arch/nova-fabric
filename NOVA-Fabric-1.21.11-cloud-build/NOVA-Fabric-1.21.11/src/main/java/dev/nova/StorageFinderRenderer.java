package dev.nova;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class StorageFinderRenderer {
    private StorageFinderRenderer() {
    }

    public static void register() {
        // Draw late in the world pass, alongside debug overlays.
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!StorageFinder.enabled) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null || context.matrices() == null) {
                return;
            }

            var camera = client.gameRenderer.getCamera();
            Vec3d cameraPos = camera.getCameraPos();
            BlockPos center = camera.getBlockPos();
            double maxDistanceSquared = StorageFinder.range == -1
                    ? Double.POSITIVE_INFINITY
                    : (double) StorageFinder.range * StorageFinder.range;

            var consumers = context.consumers();
            if (consumers == null) {
                return;
            }

            // Use the custom line layer with depth testing disabled so the outlines
            // are not hidden by solid blocks.
            var throughWallLines = consumers.getBuffer(StorageFinderRenderLayers.THROUGH_WALLS);

            for (var blockEntity : client.world.getBlockEntities()) {
                BlockPos pos = blockEntity.getPos();
                double dx = pos.getX() - center.getX();
                double dy = pos.getY() - center.getY();
                double dz = pos.getZ() - center.getZ();
                if (dx * dx + dy * dy + dz * dz > maxDistanceSquared) {
                    continue;
                }

                BlockState state = client.world.getBlockState(pos);
                if (!StorageFinder.isStorage(state)) {
                    continue;
                }

                var shape = state.getOutlineShape(client.world, pos);
                if (shape.isEmpty()) {
                    continue;
                }

                double x = pos.getX() - cameraPos.x;
                double y = pos.getY() - cameraPos.y;
                double z = pos.getZ() - cameraPos.z;

                VertexRendering.drawOutline(
                        context.matrices(), throughWallLines, shape, x, y, z,
                        StorageFinder.highlightColor, 2.0F
                );
            }

            // Custom render layers are not guaranteed to be flushed by the world renderer.
            // Submit this layer explicitly while the world-render context is active.
            if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw(StorageFinderRenderLayers.THROUGH_WALLS);
            }
        });
    }
}
