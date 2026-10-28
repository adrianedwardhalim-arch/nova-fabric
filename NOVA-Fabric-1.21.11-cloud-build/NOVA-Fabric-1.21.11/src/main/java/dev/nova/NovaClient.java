
package dev.nova;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.state.OutlineRenderState;

public final class StorageFinderRenderer {
    private StorageFinderRenderer() {
    }

    public static void register() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(
                (context, outlineState) -> {
                    if (!StorageFinder.enabled) {
                        return true;
                    }

                    MinecraftClient client = MinecraftClient.getInstance();

                    if (client.world == null) {
                        return true;
                    }

                    BlockState state = client.world.getBlockState(
                            outlineState.pos()
                    );

                    if (!StorageFinder.isStorage(state)) {
                        return true;
                    }

                    return true;
                }
        );
    }
}
