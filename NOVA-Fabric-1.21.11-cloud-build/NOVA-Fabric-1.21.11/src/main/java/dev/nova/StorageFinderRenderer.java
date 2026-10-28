
package dev.nova;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;

public final class StorageFinderRenderer {
    private StorageFinderRenderer() {
    }

    public static void register() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hitResult) -> {
            if (!StorageFinder.enabled) {
                return true;
            }

            if (!(hitResult instanceof BlockHitResult blockHit)) {
                return true;
            }

            var client = net.minecraft.client.MinecraftClient.getInstance();

            if (client.world == null) {
                return true;
            }

            BlockState state = client.world.getBlockState(blockHit.getBlockPos());

            if (!StorageFinder.isStorage(state)) {
                return true;
            }

            return true;
        });
    }
}

