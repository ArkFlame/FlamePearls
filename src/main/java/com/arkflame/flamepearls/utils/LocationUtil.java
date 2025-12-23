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
                isStep(typeName) ||
                typeName.endsWith("CARPET");
    }

    public static boolean isStep(String typeName) {
        return typeName.contains("_SLAB") ||
                typeName.contains("_STEP") ||
                typeName.equals("STEP");
    }

    public static boolean isStep(Location testLocation) {
        return isStep(testLocation.getBlock().getType().name());
    }

    public static Material getTypeAt(Location location) {
        return location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ())
                .getType();
    }

    public static boolean isSafe(Location location) {
        Material type = getTypeAt(location);
        boolean safe = isSafe(type);
        if (!safe) {
            return false;
        }
        return isSafe(location.clone().add(0, 1, 0).getBlock().getType())
                || isSafe(location.clone().add(0, -2, 0).getBlock().getType());
    }

    private static boolean isSlab(Material type) {
        return type.name().endsWith("SLAB");
    }

    public static boolean isSlab(Location location) {
        return isSlab(location.getBlock().getType());
    }

    public static Location findSafeY(Player player, Location pearlLocation, Location origin, World world) {
        boolean searchUp = origin.getBlockY() + 1 > pearlLocation.getBlockY();
        Location testLocation = pearlLocation.clone();
        int attempts = 0;
        testLocation.setY(testLocation.getBlockY());
        while (!isSafe(testLocation) && attempts++ < 3) {
            testLocation.add(0, searchUp ? 1 : -1, 0);
            if (isStep(testLocation)) {
                testLocation.add(0, searchUp ? 0.5 : -0.5, 0);
            }
        }
        if (!isSafe(testLocation)) {
            return pearlLocation;
        } else {
            return testLocation;
        }
    }

    private static Location findSafeXYZ(Player player, Location pearlLocation, Location origin, World world) {
        Location pearlBlockLocation = pearlLocation.getBlock().getLocation().add(0.5, 0, 0.5);
        if (isSafe(pearlBlockLocation)) {
            return pearlBlockLocation;
        }

        double bestDistance = Double.MAX_VALUE;
        Location bestLocation = pearlBlockLocation.clone();
        Location testLocation = pearlBlockLocation.clone();

        double[] sideOffsets = { -0.5, 0, 0.5 };
        // Check in all 8 directions (4 cardinal + 4 diagonal)
        double[] offsets = { -1, 0, 1 }; // Now includes 0 for single-axis checks
        for (double xOffset : offsets) {
            for (double zOffset : offsets) {
                for (double yOffset : offsets) {
                    // Skip (0,0,0) because it's the original position
                    if (xOffset == 0 && zOffset == 0 && yOffset == 0) {
                        continue;
                    }

                    for (double sideXOffset : sideOffsets) {
                        for (double sideZOffset : sideOffsets) {
                            // Apply side offset
                            testLocation.setX(pearlBlockLocation.getX() + xOffset + sideXOffset);
                            testLocation.setY(pearlBlockLocation.getY() + yOffset);
                            testLocation.setZ(pearlBlockLocation.getZ() + zOffset + sideZOffset);
                            // Find nearest to original position
                            double distance = testLocation.distance(origin);
                            if (distance < bestDistance) {
                                bestDistance = distance;
                                bestLocation.setX(pearlBlockLocation.getX() + xOffset + sideXOffset);
                                bestLocation.setY(pearlBlockLocation.getY() + yOffset);
                                bestLocation.setZ(pearlBlockLocation.getZ() + zOffset + sideZOffset);
                            }
                        }
                    }
                }
            }
        }

        return bestLocation;
    }

    public static Location findSafeLocation(Player player, Location location, Location origin, World world) {
        Location testLocation = location.clone();
        testLocation = findSafeXYZ(player, testLocation, origin, world);
        // Location changed, apply safe location
        if (!testLocation.equals(location)) {
            return testLocation;
        }

        return location;
    }
    
    /**
     * Checks if the head is stuck in a block.
     * If stuck, it checks if the block below is safe for both feet and head.
     * 
     * @param location The current calculated teleport location (feet)
     * @return A location moved down 1 block if the head was stuck and the lower position is safe.
     */
    public static Location fixHeadStuck(Location location) {
        // The block where the head will be (1 block above the feet)
        Location headLocation = location.clone().add(0, 1, 0);
        Material headType = getTypeAt(headLocation);
        String headName = headType.name();

        // Skip for any materials containing "SLAB" or "STEP"
        if (headName.contains("SLAB") || headName.contains("STEP")) {
            return location;
        }

        // Check if the current head is stuck (not safe)
        if (!isSafe(headType)) {
            // Get the location 1 block below the current feet
            Location lowerLocation = location.clone().add(0, -1, 0);
            
            // New feet is the block below, new head is the current feet block
            Material newFeetType = getTypeAt(lowerLocation);
            Material newHeadType = getTypeAt(location); // location is 1 block above lowerLocation

            // If the lower position is safe for both feet and head, return it
            if (isSafe(newFeetType) && isSafe(newHeadType)) {
                return lowerLocation;
            }
        }

        return location;
    }
}
