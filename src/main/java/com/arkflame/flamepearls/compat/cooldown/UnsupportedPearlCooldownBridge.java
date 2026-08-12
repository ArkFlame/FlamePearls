package com.arkflame.flamepearls.compat.cooldown;

import org.bukkit.entity.Player;

public final class UnsupportedPearlCooldownBridge implements PearlCooldownBridge {
    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public boolean setEnderPearlCooldown(final Player player, final int ticks) {
        return false;
    }
}
