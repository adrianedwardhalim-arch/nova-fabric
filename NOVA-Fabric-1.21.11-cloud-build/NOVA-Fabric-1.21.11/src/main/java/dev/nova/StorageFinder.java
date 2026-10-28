package dev.nova;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;

public final class StorageFinder {
    public static boolean enabled = false;
    public static int highlightColor = 0xFF9B5CFF;
    public static int range = 6;

    private static final int[] COLORS = {
            0xFF9B5CFF,
            0xFF42D9FF,
            0xFF59E38C,
            0xFFFFC857
    };
    private static final String[] COLOR_NAMES = {
            "Purple", "Cyan", "Green", "Gold"
    };
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
        range = range >= 12 ? 4 : range + 2;
    }

    public static void cycleColor() {
        colorIndex = (colorIndex + 1) % COLORS.length;
        highlightColor = COLORS[colorIndex];
    }

    public static String colorName() {
        return COLOR_NAMES[colorIndex];
    }
}
