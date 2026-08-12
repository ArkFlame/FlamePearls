package com.arkflame.flamepearls.managers;

import com.arkflame.flamepearls.compat.cooldown.PearlCooldownBridge;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// Stores and manages the last times pearls were thrown.
public class CooldownManager {
    private final GeneralConfigHolder config;
    private final PearlCooldownBridge cooldownBridge;

    // Last time pearl was thrown by a player (Cooldown checks)
    private final ConcurrentHashMap<Player, Long> lastPearlThrows = new ConcurrentHashMap<>();

    public CooldownManager(GeneralConfigHolder config, PearlCooldownBridge cooldownBridge) {
        this.config = Objects.requireNonNull(config, "config");
        this.cooldownBridge = Objects.requireNonNull(cooldownBridge, "cooldownBridge");
    }

    public void updateLastPearl(Player player) {
        if (player == null) {
            return;
        }
        if (!config.isPearlCooldownEnabled()) {
            return;
        }
        lastPearlThrows.put(player, System.currentTimeMillis());
    }

    public double getCooldown(Player player) {
        if (player == null) {
            return 0.0D;
        }
        if (!config.isPearlCooldownEnabled()) {
            return 0.0D;
        }
        long timeSinceLastPearl = System.currentTimeMillis() - lastPearlThrows.getOrDefault(player, 0L);
        double cooldown = config.getPearlCooldown(player) * 1000D;

        return (cooldown - Math.min(cooldown, timeSinceLastPearl)) / 1000D;
    }

    public void syncModernCooldown(Player player) {
        if (player == null
                || !config.isPearlCooldownEnabled()
                || !config.isModernCooldownEnabled()
                || !cooldownBridge.isSupported()
                || config.isWorldDisabled(player.getWorld().getName())) {
            return;
        }
        cooldownBridge.setEnderPearlCooldown(player, toCooldownTicks(getCooldown(player)));
    }

    static int toCooldownTicks(double seconds) {
        if (!(seconds > 0.0D)) {
            return 0;
        }
        final double ticks = Math.ceil(seconds * 20.0D);
        if (Double.isInfinite(ticks) || ticks >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) ticks;
    }

    public void resetCooldown(Player player) {
        if (player == null) {
            return;
        }
        lastPearlThrows.remove(player);
    }

    public String getFancyCooldown(Player player) {
        double cooldown = getCooldown(player);
        return String.format("%.1f", cooldown);
    }
}
