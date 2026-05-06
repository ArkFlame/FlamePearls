package com.arkflame.flamepearls.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import com.arkflame.flamepearls.FlamePearls;
import com.arkflame.flamepearls.config.GeneralConfigHolder;

// Stores and manages the last times pearls were thrown.
public class CooldownManager {
    // Last time pearl was thrown by a player (Cooldown checks)
    private Map<Player, Long> lastPearlThrows = new ConcurrentHashMap<>();

    public void updateLastPearl(Player player) {
        if (player == null) {
            return;
        }
        if (!FlamePearls.getInstance().getGeneralConfigHolder().isPearlCooldownEnabled()) {
            return;
        }
        lastPearlThrows.put(player, System.currentTimeMillis());
    }

    public double getCooldown(Player player) {
        if (player == null) {
            return 0.0D;
        }
        GeneralConfigHolder config = FlamePearls.getInstance().getGeneralConfigHolder();
        if (!config.isPearlCooldownEnabled()) {
            return 0.0D;
        }
        long timeSinceLastPearl = System.currentTimeMillis() - lastPearlThrows.getOrDefault(player, 0L);
        double cooldown = config.getPearlCooldown(player) * 1000D;

        return (cooldown - Math.min(cooldown, timeSinceLastPearl)) / 1000D;
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
