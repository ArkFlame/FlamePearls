package com.arkflame.flamepearls.compat.cooldown;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;

public final class PearlCooldownBridgeFactory {
    private PearlCooldownBridgeFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static PearlCooldownBridge create(final Logger logger) {
        Objects.requireNonNull(logger, "logger");
        try {
            final Method method = HumanEntity.class.getMethod(
                    "setCooldown",
                    Material.class,
                    Integer.TYPE
            );
            return new ReflectivePearlCooldownBridge(logger, method);
        } catch (final NoSuchMethodException ignored) {
            return new UnsupportedPearlCooldownBridge();
        } catch (final SecurityException exception) {
            logger.log(Level.WARNING, "Unable to inspect ender pearl cooldown capability; disabling it.", exception);
            return new UnsupportedPearlCooldownBridge();
        }
    }
}
