package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.services.PearlTeleportService;
import com.arkflame.flamepearls.tasks.FoliaPearlPreCollisionTask;
import com.arkflame.flamepearls.utils.FoliaAPI;
import com.arkflame.flamepearls.utils.ProjectileHitEventSupport;
import org.bukkit.Location;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Objects;

public final class ProjectileLaunchListener implements Listener {
    private final OriginManager originManager;
    private final GeneralConfigHolder generalConfigHolder;
    private final PearlTeleportService pearlTeleportService;

    public ProjectileLaunchListener(final OriginManager originManager,
                                    final GeneralConfigHolder generalConfigHolder,
                                    final PearlTeleportService pearlTeleportService) {
        this.originManager = Objects.requireNonNull(originManager, "originManager");
        this.generalConfigHolder = Objects.requireNonNull(generalConfigHolder, "generalConfigHolder");
        this.pearlTeleportService = Objects.requireNonNull(pearlTeleportService, "pearlTeleportService");
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        final Projectile projectile = event.getEntity();
        if (!(projectile instanceof EnderPearl)) {
            return;
        }

        final ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }

        final Player player = (Player) shooter;
        final Location origin = player.getLocation().clone();
        originManager.setOrigin(projectile, origin);

        if (!generalConfigHolder.isPearlTeleportFixEnabled()) {
            return;
        }
        if (!FoliaAPI.isFolia() && ProjectileHitEventSupport.canCancel()) {
            return;
        }

        new FoliaPearlPreCollisionTask(
                originManager,
                generalConfigHolder,
                pearlTeleportService,
                projectile,
                player
        ).start();
    }
}
