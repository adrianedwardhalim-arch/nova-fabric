
package dev.nova;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class StorageFinderRenderer {
    private static final int HORIZONTAL_RADIUS = 6;
    private static final int VERTICAL_RADIUS = 4;

    private StorageFinderRenderer() {
    }

    public static void register() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
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

            var matrices = context.matrices();
            var lines = context.consumers()
                    .getBuffer(RenderLayers.LINES);

            BlockPos.Mutable pos = new BlockPos.Mutable();

            for (int x = -HORIZONTAL_RADIUS;
                 x <= HORIZONTAL_RADIUS; x++) {
                for (int y = -VERTICAL_RADIUS;
                     y <= VERTICAL_RADIUS; y++) {
                    for (int z = -HORIZONTAL_RADIUS;
                         z <= HORIZONTAL_RADIUS; z++) {

                        pos.set(
                                center.getX() + x,
                                center.getY() + y,
                                center.getZ() + z
                        );

                        BlockState state =
                                client.world.getBlockState(pos);

                        if (!StorageFinder.isStorage(state)) {
                            continue;
                        }

                        var shape = state.getOutlineShape(
                                client.world, pos
                        );

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
        });
    }
}
