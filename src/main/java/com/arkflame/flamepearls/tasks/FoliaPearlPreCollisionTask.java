package com.arkflame.flamepearls.tasks;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.services.PearlTeleportService;
import com.arkflame.flamepearls.utils.FoliaAPI;
import com.arkflame.flamepearls.utils.pearl.PearlCollisionPredictor;
import com.arkflame.flamepearls.utils.pearl.PredictedPearlCollision;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class FoliaPearlPreCollisionTask {
    private static final int MIN_TICKS_LIVED_BEFORE_CONSUME = 2;
    private static final double ENTITY_CANDIDATE_RADIUS = 0.75D;

    private final OriginManager originManager;
    private final GeneralConfigHolder generalConfigHolder;
    private final PearlTeleportService pearlTeleportService;
    private final Projectile projectile;
    private final Player player;
    private final UUID projectileId;
    private volatile boolean finished;

    public FoliaPearlPreCollisionTask(final OriginManager originManager,
                                      final GeneralConfigHolder generalConfigHolder,
                                      final PearlTeleportService pearlTeleportService,
                                      final Projectile projectile,
                                      final Player player) {
        this.originManager = Objects.requireNonNull(originManager, "originManager");
        this.generalConfigHolder = Objects.requireNonNull(generalConfigHolder, "generalConfigHolder");
        this.pearlTeleportService = Objects.requireNonNull(pearlTeleportService, "pearlTeleportService");
        this.projectile = Objects.requireNonNull(projectile, "projectile");
        this.player = Objects.requireNonNull(player, "player");
        this.projectileId = projectile.getUniqueId();
    }

    public void start() {
        if (!FoliaAPI.isFolia()) {
            return;
        }
        FoliaAPI.runTaskForEntityRepeating(
                projectile,
                scheduledTask -> tick(scheduledTask),
                () -> finishWithoutProjectileRemoval(),
                1L,
                1L
        );
    }

    private void tick(final Object scheduledTask) {
        if (finished) {
            FoliaAPI.cancelScheduledTask(scheduledTask);
            return;
        }
        if (shouldStop()) {
            cancel(scheduledTask);
            return;
        }
        if (!(projectile instanceof EnderPearl)) {
            cancel(scheduledTask);
            return;
        }

        final ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player) || !((Player) shooter).getUniqueId().equals(player.getUniqueId())) {
            cancel(scheduledTask);
            return;
        }

        final Location location;
        final Vector velocity;
        try {
            location = projectile.getLocation().clone();
            velocity = projectile.getVelocity().clone();
        } catch (final Exception exception) {
            cancel(scheduledTask);
            return;
        }

        final World world = location.getWorld();
        if (world == null || generalConfigHolder.isWorldDisabled(world.getName())) {
            cancel(scheduledTask);
            return;
        }
        if (projectile.getTicksLived() < MIN_TICKS_LIVED_BEFORE_CONSUME) {
            return;
        }

        final Optional<PredictedPearlCollision> predictionOptional;
        try {
            predictionOptional = PearlCollisionPredictor.predictNextTick(location, velocity);
        } catch (final Exception exception) {
            cancel(scheduledTask);
            return;
        }
        if (!predictionOptional.isPresent()) {
            return;
        }

        if (hasNearbyEntityCandidate()) {
            originManager.removeProjectile(projectile);
            cancel(scheduledTask);
            return;
        }

        consumeBlockHit(scheduledTask, predictionOptional.get());
    }

    private boolean shouldStop() {
        return projectile.isDead() || !projectile.isValid() || !originManager.containsProjectile(projectile);
    }

    private boolean hasNearbyEntityCandidate() {
        try {
            for (final Entity entity : projectile.getNearbyEntities(
                    ENTITY_CANDIDATE_RADIUS,
                    ENTITY_CANDIDATE_RADIUS,
                    ENTITY_CANDIDATE_RADIUS
            )) {
                if (entity == null || projectileId.equals(entity.getUniqueId()) || player.getUniqueId().equals(entity.getUniqueId())) {
                    continue;
                }
                if (entity.isDead() || !entity.isValid()) {
                    continue;
                }
                return true;
            }
        } catch (final Exception exception) {
            return false;
        }
        return false;
    }

    private void consumeBlockHit(final Object scheduledTask, final PredictedPearlCollision prediction) {
        finished = true;
        final Location origin = originManager.getOriginAndRemove(projectile);
        if (origin == null) {
            cancel(scheduledTask);
            return;
        }

        final Location impactLocation = prediction.getCollisionLocation().clone();
        try {
            projectile.remove();
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
        cancel(scheduledTask);
        FoliaAPI.runTaskForEntity(
                player,
                () -> pearlTeleportService.handleImpact(player, projectile, origin, impactLocation, true),
                () -> {},
                1L
        );
    }

    private void cancel(final Object scheduledTask) {
        finished = true;
        FoliaAPI.cancelScheduledTask(scheduledTask);
    }

    private void finishWithoutProjectileRemoval() {
        finished = true;
        try {
            if (projectile.isDead() || !projectile.isValid()) {
                originManager.removeProjectile(projectile);
            }
        } catch (final Exception exception) {
            // Retired callback can run after entity removal; avoid loading or touching world state here.
        }
    }
}
