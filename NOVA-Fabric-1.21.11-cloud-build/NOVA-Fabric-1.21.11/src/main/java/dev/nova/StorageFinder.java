package dev.nova;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;

public final class StorageFinder {
    public static boolean enabled = false;
    public static int highlightColor = 0xFF9B5CFF;
    // Larger values let the finder inspect storage blocks farther away.
    public static int range = 12;

    private static final int[] RANGES = {8, 12, 16, 20, 24};
    private static final int[] COLORS = {
            0xFF9B5CFF,
            0xFF42D9FF,
            0xFF59E38C,
            0xFFFFC857
    };
    private static final String[] COLOR_NAMES = {
            "Purple", "Cyan", "Green", "Gold"
    };
    private static int rangeIndex = 1;
    private static int colorIndex = 0;

    private StorageFinder() {
    }

    public static boolean isStorage(BlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.TRAPPED_CHEST) {
            return false;
        }

        return block == Blocks.CHEST
                || block == Blocks.ENDER_CHEST
                || block instanceof ShulkerBoxBlock
                || block == Blocks.BARREL
                || block == Blocks.DROPPER
                || block == Blocks.DISPENSER
                || block == Blocks.HOPPER;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static void cycleRange() {
        rangeIndex = (rangeIndex + 1) % RANGES.length;
        range = RANGES[rangeIndex];
    }

    public static void cycleColor() {
        colorIndex = (colorIndex + 1) % COLORS.length;
        highlightColor = COLORS[colorIndex];
    }

    public static String colorName() {
        return COLOR_NAMES[colorIndex];
    }
}
