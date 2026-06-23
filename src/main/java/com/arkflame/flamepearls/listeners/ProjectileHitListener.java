package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.FlamePearls;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.services.PearlTeleportOutcome;
import com.arkflame.flamepearls.services.PearlTeleportService;
import com.arkflame.flamepearls.utils.FoliaAPI;
import com.arkflame.flamepearls.utils.ProjectileHitEventSupport;
import org.bukkit.Location;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Objects;

public final class ProjectileHitListener implements Listener {
    private final PearlTeleportService pearlTeleportService;
    private final OriginManager originManager;
    private final GeneralConfigHolder generalConfigHolder;

    public ProjectileHitListener(final PearlTeleportService pearlTeleportService,
                                 final OriginManager originManager,
                                 final GeneralConfigHolder generalConfigHolder) {
        this.pearlTeleportService = Objects.requireNonNull(pearlTeleportService, "pearlTeleportService");
        this.originManager = Objects.requireNonNull(originManager, "originManager");
        this.generalConfigHolder = Objects.requireNonNull(generalConfigHolder, "generalConfigHolder");
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(final ProjectileHitEvent event) {
        final Projectile projectile = event.getEntity();
        if (!(projectile instanceof EnderPearl)) {
            return;
        }

        final ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }

        if (!generalConfigHolder.isPearlTeleportFixEnabled()) {
            originManager.removeProjectile(projectile);
            return;
        }
        if (FoliaAPI.isFolia() || !ProjectileHitEventSupport.canCancel()) {
            originManager.removeProjectile(projectile);
            return;
        }

        final Location origin = originManager.getOriginAndRemove(projectile);
        if (origin == null) {
            FlamePearls.getInstance().getLogger().severe(
                    "Error while teleporting player with enderpearl. Origin should not be null. Caused by another plugin?"
            );
            return;
        }

        final PearlTeleportOutcome outcome = pearlTeleportService.handleImpact(
                (Player) shooter,
                projectile,
                origin,
                projectile.getLocation(),
                false
        );

        if (outcome == PearlTeleportOutcome.BLOCKED || outcome == PearlTeleportOutcome.TELEPORTED) {
            ProjectileHitEventSupport.setCancelled(event, true);
            projectile.remove();
        }
    }
}
