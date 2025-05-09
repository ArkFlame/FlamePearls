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
        System.out.println("Checking if block at " + formatLocation(location) + " is safe: " + type + " -> " + safe);
        return safe && aboveSafe;
    }

    private static boolean isSlab(Material type) {
        return type.name().endsWith("SLAB");
    }

    public static boolean isSlab(Location location) {
        boolean slab = isSlab(location.getBlock().getType());
        System.out.println("Checking if block at " + formatLocation(location) + " is a slab: " + location.getBlock().getType() + " -> " + slab);
        return slab;
    }

    public static Location findSafeY(Location location, Location origin, World world) {
        Location testLocation = location.clone();
        int attempts = 0;
        boolean wasSlab = false;

        testLocation.setY(testLocation.getBlockY());

        System.out.println("Starting vertical safety check from " + formatLocation(testLocation));
        while (!isSafe(testLocation) && attempts < 2) {
            attempts++;
            wasSlab = isSlab(testLocation);
            testLocation.add(0, 1, 0);
            System.out.println("Moved up to " + formatLocation(testLocation) + ", attempt " + attempts);
        }

        if (attempts == 0 || !isSafe(testLocation)) {
            System.out.println("No safe Y position found. Returning original location.");
            return location;
        }

        if (wasSlab) {
            testLocation.subtract(0, 0.5, 0);
            System.out.println("Adjusted for slab at " + formatLocation(testLocation));
        }

        System.out.println("Safe Y found: " + formatLocation(testLocation));
        return testLocation;
    }

    public static double[] getDirectionToOrigin(Location location, Location origin) {
        double dx = origin.getX() - location.getX();
        double dz = origin.getZ() - location.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length == 0) {
            System.out.println("Location equals origin. No direction.");
            return new double[]{0, 0};
        }

        double sin = dz / length;
        double cos = dx / length;

        System.out.println("Direction to origin from " + formatLocation(location) + " to " + formatLocation(origin) + ": sin=" + sin + ", cos=" + cos);
        return new double[]{sin, cos};
    }

    public static Location findSafeLocation(Location location, Location origin, World world) {
        Location testLocation = location.clone();
        double[] sinCos = getDirectionToOrigin(location, origin);
        int iterations = 0;

        System.out.println("Starting safe location search from " + formatLocation(location));

        while (iterations < 10) {
            if (iterations++ > 0) {
                testLocation.add(sinCos[0], 0, sinCos[1]);
                System.out.println("Iteration " + iterations + ": moved to " + formatLocation(testLocation));
            }

            if (isSafe(testLocation)) {
                System.out.println("Found safe location: " + formatLocation(testLocation));
                return testLocation.getBlock().getLocation().add(0.5, 0, 0.5);
            }

            Location originalTestLocation = testLocation.clone();
            testLocation = findSafeY(testLocation, origin, world);

            if (!testLocation.equals(originalTestLocation)) {
                System.out.println("Found safe location after Y-check: " + formatLocation(testLocation));
                return testLocation.getBlock().getLocation().add(0.5, 0, 0.5);
            }
        }

        System.out.println("Failed to find safe location. Returning original.");
        return location;
    }

    private static String formatLocation(Location loc) {
        return String.format("[%.2f, %.2f, %.2f] in %s", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }
}
