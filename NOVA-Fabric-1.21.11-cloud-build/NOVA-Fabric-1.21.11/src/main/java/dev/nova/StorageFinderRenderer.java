package dev.nova;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

public final class StorageFinderRenderer {
    private StorageFinderRenderer() {
    }

    public static void register() {
        // Minecraft 1.21.11 has a dedicated gizmo renderer. Use it instead of
        // a custom RenderLayer whose buffers may never be submitted by vanilla.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!StorageFinder.enabled || client.world == null || client.player == null) {
                return;
            }

            BlockPos center = client.gameRenderer.getCamera().getBlockPos();
            double maxDistanceSquared = StorageFinder.range == -1
                    ? Double.POSITIVE_INFINITY
                    : (double) StorageFinder.range * StorageFinder.range;

            // Register short-lived gizmos each tick so moved/removed storage is
            // updated promptly. ignoreOcclusion() explicitly draws through blocks.
            try (var scope = client.newGizmoScope()) {
                for (var blockEntity : client.world.getBlockEntities()) {
                    BlockPos pos = blockEntity.getPos();
                    double dx = pos.getX() - center.getX();
                    double dy = pos.getY() - center.getY();
                    double dz = pos.getZ() - center.getZ();
                    if (dx * dx + dy * dy + dz * dz > maxDistanceSquared) {
                        continue;
                    }

                    if (!StorageFinder.isStorage(client.world.getBlockState(pos))) {
                        continue;
                    }

                    GizmoDrawing.box(
                            new Box(pos),
                            DrawStyle.stroked(StorageFinder.highlightColor, 2.0F)
                    ).ignoreOcclusion().withLifespan(100);
                }
            }
        });
    }
}
