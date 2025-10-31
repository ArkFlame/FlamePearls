package com.arkflame.flamepearls.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.arkflame.flamepearls.FlamePearls;

public class PlayerJoinListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        FlamePearls.runAsync(() -> FlamePearls.getInstance().getGeneralConfigHolder().updateCooldown(event.getPlayer()));
    }
}
