package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.utils.FoliaAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Instantiates a mirrored approach of PlayerTeleportEvent handling for ender pearls.
 * - On non-Folia servers: handles the actual PlayerTeleportEvent.
 * - On Folia: emulates the same intent using a periodic scheduler
 *   by finalizing pending pearl teleports (setAsTeleported) on the player's entity thread.
 */
public class PlayerTeleportListener implements Listener {
    private final OriginManager originManager;
    private final GeneralConfigHolder generalConfigHolder;

    public PlayerTeleportListener(OriginManager originManager, GeneralConfigHolder generalConfigHolder) {
        this.originManager = originManager;
        this.generalConfigHolder = generalConfigHolder;

        if (FoliaAPI.isFolia()) {
            FoliaAPI.runTaskTimer(obj -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    FoliaAPI.runTaskForEntity(player, () -> {
                        if (originManager.canTeleport(player)) {
                            originManager.setAsTeleported(player);
                        }
                    }, () -> {}, 1L);
                }
            }, 1L, 10L);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Only process on non-Folia; Folia emulates this with the periodic scheduler above.
        if (FoliaAPI.isFolia()) {
            return;
        }

        if (event.getCause() == TeleportCause.ENDER_PEARL) {
            // Get the player
            Player player = event.getPlayer();
            // Get the destination location and world
            Location to = event.getTo();
            if (to == null) {
                return;
            }

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
                // Event-based finalization of a valid pearl teleport
                originManager.setAsTeleported(player);
                return;
            }

            // Reference event then cancel
            event.setTo(event.getFrom());
            event.setCancelled(true);
        }
    }
}