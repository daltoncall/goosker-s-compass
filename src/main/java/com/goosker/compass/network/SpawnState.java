package com.goosker.compass.network;

import net.minecraft.util.math.BlockPos;

public final class SpawnState {
    private static volatile String dimensionId;
    private static volatile BlockPos position;

    private SpawnState() {}

    public static void set(String dimension, BlockPos pos) {
        dimensionId = dimension;
        position = pos;
    }

    public static void clear() {
        dimensionId = null;
        position = null;
    }

    public static boolean hasCustomSpawn() {
        return dimensionId != null && position != null;
    }

    public static String dimensionId() {
        return dimensionId;
    }

    public static BlockPos position() {
        return position;
    }
}
