package com.arkflame.flamepearls.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import com.arkflame.flamepearls.config.GeneralConfigHolder;

// Stores and manages the last times pearls were thrown.
public class CooldownManager {
    // Last time pearl was thrown by a player (Cooldown checks)
    private Map<Player, Long> lastPearlThrows = new ConcurrentHashMap<>();

    // Time between throws
    private int cooldown = 500;
    
    public CooldownManager(GeneralConfigHolder generalConfigHolder) {
        cooldown = (int) (generalConfigHolder.getPearlCooldown() * 1000);
    }
    

    public void updateLastPearl(Player player) {
        lastPearlThrows.put(player, System.currentTimeMillis());
    }

    public double getCooldown(Player player) {
        // Get the time passed since last pearl in milliseconds
        long timeSinceLastPearl = System.currentTimeMillis() - lastPearlThrows.getOrDefault(player, 0L);

        // Return the cooldown minus the time passed and convert to seconds
        return (cooldown - Math.min(cooldown, timeSinceLastPearl)) / 1000D;
    }

    public void resetCooldown(Player player) {
        lastPearlThrows.remove(player);
    }


    public String getFancyCooldown(Player player) {
        double cooldown = getCooldown(player);
        return String.format("%.1f", cooldown);
    }
}
