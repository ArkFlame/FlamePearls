package com.arkflame.flamepearls.services;

import com.arkflame.flamepearls.FlamePearls;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.config.MessagesConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.managers.TeleportDataManager;
import com.arkflame.flamepearls.utils.FoliaAPI;
import com.arkflame.flamepearls.utils.LocationUtil;
import com.arkflame.flamepearls.utils.MessageUtil;
import com.arkflame.flamepearls.utils.Players;
import com.arkflame.flamepearls.utils.Sounds;
import com.arkflame.flamepearls.utils.WorldUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

import java.util.Objects;

public final class PearlTeleportService {
    private final TeleportDataManager teleportDataManager;
    private final OriginManager originManager;
    private final GeneralConfigHolder generalConfigHolder;
    private final MessagesConfigHolder messagesConfigHolder;

    public PearlTeleportService(final TeleportDataManager teleportDataManager,
                                final OriginManager originManager,
                                final GeneralConfigHolder generalConfigHolder,
                                final MessagesConfigHolder messagesConfigHolder) {
        this.teleportDataManager = Objects.requireNonNull(teleportDataManager, "teleportDataManager");
        this.originManager = Objects.requireNonNull(originManager, "originManager");
        this.generalConfigHolder = Objects.requireNonNull(generalConfigHolder, "generalConfigHolder");
        this.messagesConfigHolder = Objects.requireNonNull(messagesConfigHolder, "messagesConfigHolder");
    }

    public PearlTeleportOutcome handleImpact(final Player player,
                                             final Projectile projectile,
                                             final Location origin,
                                             final Location impactLocation,
                                             final boolean preCollision) {
        if (player == null || projectile == null || origin == null || impactLocation == null) {
            return PearlTeleportOutcome.IGNORED;
        }

        final World world = impactLocation.getWorld();
        if (world == null || generalConfigHolder.isWorldDisabled(world.getName())) {
            return PearlTeleportOutcome.IGNORED;
        }

        final Location playerPos = player.getLocation();
        final World playerWorld = playerPos.getWorld();

        if (generalConfigHolder.isPreventWorldSwitchTeleport() && WorldUtil.isDifferentWorld(origin, impactLocation)) {
            originManager.markBlockedWorldSwitch(player, origin, impactLocation);
            MessageUtil.sendWorldSwitchBlocked(player, messagesConfigHolder, origin, impactLocation);
            return PearlTeleportOutcome.BLOCKED;
        }

        final FileConfiguration config = FlamePearls.getInstance().getConfig();
        final double maxDistance = generalConfigHolder.getMaxTeleportDistance();
        if (maxDistance > 0.0D && playerWorld != null && WorldUtil.isSameWorld(playerWorld, world)) {
            final double distance = playerPos.distance(impactLocation);
            if (distance > maxDistance) {
                final String template = config.getString(
                        "messages.teleport-distance-exceeded",
                        "&cTeleport blocked: distance &e{distance}&c > &e{limit}"
                );
                if (template != null && !template.isEmpty()) {
                    final String filled = template
                            .replace("{distance}", String.valueOf(Math.round(distance)))
                            .replace("{limit}", String.valueOf(Math.round(maxDistance)));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', filled));
                }
                if (FoliaAPI.isFolia()) {
                    FoliaAPI.teleportPlayer(player, playerPos, true, 2L);
                }
                return PearlTeleportOutcome.BLOCKED;
            }
        }

        final Location safeLocation = LocationUtil.findSafeLocation(player, impactLocation, origin, world);
        if (safeLocation == null) {
            return PearlTeleportOutcome.IGNORED;
        }

        if (generalConfigHolder.isPreventWorldSwitchTeleport() && WorldUtil.isDifferentWorld(origin, safeLocation)) {
            originManager.markBlockedWorldSwitch(player, origin, safeLocation);
            MessageUtil.sendWorldSwitchBlocked(player, messagesConfigHolder, origin, safeLocation);
            return PearlTeleportOutcome.BLOCKED;
        }

        teleportDataManager.add(player);
        final Vector originalVelocityLocation = player.getVelocity();
        final boolean gliding = Players.isGliding(player);

        final Location teleportTarget = safeLocation;
        final Runnable directionRefresh = () -> teleportTarget.setDirection(player.getLocation().getDirection());
        if (FoliaAPI.isFolia() && preCollision) {
            FoliaAPI.teleportPlayerNow(player, teleportTarget, TeleportCause.ENDER_PEARL, directionRefresh);
        } else {
            final long teleportDelay = FoliaAPI.isFolia() ? 2L : 0L;
            FoliaAPI.teleportPlayer(
                    player,
                    teleportTarget,
                    TeleportCause.ENDER_PEARL,
                    teleportDelay,
                    directionRefresh
            );
        }

        if (!generalConfigHolder.isResetVelocityAfterTeleport()) {
            player.setVelocity(originalVelocityLocation);
        }
        if (generalConfigHolder.isResetFallDamageAfterTeleport()) {
            player.setFallDistance(0.0F);
            Players.setGliding(player, gliding);
        }

        final double damage = generalConfigHolder.getPearlDamageSelf();
        if (damage >= 0.0D) {
            player.damage(damage, projectile);
        }

        if (generalConfigHolder.isEndermitesEnabled() && generalConfigHolder.getEndermiteChance() > Math.random()) {
            final Location spawnLoc = projectile.getLocation().clone();
            FoliaAPI.runTaskForRegion(spawnLoc, () -> {
                final World spawnWorld = spawnLoc.getWorld();
                if (spawnWorld != null) {
                    spawnWorld.spawnEntity(spawnLoc, EntityType.ENDERMITE);
                }
            });
        }

        Sounds.play(player.getLocation(), 1.0F, 1.0F, generalConfigHolder.getPearlSounds());

        return PearlTeleportOutcome.TELEPORTED;
    }
}
