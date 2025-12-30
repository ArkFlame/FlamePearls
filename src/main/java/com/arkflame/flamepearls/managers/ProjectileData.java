package com.arkflame.flamepearls.managers;

import org.bukkit.Location;

public class ProjectileData {
    private final Location origin;
    private int ticksLived;

    public ProjectileData(Location origin) {
        this.origin = origin;
        this.ticksLived = 0;
    }

    public Location getOrigin() {
        return origin;
    }

    public int getTicksLived() {
        return ticksLived;
    }

    public void addTicks(long ticks) {
        this.ticksLived += (int) ticks;
    }
}