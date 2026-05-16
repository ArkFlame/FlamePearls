package com.arkflame.flamepearls.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public final class WorldUtil {
    private static final String UNKNOWN_WORLD = "unknown";

    private WorldUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean isDifferentWorld(final Location from, final Location to) {
        return !isSameWorld(from, to);
    }

    public static boolean isSameWorld(final Location first, final Location second) {
        if (first == null || second == null) {
            return false;
        }
        return isSameWorld(first.getWorld(), second.getWorld());
    }

    public static boolean isSameWorld(final World first, final World second) {
        if (first == null || second == null) {
            return false;
        }
        return first.getName().equals(second.getName());
    }

    public static String getWorldName(final Location location) {
        if (location == null) {
            return UNKNOWN_WORLD;
        }
        return getWorldName(location.getWorld());
    }

    public static String getWorldName(final World world) {
        if (world == null) {
            return UNKNOWN_WORLD;
        }
        return world.getName();
    }

    public static boolean isInsideWorldBorder(final Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        final WorldBorder border = location.getWorld().getWorldBorder();
        final double halfSize = border.getSize() / 2.0D;
        final double centerX = border.getCenter().getX();
        final double centerZ = border.getCenter().getZ();
        final double x = location.getX();
        final double z = location.getZ();
        return x >= centerX - halfSize
                && x <= centerX + halfSize
                && z >= centerZ - halfSize
                && z <= centerZ + halfSize;
    }
}
