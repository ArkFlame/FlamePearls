package com.arkflame.flamepearls.utils.pearl;

import com.arkflame.flamepearls.utils.FoliaAPI;
import com.arkflame.flamepearls.utils.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.Optional;

public final class PearlCollisionPredictor {
    private static final int MAX_DDA_STEPS = 4;
    private static final int MAX_CANDIDATES = 8;
    private static final int MAX_BLOCK_CHECKS = 32;
    private static final double PEARL_RADIUS = 0.125D;
    private static final double MIN_VELOCITY_SQUARED = 1.0E-5D;
    private static final double MIN_HIT_FRACTION = 0.05D;
    private static final double MAX_HIT_FRACTION = 1.0D;
    private static final double PARALLEL_EPSILON = 1.0E-12D;

    private PearlCollisionPredictor() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Optional<PredictedPearlCollision> predict(final Location start, final Vector velocity) {
        return predictNextTick(start, velocity);
    }

    public static Optional<PredictedPearlCollision> predictNextTick(final Location start, final Vector velocity) {
        if (start == null || velocity == null || velocity.lengthSquared() < MIN_VELOCITY_SQUARED) {
            return Optional.empty();
        }
        final World world = start.getWorld();
        if (world == null) {
            return Optional.empty();
        }

        final double startX = start.getX();
        final double startY = start.getY();
        final double startZ = start.getZ();
        final double deltaX = velocity.getX();
        final double deltaY = velocity.getY();
        final double deltaZ = velocity.getZ();

        int currentX = floor(startX);
        int currentY = floor(startY);
        int currentZ = floor(startZ);
        final int stepX = axisStep(deltaX);
        final int stepY = axisStep(deltaY);
        final int stepZ = axisStep(deltaZ);
        double tMaxX = initialTMax(startX, deltaX, stepX);
        double tMaxY = initialTMax(startY, deltaY, stepY);
        double tMaxZ = initialTMax(startZ, deltaZ, stepZ);
        final double tDeltaX = tDelta(deltaX);
        final double tDeltaY = tDelta(deltaY);
        final double tDeltaZ = tDelta(deltaZ);

        final CandidateBlock[] candidates = new CandidateBlock[MAX_CANDIDATES];
        double bestHitFraction = Double.POSITIVE_INFINITY;
        Location bestLocation = null;
        int bestBlockX = 0;
        int bestBlockY = 0;
        int bestBlockZ = 0;
        int blockChecks = 0;
        double t = 0.0D;

        for (int ddaStep = 0; ddaStep < MAX_DDA_STEPS && t <= MAX_HIT_FRACTION && blockChecks < MAX_BLOCK_CHECKS; ddaStep++) {
            final double pointX = startX + deltaX * t;
            final double pointY = startY + deltaY * t;
            final double pointZ = startZ + deltaZ * t;
            final int candidateCount = collectRadiusCandidates(pointX, pointY, pointZ, candidates);

            for (int i = 0; i < candidateCount && blockChecks < MAX_BLOCK_CHECKS; i++) {
                final CandidateBlock candidate = candidates[i];
                blockChecks++;
                final Location blockLocation = new Location(world, candidate.x, candidate.y, candidate.z);
                if (!FoliaAPI.isOwnedByCurrentRegion(blockLocation)) {
                    continue;
                }
                final Block block = world.getBlockAt(candidate.x, candidate.y, candidate.z);
                if (!isBlockingPearl(block)) {
                    continue;
                }
                final double hitFraction = segmentExpandedBlockHitT(
                        startX,
                        startY,
                        startZ,
                        deltaX,
                        deltaY,
                        deltaZ,
                        candidate.x,
                        candidate.y,
                        candidate.z,
                        PEARL_RADIUS
                );
                if (hitFraction > MIN_HIT_FRACTION && hitFraction <= MAX_HIT_FRACTION && hitFraction < bestHitFraction) {
                    bestHitFraction = hitFraction;
                    bestLocation = start.clone().add(deltaX * hitFraction, deltaY * hitFraction, deltaZ * hitFraction);
                    bestBlockX = candidate.x;
                    bestBlockY = candidate.y;
                    bestBlockZ = candidate.z;
                }
            }

            final double nextT = Math.min(tMaxX, Math.min(tMaxY, tMaxZ));
            if (Double.isInfinite(nextT) || nextT > MAX_HIT_FRACTION) {
                break;
            }
            if (Double.compare(tMaxX, nextT) == 0) {
                currentX += stepX;
                tMaxX += tDeltaX;
            }
            if (Double.compare(tMaxY, nextT) == 0) {
                currentY += stepY;
                tMaxY += tDeltaY;
            }
            if (Double.compare(tMaxZ, nextT) == 0) {
                currentZ += stepZ;
                tMaxZ += tDeltaZ;
            }
            t = nextT;
        }

        if (bestLocation == null) {
            return Optional.empty();
        }
        return Optional.of(new PredictedPearlCollision(bestLocation, bestHitFraction, bestBlockX, bestBlockY, bestBlockZ));
    }

