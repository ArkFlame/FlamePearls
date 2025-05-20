package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.managers.TeleportDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.arkflame.flamepearls.FlamePearls;
import com.arkflame.flamepearls.managers.CooldownManager;

public class PlayerQuitListener implements Listener {
    private TeleportDataManager teleportDataManager;
    private CooldownManager cooldownManager;

    public PlayerQuitListener(TeleportDataManager teleportDataManager, CooldownManager cooldownManager) {
        this.teleportDataManager = teleportDataManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldownManager.resetCooldown(event.getPlayer());
        teleportDataManager.remove(event.getPlayer());
        FlamePearls.getInstance().getGeneralConfigHolder().removeCooldown(event.getPlayer());
    }
}
