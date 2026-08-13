package com.arkflame.flamepearls.compat.endermite;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Endermite;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class ReflectiveEndermiteSpawnBridge implements EndermiteSpawnBridge {
    private final Logger logger;
    private final Method spawnMethod;
    private final CreatureSpawnEvent.SpawnReason enderPearlReason;
    private final AtomicBoolean failed = new AtomicBoolean(false);

    public ReflectiveEndermiteSpawnBridge(final Logger logger, final Method spawnMethod,
            final CreatureSpawnEvent.SpawnReason enderPearlReason) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.spawnMethod = Objects.requireNonNull(spawnMethod, "spawnMethod");
        this.enderPearlReason = Objects.requireNonNull(enderPearlReason, "enderPearlReason");
    }

    @Override
    public boolean preservesEnderPearlReason() {
        return !failed.get();
    }

    @Override
    public boolean spawn(final Location location) {
        if (failed.get() || location == null || location.getWorld() == null) {
            return false;
        }
        try {
            final Object spawned = spawnMethod.invoke(
                    location.getWorld(),
                    location,
                    Endermite.class,
                    enderPearlReason,
                    Boolean.TRUE,
                    null
            );
            return spawned != null;
        } catch (final Exception exception) {
            if (failed.compareAndSet(false, true)) {
                logger.log(Level.WARNING, "Failed to spawn endermite with ender pearl reason; falling back.", exception);
            }
            return false;
        }
    }
}
