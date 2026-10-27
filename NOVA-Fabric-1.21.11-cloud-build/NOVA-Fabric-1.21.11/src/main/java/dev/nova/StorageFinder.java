
package dev.nova;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.BlockState;

public final class StorageFinder {
    public static boolean enabled = false;

    public static int highlightColor = 0xFF9B5CFF;

    private StorageFinder() {
    }

    public static boolean isStorage(BlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.TRAPPED_CHEST) {
            return false;
        }

        return block == Blocks.CHEST
                || block instanceof ShulkerBoxBlock
                || block == Blocks.BARREL
                || block == Blocks.DROPPER
                || block == Blocks.DISPENSER
                || block == Blocks.HOPPER;
    }

    public static void toggle() {
        enabled = !enabled;
    }
}

