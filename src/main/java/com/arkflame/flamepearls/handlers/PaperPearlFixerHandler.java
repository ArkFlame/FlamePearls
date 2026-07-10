package com.arkflame.flamepearls.handlers;

import com.arkflame.flamepearls.FlamePearls;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.services.PearlTeleportOutcome;
import com.arkflame.flamepearls.services.PearlTeleportService;
import com.arkflame.flamepearls.utils.ProjectileHitEventSupport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;

public final class PaperPearlFixerHandler extends PearlFixerHandler {
    public PaperPearlFixerHandler(final OriginManager originManager,
                                  final GeneralConfigHolder generalConfigHolder,
                                  final PearlTeleportService pearlTeleportService) {
        super(originManager, generalConfigHolder, pearlTeleportService);
    }

    @Override
    protected void onPearlLaunch(final Projectile projectile, final Player player, final Location origin) {
        // Paper and classic Bukkit use the hit event when it can cancel the vanilla pearl action.
    }

    @Override
    protected void onPearlHit(final ProjectileHitEvent event, final Projectile projectile, final Player player) {
        if (!ProjectileHitEventSupport.canCancel()) {
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

        final Location impactLocation = projectile.getLocation().clone();
        final PearlTeleportOutcome outcome = pearlTeleportService.handleImpact(
                player,
                projectile,
                origin,
                impactLocation,
                false
        );

        if (outcome == PearlTeleportOutcome.BLOCKED || outcome == PearlTeleportOutcome.TELEPORTED) {
            ProjectileHitEventSupport.setCancelled(event, true);
            projectile.remove();
        }
    }
}