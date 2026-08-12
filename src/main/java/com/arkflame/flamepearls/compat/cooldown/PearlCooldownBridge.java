package com.arkflame.flamepearls.compat.cooldown;

import org.bukkit.entity.Player;

public interface PearlCooldownBridge {
    boolean isSupported();

    boolean setEnderPearlCooldown(Player player, int ticks);
}
