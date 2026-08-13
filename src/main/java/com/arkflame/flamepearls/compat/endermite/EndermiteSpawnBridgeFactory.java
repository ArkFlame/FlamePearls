package com.arkflame.flamepearls.compat.endermite;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class EndermiteSpawnBridgeFactory {
    private EndermiteSpawnBridgeFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static EndermiteSpawnBridge create(final Logger logger) {
        Objects.requireNonNull(logger, "logger");
        final CreatureSpawnEvent.SpawnReason enderPearlReason;
        try {
            enderPearlReason = CreatureSpawnEvent.SpawnReason.valueOf("ENDER_PEARL");
        } catch (final IllegalArgumentException exception) {
            return new LegacyEndermiteSpawnBridge();
        }
        try {
            final Method method = World.class.getMethod(
                    "spawn",
                    Location.class,
                    Class.class,
                    CreatureSpawnEvent.SpawnReason.class,
                    Boolean.TYPE,
                    Consumer.class
            );
            return new ReflectiveEndermiteSpawnBridge(logger, method, enderPearlReason);
        } catch (final NoSuchMethodException exception) {
            return new LegacyEndermiteSpawnBridge();
        } catch (final SecurityException exception) {
            logger.log(Level.WARNING, "Unable to inspect endermite spawn capability; falling back.", exception);
            return new LegacyEndermiteSpawnBridge();
        }
    }
}
