package com.arkflame.flamepearls.services;

import com.arkflame.flamepearls.compat.endermite.EndermiteSpawnBridge;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.utils.FoliaAPI;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

public final class EndermiteSpawnService {
    private static final long RECENT_IMPACT_TTL_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final double RECENT_IMPACT_RADIUS_SQUARED = 0.25D;
    private static final int MAX_RECENT_IMPACTS = 4096;

    private final GeneralConfigHolder generalConfigHolder;
    private final EndermiteSpawnBridge spawnBridge;
    private final DoubleSupplier random;
    private final LongSupplier nanoClock;
    private final boolean enderPearlReasonAvailable;
    private final ConcurrentLinkedDeque<RecentPearlImpact> recentImpacts = new ConcurrentLinkedDeque<>();

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
        if (enderPearlReasonAvailable || event == null) {
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
        final long now = nanoClock.getAsLong();
        final Iterator<RecentPearlImpact> iterator = recentImpacts.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expiresAtNanos <= now) {
                iterator.remove();
            }
        }
        while (recentImpacts.size() >= MAX_RECENT_IMPACTS) {
            recentImpacts.pollFirst();
        }
        recentImpacts.addLast(new RecentPearlImpact(
                location.getWorld().getUID(),
                location.getX(),
                location.getY(),
                location.getZ(),
                now + RECENT_IMPACT_TTL_NANOS
        ));
    }

    boolean consumeRecentImpactNear(final Location location) {
        if (!isUsable(location)) {
            return false;
        }
        final long now = nanoClock.getAsLong();
        final UUID worldId = location.getWorld().getUID();
        final double x = location.getX();
        final double y = location.getY();
        final double z = location.getZ();
        final Iterator<RecentPearlImpact> iterator = recentImpacts.iterator();
        while (iterator.hasNext()) {
            final RecentPearlImpact impact = iterator.next();
            if (impact.expiresAtNanos <= now) {
                iterator.remove();
                continue;
            }
            if (!worldId.equals(impact.worldId)) {
                continue;
            }
            final double dx = x - impact.x;
            final double dy = y - impact.y;
            final double dz = z - impact.z;
            if (dx * dx + dy * dy + dz * dz <= RECENT_IMPACT_RADIUS_SQUARED) {
                if (recentImpacts.remove(impact)) {
                    return true;
                }
            }
        }
        return false;
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
                final World world = spawnLocation.getWorld();
                if (world == null) {
                    return;
                }
                final boolean endermitesEnabled = generalConfigHolder.isEndermitesEnabled();
                final String doMobSpawning = world.getGameRuleValue("doMobSpawning");
                final boolean mobSpawningEnabled = doMobSpawning != null && doMobSpawning.equalsIgnoreCase("true");
                final double chance = generalConfigHolder.getEndermiteChance();
                if (!shouldAttemptFallback(endermitesEnabled, mobSpawningEnabled, chance, random.getAsDouble())) {
                    return;
                }
                if (!spawnBridge.preservesEnderPearlReason()) {
                    recordRecentImpact(spawnLocation);
                }
                spawnBridge.spawn(spawnLocation);
            });
        });
    }

    static boolean shouldAttemptFallback(final boolean endermitesEnabled,
                                         final boolean doMobSpawning,
                                         final double chance,
                                         final double randomValue) {
        return endermitesEnabled && doMobSpawning && chance > 0.0D
                && randomValue >= 0.0D && randomValue < chance;
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
        private final double x;
        private final double y;
        private final double z;
        private final long expiresAtNanos;

        private RecentPearlImpact(final UUID worldId,
                                  final double x,
                                  final double y,
                                  final double z,
                                  final long expiresAtNanos) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.expiresAtNanos = expiresAtNanos;
        }
    }
}
