package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.handlers.PearlFixerHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Objects;

public final class ProjectileHitListener implements Listener {
    private final PearlFixerHandler pearlFixerHandler;

    public ProjectileHitListener(final PearlFixerHandler pearlFixerHandler) {
        this.pearlFixerHandler = Objects.requireNonNull(pearlFixerHandler, "pearlFixerHandler");
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(final ProjectileHitEvent event) {
        pearlFixerHandler.onProjectileHit(event);
    }
}