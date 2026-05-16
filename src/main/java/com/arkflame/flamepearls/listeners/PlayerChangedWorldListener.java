package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.utils.FoliaAPI;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.Optional;

public class PlayerChangedWorldListener implements Listener {
    private static final long BLOCKED_PEARL_ROLLBACK_WINDOW_MILLIS = 1500L;

    private final OriginManager originManager;

    public PlayerChangedWorldListener(final OriginManager originManager) {
        this.originManager = originManager;
    }

    @EventHandler
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        final World currentWorld = player.getWorld();
        final Optional<Location> rollbackLocation = originManager.consumeBlockedWorldSwitch(
                player,
                event.getFrom(),
                currentWorld,
                BLOCKED_PEARL_ROLLBACK_WINDOW_MILLIS
        );
        if (!rollbackLocation.isPresent()) {
            return;
        }
        FoliaAPI.teleportPlayer(player, rollbackLocation.get(), TeleportCause.PLUGIN, 1L);
    }
}