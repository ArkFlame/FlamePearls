package com.arkflame.flamepearls.compat.cooldown;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class ReflectivePearlCooldownBridge implements PearlCooldownBridge {
    private final Logger logger;
    private final Method method;
    private final AtomicBoolean failed = new AtomicBoolean(false);

    public ReflectivePearlCooldownBridge(final Logger logger, final Method method) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.method = Objects.requireNonNull(method, "method");
    }

    @Override
    public boolean isSupported() {
        return !failed.get();
    }

    @Override
    public boolean setEnderPearlCooldown(final Player player, final int ticks) {
        if (!isSupported() || player == null) {
            return false;
        }
        try {
            method.invoke(player, Material.ENDER_PEARL, Integer.valueOf(Math.max(0, ticks)));
            return true;
        } catch (final Exception exception) {
            if (failed.compareAndSet(false, true)) {
                logger.log(Level.WARNING, "Failed to invoke ender pearl cooldown capability; disabling it.", exception);
            }
            return false;
        }
    }
}
