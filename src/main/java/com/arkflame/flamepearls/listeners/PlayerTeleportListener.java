package com.arkflame.flamepearls.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;

public class PlayerTeleportListener implements Listener {
    private final OriginManager originManager;
    private final GeneralConfigHolder generalConfigHolder;

    public PlayerTeleportListener(OriginManager originManager, GeneralConfigHolder generalConfigHolder) {
        this.originManager = originManager;
        this.generalConfigHolder = generalConfigHolder;
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Check if the teleport cause is ENDER_PEARL
        if (event.getCause() == TeleportCause.ENDER_PEARL) {
            // Get the player
            Player player = event.getPlayer();
            // Get the destination location and world
            Location to = event.getTo();
            World world = to.getWorld();

            // Safety check for the destination world
            if (world == null) {
                return;
            }

            // Check if the destination world is disabled
            if (generalConfigHolder.isWorldDisabled(world.getName())) {
                return;
            }

            // Prevent teleporting outside the world border if configured to do so
            if (generalConfigHolder.isPreventWorldBorderTeleport() && !world.getWorldBorder().isInside(to)) {
                event.setCancelled(true);
                return;
            }
            
            if (originManager.canTeleport(player)) {
                // Teleported
                originManager.setAsTeleported(player);
                return; 
            }
            // Cancel the event
            event.setTo(event.getFrom());
            // Really cancel
            event.setCancelled(true);
        }
    }
}