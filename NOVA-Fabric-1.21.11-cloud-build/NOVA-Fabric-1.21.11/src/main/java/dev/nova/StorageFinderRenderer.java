package dev.nova;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class StorageFinderRenderer {
    private StorageFinderRenderer() {
    }

    public static void register() {
        WorldRenderEvents.LAST.register(context -> {
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
            double maxDistanceSquared = StorageFinder.range == -1
                    ? Double.POSITIVE_INFINITY
                    : (double) StorageFinder.range * StorageFinder.range;

            var matrices = context.matrices();
            var consumers = context.consumers();
            if (consumers == null) {
                return;
            }

            var lines = consumers.getBuffer(StorageFinderRenderLayers.THROUGH_WALLS);
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

            consumers.draw(StorageFinderRenderLayers.THROUGH_WALLS);
        });
    }
}
