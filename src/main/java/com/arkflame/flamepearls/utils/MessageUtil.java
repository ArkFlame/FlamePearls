package com.arkflame.flamepearls.utils;

import org.bukkit.entity.Player;

public class MessageUtil {
    public static void sendMessage(Player player, String message) {
        if (message.isEmpty()) {
            return;
        }

        player.sendMessage(message);
    }
}