    private static boolean isBlockingPearl(final Block block) {
        if (block == null) {
            return false;
        }
        final Material type = block.getType();
        return type != null && type != Material.AIR && !isLiquid(type) && type.isSolid() && !LocationUtil.isSafe(block);
    }

    private static boolean isLiquid(final Material type) {
        final String name = type.name();
        return name.indexOf("WATER") >= 0 || name.indexOf("LAVA") >= 0;
    }

    private static int collectRadiusCandidates(final double pointX,
                                               final double pointY,
                                               final double pointZ,
                                               final CandidateBlock[] out) {
        final int minX = floor(pointX - PEARL_RADIUS);
        final int maxX = floor(pointX + PEARL_RADIUS);
        final int minY = floor(pointY - PEARL_RADIUS);
        final int maxY = floor(pointY + PEARL_RADIUS);
        final int minZ = floor(pointZ - PEARL_RADIUS);
        final int maxZ = floor(pointZ + PEARL_RADIUS);

        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (count >= out.length) {
                        return count;
                    }
                    if (!contains(out, count, x, y, z)) {
                        out[count++] = new CandidateBlock(x, y, z);
                    }
                }
            }
        }
        return count;
    }

    private static boolean contains(final CandidateBlock[] blocks, final int count, final int x, final int y, final int z) {
        for (int i = 0; i < count; i++) {
            final CandidateBlock block = blocks[i];
            if (block.x == x && block.y == y && block.z == z) {
                return true;
            }
        }
        return false;
    }

    private static int axisStep(final double delta) {
        if (delta > PARALLEL_EPSILON) {
            return 1;
        }
        if (delta < -PARALLEL_EPSILON) {
            return -1;
        }
        return 0;
    }

    private static double initialTMax(final double coord, final double delta, final int step) {
        if (step == 0 || Math.abs(delta) < PARALLEL_EPSILON) {
            return Double.POSITIVE_INFINITY;
        }
        final double boundary = step > 0 ? Math.floor(coord) + 1.0D : Math.floor(coord);
        final double t = (boundary - coord) / delta;
        return t < 0.0D ? 0.0D : t;
    }

    private static double tDelta(final double delta) {
        if (Math.abs(delta) < PARALLEL_EPSILON) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.abs(1.0D / delta);
    }

    private static double segmentExpandedBlockHitT(final double originX,
                                                   final double originY,
                                                   final double originZ,
                                                   final double deltaX,
                                                   final double deltaY,
                                                   final double deltaZ,
                                                   final int blockX,
                                                   final int blockY,
                                                   final int blockZ,
                                                   final double expansion) {
        Interval interval = new Interval(0.0D, 1.0D);
        interval = slab(originX, deltaX, blockX - expansion, blockX + 1.0D + expansion, interval);
        if (interval == null) {
            return -1.0D;
        }
        interval = slab(originY, deltaY, blockY - expansion, blockY + 1.0D + expansion, interval);
        if (interval == null) {
            return -1.0D;
        }
        interval = slab(originZ, deltaZ, blockZ - expansion, blockZ + 1.0D + expansion, interval);
        if (interval == null) {
            return -1.0D;
        }
        return interval.min >= 0.0D && interval.min <= 1.0D ? interval.min : -1.0D;
    }

    private static Interval slab(final double origin,
                                 final double delta,
                                 final double min,
                                 final double max,
                                 final Interval interval) {
        if (Math.abs(delta) < PARALLEL_EPSILON) {
            return origin >= min && origin <= max ? interval : null;
        }

        double t1 = (min - origin) / delta;
        double t2 = (max - origin) / delta;
        if (t1 > t2) {
            final double swap = t1;
            t1 = t2;
            t2 = swap;
        }

        final double nextMin = Math.max(interval.min, t1);
        final double nextMax = Math.min(interval.max, t2);
        return nextMin <= nextMax ? new Interval(nextMin, nextMax) : null;
    }

    private static int floor(final double value) {
        return (int) Math.floor(value);
    }

    private static final class CandidateBlock {
        private final int x;
        private final int y;
        private final int z;

        private CandidateBlock(final int x, final int y, final int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class Interval {
        private final double min;
        private final double max;

        private Interval(final double min, final double max) {
            this.min = min;
            this.max = max;
        }
    }
}
