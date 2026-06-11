package com.arkflame.flamepearls.utils.pearl;

import org.bukkit.Location;

import java.util.Objects;

public final class PredictedPearlCollision {
    private final Location collisionLocation;
    private final double ticksUntilHit;
    private final int blockX;
    private final int blockY;
    private final int blockZ;

    public PredictedPearlCollision(final Location collisionLocation,
                                   final double ticksUntilHit,
                                   final int blockX,
                                   final int blockY,
                                   final int blockZ) {
        if (Double.isNaN(ticksUntilHit) || Double.isInfinite(ticksUntilHit) || ticksUntilHit <= 0.0D) {
            throw new IllegalArgumentException("ticksUntilHit must be positive finite");
        }
        this.collisionLocation = Objects.requireNonNull(collisionLocation, "collisionLocation").clone();
        this.ticksUntilHit = ticksUntilHit;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    public Location getCollisionLocation() {
        return collisionLocation.clone();
    }

    public double getTicksUntilHit() {
        return ticksUntilHit;
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PredictedPearlCollision)) {
            return false;
        }
        final PredictedPearlCollision that = (PredictedPearlCollision) other;
        return Double.compare(that.ticksUntilHit, ticksUntilHit) == 0
                && blockX == that.blockX
                && blockY == that.blockY
                && blockZ == that.blockZ
                && collisionLocation.equals(that.collisionLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collisionLocation, ticksUntilHit, blockX, blockY, blockZ);
    }

    @Override
    public String toString() {
        return "PredictedPearlCollision{" +
                "collisionLocation=" + collisionLocation +
                ", ticksUntilHit=" + ticksUntilHit +
                ", blockX=" + blockX +
                ", blockY=" + blockY +
                ", blockZ=" + blockZ +
                '}';
    }
}
