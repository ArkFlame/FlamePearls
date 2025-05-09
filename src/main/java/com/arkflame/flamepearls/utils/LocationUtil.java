package com.arkflame.flamepearls.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class LocationUtil {
    public static boolean isSafe(Material type) {
        if (type == null) {
            return true;
        }

        String typeName = type.name();
        return type == Material.AIR ||
                !type.isSolid() ||
                typeName.equals("REDSTONE") ||
                typeName.equals("TRIPWIRE_HOOK") ||
                typeName.endsWith("PRESSURE_PLATE") ||
                typeName.equals("TALL_GRASS") ||
                typeName.equals("LONG_GRASS") ||
                typeName.endsWith("CARPET");
    }

    public static boolean isSafe(Location location) {
        Material type = location.getBlock().getType();
        boolean safe = isSafe(type);
        boolean aboveSafe = isSafe(location.clone().add(0, 1, 0).getBlock().getType());
        return safe && aboveSafe;
    }

    private static boolean isSlab(Material type) {
        return type.name().endsWith("SLAB");
    }

    public static boolean isSlab(Location location) {
        boolean slab = isSlab(location.getBlock().getType());
        return slab;
    }

    public static Location findSafeY(Location location, Location origin, World world) {
        Location testLocation = location.clone();
        int attempts = 0;
        boolean wasSlab = false;

        testLocation.setY(testLocation.getBlockY());

        while (!isSafe(testLocation) && attempts < 2) {
            attempts++;
            wasSlab = isSlab(testLocation);
            testLocation.add(0, 1, 0);
        }

        if (attempts == 0 || !isSafe(testLocation)) {
            return location;
        }

        if (wasSlab) {
            testLocation.subtract(0, 0.5, 0);
        }

        return testLocation;
    }

    public static double[] getDirectionToOrigin(Location location, Location origin) {
        double dx = origin.getX() - location.getX();
        double dz = origin.getZ() - location.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length == 0) {
            return new double[]{0, 0};
        }

        double sin = dz / length;
        double cos = dx / length;

        return new double[]{sin, cos};
    }

    public static Location findSafeLocation(Location location, Location origin, World world) {
        Location testLocation = location.clone();
        double[] sinCos = getDirectionToOrigin(location, origin);
        int iterations = 0;

        while (iterations < 10) {
            if (iterations++ > 0) {
                testLocation.add(sinCos[0], 0, sinCos[1]);
            }

            if (isSafe(testLocation)) {
                return testLocation.getBlock().getLocation().add(0.5, 0, 0.5);
            }

            Location originalTestLocation = testLocation.clone();
            testLocation = findSafeY(testLocation, origin, world);

            if (!testLocation.equals(originalTestLocation)) {
                return testLocation.getBlock().getLocation().add(0.5, 0, 0.5);
            }
        }

        return location;
    }

    private static String formatLocation(Location loc) {
        return String.format("[%.2f, %.2f, %.2f] in %s", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }
}
