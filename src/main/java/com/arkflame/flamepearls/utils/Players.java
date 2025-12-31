package com.arkflame.flamepearls.utils;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class Players {

    private static final Method IS_GLIDING;
    private static final Method SET_GLIDING;

    static {
        Method isGliding = null;
        Method setGliding = null;

        try {
            // Exists since 1.9+
            isGliding = Player.class.getMethod("isGliding");
            setGliding = Player.class.getMethod("setGliding", boolean.class);
        } catch (NoSuchMethodException ignored) {
            // 1.8 or custom API → intentionally ignored
        }

        IS_GLIDING = isGliding;
        SET_GLIDING = setGliding;
    }

    private Players() {
        // utility
    }

    /**
     * Vanilla behavior:
     * - 1.8: Elytra does not exist → always false
     * - 1.9+: delegates to Player#isGliding
     */
    public static boolean isGliding(Player player) {
        if (IS_GLIDING == null) {
            return false;
        }
        try {
            return (boolean) IS_GLIDING.invoke(player);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Vanilla behavior:
     * - 1.8: no-op
     * - 1.9+: delegates to Player#setGliding
     */
    public static void setGliding(Player player, boolean gliding) {
        if (SET_GLIDING == null) {
            return;
        }
        try {
            SET_GLIDING.invoke(player, gliding);
        } catch (Throwable ignored) {
        }
    }
}
