package com.arkflame.flamepearls.utils;

import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.ProjectileHitEvent;

public final class ProjectileHitEventSupport {
    private static final boolean CANCELLABLE = Cancellable.class.isAssignableFrom(ProjectileHitEvent.class);

    private ProjectileHitEventSupport() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean canCancel() {
        return CANCELLABLE;
    }

    public static boolean setCancelled(final ProjectileHitEvent event, final boolean cancelled) {
        if (!CANCELLABLE || !(event instanceof Cancellable)) {
            return false;
        }
        ((Cancellable) event).setCancelled(cancelled);
        return true;
    }
}
