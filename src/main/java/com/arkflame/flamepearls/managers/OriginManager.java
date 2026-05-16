package com.arkflame.flamepearls.managers;

import com.arkflame.flamepearls.utils.WorldUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Stores and manages ender pearl origin state.
public class OriginManager {
    private final Map<Projectile, Location> projectileOrigins = new ConcurrentHashMap<>();
    private final Map<UUID, BlockedWorldSwitch> blockedWorldSwitches = new ConcurrentHashMap<>();

    private int projectileCount = 0;

    public void setOrigin(final Projectile projectile, final Location location) {
        if (projectile == null || location == null) {
            return;
        }
        projectileOrigins.put(projectile, location.clone());
        projectileCount++;
    }

    public Location getOriginAndRemove(final Projectile projectile) {
        return projectileOrigins.remove(projectile);
    }

    public int getProjectileCount() {
        return projectileCount;
    }

    public Collection<Projectile> getProjectiles() {
        return projectileOrigins.keySet();
    }

    public void markBlockedWorldSwitch(final Player player, final Location rollbackLocation, final Location targetLocation) {
        if (player == null || rollbackLocation == null) {
            return;
        }
        blockedWorldSwitches.put(
                player.getUniqueId(),
                new BlockedWorldSwitch(rollbackLocation.clone(), WorldUtil.getWorldName(targetLocation), System.currentTimeMillis())
        );
    }

    public Optional<Location> consumeBlockedWorldSwitch(final Player player,
                                                       final World fromWorld,
                                                       final World currentWorld,
                                                       final long maxAgeMillis) {
        if (player == null) {
            return Optional.empty();
        }
        final BlockedWorldSwitch blocked = blockedWorldSwitches.remove(player.getUniqueId());
        if (blocked == null) {
            return Optional.empty();
        }
        final long ageMillis = System.currentTimeMillis() - blocked.createdAtMillis;
        if (ageMillis < 0L || ageMillis > maxAgeMillis) {
            return Optional.empty();
        }
        if (!WorldUtil.isSameWorld(blocked.rollbackLocation.getWorld(), fromWorld)) {
            return Optional.empty();
        }
        if (currentWorld == null || !blocked.targetWorldName.equals(currentWorld.getName())) {
            return Optional.empty();
        }
        return Optional.of(blocked.rollbackLocation.clone());
    }

    private static final class BlockedWorldSwitch {
        private final Location rollbackLocation;
        private final String targetWorldName;
        private final long createdAtMillis;

        private BlockedWorldSwitch(final Location rollbackLocation, final String targetWorldName, final long createdAtMillis) {
            this.rollbackLocation = Objects.requireNonNull(rollbackLocation, "rollbackLocation");
            this.targetWorldName = Objects.requireNonNull(targetWorldName, "targetWorldName");
            this.createdAtMillis = createdAtMillis;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof BlockedWorldSwitch)) {
                return false;
            }
            final BlockedWorldSwitch that = (BlockedWorldSwitch) other;
            return createdAtMillis == that.createdAtMillis
                    && rollbackLocation.equals(that.rollbackLocation)
                    && targetWorldName.equals(that.targetWorldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rollbackLocation, targetWorldName, createdAtMillis);
        }

        @Override
        public String toString() {
            return "BlockedWorldSwitch{" +
                    "rollbackLocation=" + rollbackLocation +
                    ", targetWorldName='" + targetWorldName + '\'' +
                    ", createdAtMillis=" + createdAtMillis +
                    '}';
        }
    }
}