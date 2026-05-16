package com.arkflame.flamepearls.utils;

import com.arkflame.flamepearls.config.MessagesConfigHolder;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class MessageUtil {
    private MessageUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void sendMessage(final Player player, final String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        player.sendMessage(message);
    }

    public static void sendWorldSwitchBlocked(final Player player,
                                              final MessagesConfigHolder messagesConfigHolder,
                                              final Location from,
                                              final Location to) {
        if (messagesConfigHolder == null) {
            return;
        }
        final String message = messagesConfigHolder.getMessage("teleport-world-switch-blocked")
                .replace("{from}", WorldUtil.getWorldName(from))
                .replace("{to}", WorldUtil.getWorldName(to));
        sendMessage(player, message);
    }
}