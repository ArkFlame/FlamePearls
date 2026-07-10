package com.arkflame.flamepearls.handlers;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.services.PearlTeleportService;
import org.bukkit.Location;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Objects;

public abstract class PearlFixerHandler {
    protected final OriginManager originManager;
    protected final GeneralConfigHolder generalConfigHolder;
    protected final PearlTeleportService pearlTeleportService;

    protected PearlFixerHandler(final OriginManager originManager,
                                final GeneralConfigHolder generalConfigHolder,
                                final PearlTeleportService pearlTeleportService) {
        this.originManager = Objects.requireNonNull(originManager, "originManager");
        this.generalConfigHolder = Objects.requireNonNull(generalConfigHolder, "generalConfigHolder");
        this.pearlTeleportService = Objects.requireNonNull(pearlTeleportService, "pearlTeleportService");
    }

    public final void onProjectileLaunch(final ProjectileLaunchEvent event) {
        if (event == null) {
            return;
        }
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

        onPearlLaunch(projectile, player, origin);
    }

    public final void onProjectileHit(final ProjectileHitEvent event) {
        if (event == null) {
            return;
        }
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

        onPearlHit(event, projectile, (Player) shooter);
    }

    protected abstract void onPearlLaunch(Projectile projectile, Player player, Location origin);

    protected abstract void onPearlHit(ProjectileHitEvent event, Projectile projectile, Player player);
}