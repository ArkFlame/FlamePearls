package com.arkflame.flamepearls.services;

import com.arkflame.flamepearls.compat.endermite.EndermiteSpawnBridge;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EndermiteSpawnServiceTest {
    private static final long RECENT_IMPACT_TTL_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final int MAX_RECENT_IMPACTS = 4096;
    private static final UUID WORLD_A_UID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID WORLD_B_UID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    public void fallbackRejectedWhenEndermitesDisabled() {
        assertFalse(EndermiteSpawnService.shouldAttemptFallback(false, 1.0D, () -> 0.5D));
    }

    @Test
    public void disabledFallbackDoesNotReadRandom() {
        final AtomicInteger randomReads = new AtomicInteger();
        EndermiteSpawnService.shouldAttemptFallback(false, 0.5D, () -> {
            randomReads.incrementAndGet();
            return 0.5D;
        });
        assertEquals(0, randomReads.get());
    }

    @Test
    public void zeroChanceDoesNotReadRandom() {
        final AtomicInteger randomReads = new AtomicInteger();
        EndermiteSpawnService.shouldAttemptFallback(true, 0.0D, () -> {
            randomReads.incrementAndGet();
            return 0.5D;
        });
        assertEquals(0, randomReads.get());
    }

    @Test
    public void fullChanceAcceptedWithoutRandom() {
        assertTrue(EndermiteSpawnService.shouldAttemptFallback(true, 1.0D, () -> {
            throw new AssertionError("random must not be read at full chance");
        }));
    }

    @Test
    public void fallbackUsesStrictChanceBoundary() {
        assertFalse(EndermiteSpawnService.shouldAttemptFallback(true, 0.05D, () -> 0.05D));
    }

    @Test
    public void fallbackAcceptedBelowChance() {
        assertTrue(EndermiteSpawnService.shouldAttemptFallback(true, 0.05D, () -> 0.049999D));
    }

    @Test
    public void recentImpactMatchesNearbySameWorldSameChunk() {
        final World worldA = worldWithUid(WORLD_A_UID);
        final EndermiteSpawnService service = newService(() -> 0.0D, new AtomicLong(0L)::get);

        service.recordRecentImpact(new Location(worldA, 10.0D, 64.0D, 10.0D));

        assertTrue(service.consumeRecentImpactNear(new Location(worldA, 10.1D, 64.0D, 10.1D)));
    }

    @Test
    public void recentImpactRejectsDifferentWorld() {
        final World worldA = worldWithUid(WORLD_A_UID);
        final World worldB = worldWithUid(WORLD_B_UID);
        final EndermiteSpawnService service = newService(() -> 0.0D, new AtomicLong(0L)::get);

        service.recordRecentImpact(new Location(worldA, 10.0D, 64.0D, 10.0D));

        assertFalse(service.consumeRecentImpactNear(new Location(worldB, 10.0D, 64.0D, 10.0D)));
    }

    @Test
    public void recentImpactRejectsAdjacentChunkEvenWhenNearby() {
        final World worldA = worldWithUid(WORLD_A_UID);
        final EndermiteSpawnService service = newService(() -> 0.0D, new AtomicLong(0L)::get);

        service.recordRecentImpact(new Location(worldA, 15.9D, 64.0D, 10.0D));

        assertFalse(service.consumeRecentImpactNear(new Location(worldA, 16.0D, 64.0D, 10.0D)));
    }

    @Test
    public void recentImpactRejectsFarLocationInSameChunk() {
        final World worldA = worldWithUid(WORLD_A_UID);
        final EndermiteSpawnService service = newService(() -> 0.0D, new AtomicLong(0L)::get);

        service.recordRecentImpact(new Location(worldA, 1.0D, 64.0D, 1.0D));

        assertFalse(service.consumeRecentImpactNear(new Location(worldA, 10.0D, 64.0D, 10.0D)));
    }

    @Test
    public void recentImpactExpiresWithoutSleeping() {
        final AtomicLong clock = new AtomicLong(1_000_000L);
        final EndermiteSpawnService service = newService(() -> 0.0D, clock::get);
        final Location location = new Location(worldWithUid(WORLD_A_UID), 10.0D, 64.0D, 10.0D);

        service.recordRecentImpact(location);
        clock.set(clock.get() + RECENT_IMPACT_TTL_NANOS + 1L);

        assertFalse(service.consumeRecentImpactNear(location));
    }

    @Test
    public void recentImpactIsConsumedOnce() {
        final World worldA = worldWithUid(WORLD_A_UID);
        final EndermiteSpawnService service = newService(() -> 0.0D, new AtomicLong(0L)::get);
        final Location location = new Location(worldA, 10.0D, 64.0D, 10.0D);

        service.recordRecentImpact(location);

        assertTrue(service.consumeRecentImpactNear(location));
    }

    @Test
    public void consumedImpactCannotMatchAgain() {
        final World worldA = worldWithUid(WORLD_A_UID);
        final EndermiteSpawnService service = newService(() -> 0.0D, new AtomicLong(0L)::get);
        final Location location = new Location(worldA, 10.0D, 64.0D, 10.0D);

        service.recordRecentImpact(location);
        assertTrue(service.consumeRecentImpactNear(location));

        assertFalse(service.consumeRecentImpactNear(location));
    }

    @Test
    public void negativeCoordinatesUseCorrectChunkPacking() {
        final World worldA = worldWithUid(WORLD_A_UID);
        final EndermiteSpawnService service = newService(() -> 0.0D, new AtomicLong(0L)::get);

        service.recordRecentImpact(new Location(worldA, -0.1D, 64.0D, -0.1D));

        assertTrue(service.consumeRecentImpactNear(new Location(worldA, -0.2D, 64.0D, -0.2D)));
    }

    @Test
    public void newerSameChunkImpactSurvivesOldRingEviction() {
        final World worldA = worldWithUid(WORLD_A_UID);
        final EndermiteSpawnService service = newService(() -> 0.0D, new AtomicLong(0L)::get);
        final Location impactA = new Location(worldA, 1.0D, 64.0D, 1.0D);
        final Location impactB = new Location(worldA, 1.1D, 64.0D, 1.1D);

        service.recordRecentImpact(impactA);
        service.recordRecentImpact(impactB);

        for (int chunkX = 1; chunkX < MAX_RECENT_IMPACTS; chunkX++) {
            service.recordRecentImpact(new Location(worldA, chunkX * 16, 64.0D, 0.0D));
        }

        assertTrue(service.consumeRecentImpactNear(impactB));
    }

    @Test
    public void packedChunkKeyDistinguishesSignedCoordinates() {
        final Set<Long> keys = new HashSet<>();
        keys.add(EndermiteSpawnService.packChunkKey(0, 0));
        keys.add(EndermiteSpawnService.packChunkKey(1, 0));
        keys.add(EndermiteSpawnService.packChunkKey(-1, 0));
        keys.add(EndermiteSpawnService.packChunkKey(0, 1));
        keys.add(EndermiteSpawnService.packChunkKey(0, -1));
        keys.add(EndermiteSpawnService.packChunkKey(Integer.MAX_VALUE, Integer.MIN_VALUE));
        keys.add(EndermiteSpawnService.packChunkKey(Integer.MIN_VALUE, Integer.MAX_VALUE));

        assertEquals(7, keys.size());
    }

    private static EndermiteSpawnService newService(final DoubleSupplier random,
                                                    final LongSupplier nanoClock) {
        return new EndermiteSpawnService(
                new GeneralConfigHolder(),
                new EndermiteSpawnBridge() {
                    @Override
                    public boolean spawn(final Location location) {
                        return true;
                    }

                    @Override
                    public boolean preservesEnderPearlReason() {
                        return true;
                    }
                },
                random,
                nanoClock);
    }

    private static World worldWithUid(final UUID uid) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> method.getName().equals("getUID") ? uid : null);
    }
}
