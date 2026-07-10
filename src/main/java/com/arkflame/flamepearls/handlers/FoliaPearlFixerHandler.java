package com.arkflame.flamepearls.handlers;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.services.PearlTeleportService;
import com.arkflame.flamepearls.tasks.FoliaPearlPreCollisionTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;

public final class FoliaPearlFixerHandler extends PearlFixerHandler {
    public FoliaPearlFixerHandler(final OriginManager originManager,
                                  final GeneralConfigHolder generalConfigHolder,
                                  final PearlTeleportService pearlTeleportService) {
        super(originManager, generalConfigHolder, pearlTeleportService);
    }

    @Override
    protected void onPearlLaunch(final Projectile projectile, final Player player, final Location origin) {
        new FoliaPearlPreCollisionTask(
                originManager,
                generalConfigHolder,
                pearlTeleportService,
                projectile,
                player
        ).start();
    }

    @Override
    protected void onPearlHit(final ProjectileHitEvent event, final Projectile projectile, final Player player) {
        originManager.removeProjectile(projectile);
    }
}