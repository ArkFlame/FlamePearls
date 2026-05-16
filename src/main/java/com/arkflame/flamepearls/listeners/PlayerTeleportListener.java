package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.config.MessagesConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.utils.MessageUtil;
import com.arkflame.flamepearls.utils.WorldUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class PlayerTeleportListener implements Listener {
    private final OriginManager originManager;
    private final GeneralConfigHolder generalConfigHolder;
    private final MessagesConfigHolder messagesConfigHolder;

    public PlayerTeleportListener(final OriginManager originManager,
                                  final GeneralConfigHolder generalConfigHolder,
                                  final MessagesConfigHolder messagesConfigHolder) {
        this.originManager = originManager;
        this.generalConfigHolder = generalConfigHolder;
        this.messagesConfigHolder = messagesConfigHolder;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if (event.getCause() != TeleportCause.ENDER_PEARL) {
            return;
        }

        final Location from = event.getFrom();
        final Location to = event.getTo();
        if (to == null) {
            return;
        }

        final World toWorld = to.getWorld();
        if (toWorld == null || generalConfigHolder.isWorldDisabled(toWorld.getName())) {
            return;
        }

        final Player player = event.getPlayer();
        if (generalConfigHolder.isPreventWorldSwitchTeleport() && WorldUtil.isDifferentWorld(from, to)) {
            originManager.markBlockedWorldSwitch(player, from, to);
            event.setCancelled(true);
            MessageUtil.sendWorldSwitchBlocked(player, messagesConfigHolder, from, to);
            return;
        }

        if (generalConfigHolder.isPreventWorldBorderTeleport() && !WorldUtil.isInsideWorldBorder(to)) {
            event.setCancelled(true);
        }
    }
}