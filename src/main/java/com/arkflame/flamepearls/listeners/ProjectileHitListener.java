package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.handlers.PearlFixerHandler;
import com.arkflame.flamepearls.services.EndermiteSpawnService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Objects;

public final class ProjectileHitListener implements Listener {
    private final PearlFixerHandler pearlFixerHandler;
    private final EndermiteSpawnService endermiteSpawnService;

    public ProjectileHitListener(final PearlFixerHandler pearlFixerHandler,
                                 final EndermiteSpawnService endermiteSpawnService) {
        this.pearlFixerHandler = Objects.requireNonNull(pearlFixerHandler, "pearlFixerHandler");
        this.endermiteSpawnService = Objects.requireNonNull(endermiteSpawnService, "endermiteSpawnService");
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(final ProjectileHitEvent event) {
        endermiteSpawnService.recordPotentialNativePearlImpact(event);
        pearlFixerHandler.onProjectileHit(event);
    }
}
