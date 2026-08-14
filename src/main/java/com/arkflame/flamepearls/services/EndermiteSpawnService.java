package com.arkflame.flamepearls.services;

import com.arkflame.flamepearls.compat.endermite.EndermiteSpawnBridge;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.utils.FoliaAPI;
import org.bukkit.Location;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

public final class EndermiteSpawnService {
    private static final long RECENT_IMPACT_TTL_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final double RECENT_IMPACT_RADIUS_SQUARED = 0.25D;
    private static final int MAX_RECENT_IMPACTS = 4096;
    private static final int RECENT_IMPACT_RING_MASK = MAX_RECENT_IMPACTS - 1;

    private final GeneralConfigHolder generalConfigHolder;
    private final EndermiteSpawnBridge spawnBridge;
    private final DoubleSupplier random;
    private final LongSupplier nanoClock;
    private final boolean enderPearlReasonAvailable;
    private final ConcurrentMap<UUID, ConcurrentMap<Long, RecentPearlImpact>> recentImpactsByWorldChunk = new ConcurrentHashMap<>();
    private final AtomicReferenceArray<RecentPearlImpact> recentImpactEvictionRing = new AtomicReferenceArray<>(MAX_RECENT_IMPACTS);
    private final AtomicLong recentImpactSequence = new AtomicLong();

    public EndermiteSpawnService(final GeneralConfigHolder generalConfigHolder,
                                 final EndermiteSpawnBridge spawnBridge) {
        this(generalConfigHolder, spawnBridge, Math::random, System::nanoTime);
    }

    EndermiteSpawnService(final GeneralConfigHolder generalConfigHolder,
                          final EndermiteSpawnBridge spawnBridge,
                          final DoubleSupplier random,
                          final LongSupplier nanoClock) {
        this.generalConfigHolder = Objects.requireNonNull(generalConfigHolder, "generalConfigHolder");
        this.spawnBridge = Objects.requireNonNull(spawnBridge, "spawnBridge");
        this.random = Objects.requireNonNull(random, "random");
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
        this.enderPearlReasonAvailable = resolveEnderPearlReasonAvailability();
    }

    private static boolean resolveEnderPearlReasonAvailability() {
        try {
            CreatureSpawnEvent.SpawnReason.valueOf("ENDER_PEARL");
            return true;
        } catch (final IllegalArgumentException exception) {
            return false;
        }
    }

    public void recordPotentialNativePearlImpact(final ProjectileHitEvent event) {
        if (enderPearlReasonAvailable) {
            return;
        }
        if (generalConfigHolder.isEndermitesEnabled()) {
            return;
        }
        if (event == null) {
            return;
        }
        final Projectile projectile = event.getEntity();
        if (!(projectile instanceof EnderPearl)) {
            return;
        }
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        final Player player = (Player) projectile.getShooter();
        final Location location = player.getLocation().clone();
        recordRecentImpact(location);
    }

    public boolean isPearlRelated(final CreatureSpawnEvent event) {
        if (event == null || !(event.getEntity() instanceof Endermite)) {
            return false;
        }
        if (event.getSpawnReason() != null && event.getSpawnReason().name().equals("ENDER_PEARL")) {
            return true;
        }
        return consumeRecentImpactNear(event.getEntity().getLocation());
    }

    void recordRecentImpact(final Location location) {
        if (!isUsable(location)) {
            return;
        }
        final UUID worldId = location.getWorld().getUID();
        final long chunkKey = chunkKey(location);
        final long now = nanoClock.getAsLong();
        final RecentPearlImpact impact = new RecentPearlImpact(
                worldId,
                chunkKey,
                location.getX(),
                location.getY(),
                location.getZ(),
                now + RECENT_IMPACT_TTL_NANOS
        );
        recentImpactsByWorldChunk.computeIfAbsent(worldId, ignored -> new ConcurrentHashMap<>()).put(chunkKey, impact);
        final int slot = (int) (recentImpactSequence.getAndIncrement() & RECENT_IMPACT_RING_MASK);
        final RecentPearlImpact evicted = recentImpactEvictionRing.getAndSet(slot, impact);
        if (evicted != null) {
            removeIndexedImpact(evicted);
        }
    }

    boolean consumeRecentImpactNear(final Location location) {
        if (!isUsable(location)) {
            return false;
        }
        final UUID worldId = location.getWorld().getUID();
        final ConcurrentMap<Long, RecentPearlImpact> worldIndex = recentImpactsByWorldChunk.get(worldId);
        if (worldIndex == null) {
            return false;
        }
        final long chunkKey = chunkKey(location);
        final RecentPearlImpact impact = worldIndex.get(chunkKey);
        if (impact == null) {
            return false;
        }
        final long now = nanoClock.getAsLong();
        if (impact.expiresAtNanos <= now) {
            worldIndex.remove(chunkKey, impact);
            return false;
        }
        final double dx = location.getX() - impact.x;
        final double dy = location.getY() - impact.y;
        final double dz = location.getZ() - impact.z;
        if (dx * dx + dy * dy + dz * dz > RECENT_IMPACT_RADIUS_SQUARED) {
            return false;
        }
        return worldIndex.remove(chunkKey, impact);
    }

    private void removeIndexedImpact(final RecentPearlImpact impact) {
        final ConcurrentMap<Long, RecentPearlImpact> worldIndex = recentImpactsByWorldChunk.get(impact.worldId);
        if (worldIndex == null) {
            return;
        }
        worldIndex.remove(impact.chunkKey, impact);
    }

    private static long chunkKey(final Location location) {
        return packChunkKey(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    static long packChunkKey(final int chunkX, final int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    public void scheduleCustomImpactFallback(final Location preTeleportLocation,
                                             final CompletableFuture<Boolean> teleportFuture) {
        if (preTeleportLocation == null || teleportFuture == null) {
            return;
        }
        final Location spawnLocation = preTeleportLocation.clone();
        teleportFuture.whenComplete((success, throwable) -> {
            if (throwable != null || !Boolean.TRUE.equals(success)) {
                return;
            }
            FoliaAPI.runTaskForRegion(spawnLocation, () -> {
                if (!shouldAttemptFallback(generalConfigHolder.isEndermitesEnabled(), generalConfigHolder.getEndermiteChance(), random)) {
                    return;
                }
                spawnBridge.spawn(spawnLocation);
            });
        });
    }

    static boolean shouldAttemptFallback(final boolean endermitesEnabled,
                                         final double chance,
                                         final DoubleSupplier random) {
        if (!endermitesEnabled) {
            return false;
        }
        if (!(chance > 0.0D)) {
            return false;
        }
        if (chance >= 1.0D) {
            return true;
        }
        final double roll = random.getAsDouble();
        return roll >= 0.0D && roll < chance;
    }

    private static boolean isUsable(final Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        final double x = location.getX();
        final double y = location.getY();
        final double z = location.getZ();
        return !Double.isNaN(x) && !Double.isInfinite(x)
                && !Double.isNaN(y) && !Double.isInfinite(y)
                && !Double.isNaN(z) && !Double.isInfinite(z);
    }

    private static final class RecentPearlImpact {
        private final UUID worldId;
        private final long chunkKey;
        private final double x;
        private final double y;
        private final double z;
        private final long expiresAtNanos;

        private RecentPearlImpact(final UUID worldId,
                                  final long chunkKey,
                                  final double x,
                                  final double y,
                                  final double z,
                                  final long expiresAtNanos) {
            this.worldId = worldId;
            this.chunkKey = chunkKey;
            this.x = x;
            this.y = y;
            this.z = z;
            this.expiresAtNanos = expiresAtNanos;
        }
    }
}
