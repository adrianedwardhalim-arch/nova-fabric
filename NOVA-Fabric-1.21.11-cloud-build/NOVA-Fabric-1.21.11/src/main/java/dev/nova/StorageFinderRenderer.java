package dev.nova;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

public final class StorageFinderRenderer {
    private StorageFinderRenderer() {
    }

    public static void register() {
        // Minecraft 1.21.11 collects these during the client tick and draws
        // them in the world gizmo pass. Rebuild them every tick so the list
        // always reflects currently loaded storage and the current settings.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!StorageFinder.enabled || client.world == null || client.player == null) {
                return;
            }

            BlockPos center = client.gameRenderer.getCamera().getBlockPos();
            double maxDistanceSquared = StorageFinder.range == -1
                    ? Double.POSITIVE_INFINITY
                    : (double) StorageFinder.range * StorageFinder.range;

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

                    // Do not set a lifespan: the per-tick collector already
                    // redraws these each tick. Persisting them created stale,
                    // overlapping outlines after moving or changing settings.
                    GizmoDrawing.box(
                            pos,
                            DrawStyle.stroked(StorageFinder.highlightColor, 2.0F)
                    ).ignoreOcclusion();
                }
            }
        });
    }
}
