package com.arkflame.flamepearls.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

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
                typeName.equals("FLOWER_POT") ||
                typeName.contains("_SLAB") ||
                typeName.contains("_STEP") ||
                typeName.equals("STEP") ||
                typeName.endsWith("CARPET");
    }

    public static boolean isSafe(Location location) {
        Material type = location.getBlock().getType();
        boolean safe = isSafe(type);
        if (!safe) {
            return false;
        }
        boolean aboveOrBelowSafe = isSafe(location.clone().add(0, 1, 0).getBlock().getType())
                || isSafe(location.clone().add(0, -2, 0).getBlock().getType());
        return safe && aboveOrBelowSafe;
    }

    private static boolean isSlab(Material type) {
        return type.name().endsWith("SLAB");
    }

    public static boolean isSlab(Location location) {
        boolean slab = isSlab(location.getBlock().getType());
        return slab;
    }

    public static Location findSafeY(Player player, Location pearlLocation, Location origin, World world) {
        Location testLocation = pearlLocation.clone();
        int attempts = 0;
        boolean wasSlab = false;

        testLocation.setY(testLocation.getBlockY());

        while (!isSafe(testLocation) && attempts++ < 2) {
            wasSlab = isSlab(testLocation);
            testLocation.add(0, 0.99, 0);
        }

        if (attempts == 0 || !isSafe(testLocation)) {
            return pearlLocation;
        }

        if (wasSlab) {
            testLocation.subtract(0, 0.5, 0);
        }

        if (!isSafe(testLocation)) {
            return pearlLocation;
        }

        return testLocation;
    }

    private static Location findSafeXZ(Player player, Location pearlLocation, Location origin, World world) {
        if (isSafe(pearlLocation)) {
            return pearlLocation;
        }
    
        double originalX = pearlLocation.getX();
        double originalZ = pearlLocation.getZ();
        pearlLocation = pearlLocation.getBlock().getLocation().add(0.5, 0, 0.5);
        double bestDistance = Double.MAX_VALUE;
        Location bestLocation = pearlLocation.clone();
        Location testLocation = pearlLocation.clone();
        testLocation.setY(origin.getY());
    
        // Check in all 8 directions (4 cardinal + 4 diagonal)
        double[] offsets = { -1, 0, 1 }; // Now includes 0 for single-axis checks
        for (double xOffset : offsets) {
            for (double zOffset : offsets) {
                // Skip (0,0) because it's the original position
                if (xOffset == 0 && zOffset == 0) {
                    continue;
                }
    
                testLocation.setX(pearlLocation.getX() + xOffset);
                testLocation.setZ(pearlLocation.getZ() + zOffset);
    
                double distance = testLocation.distance(origin);
    
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestLocation.setX(originalX + xOffset);
                    bestLocation.setZ(originalZ + zOffset);
                }
            }
        }


        if (isSafe(bestLocation)) {
            return bestLocation;
        }
    
        pearlLocation.setX(originalX);
        pearlLocation.setZ(originalZ);
        return pearlLocation;
    }

    public static Location findSafeLocation(Player player, Location location, Location origin, World world) {
        Location testLocation = location.clone();
        testLocation = findSafeXZ(player, testLocation, origin, world);

        if (isSafe(testLocation)) {
            testLocation.setY(testLocation.getBlockY() + (testLocation.getBlock().getType() == Material.AIR ? 0 : 1));
            return testLocation;
        }

        return location;
    }
}
