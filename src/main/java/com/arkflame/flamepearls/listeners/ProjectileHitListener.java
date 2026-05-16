package com.arkflame.flamepearls.listeners;

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
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public class ProjectileHitListener implements Listener {
    private final OriginManager originManager;
    private final TeleportDataManager teleportDataManager;
    private final GeneralConfigHolder generalConfigHolder;
    private final MessagesConfigHolder messagesConfigHolder;
    private final double endermiteChance;

    public ProjectileHitListener(final TeleportDataManager teleportDataManager,
                                 final OriginManager originManager,
                                 final GeneralConfigHolder generalConfigHolder,
                                 final MessagesConfigHolder messagesConfigHolder) {
        this.originManager = originManager;
        this.teleportDataManager = teleportDataManager;
        this.generalConfigHolder = generalConfigHolder;
        this.messagesConfigHolder = messagesConfigHolder;
        this.endermiteChance = generalConfigHolder.getEndermiteChance();
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

        final Player player = (Player) shooter;
        final Location origin = originManager.getOriginAndRemove(projectile);
        if (origin == null) {
            FlamePearls.getInstance().getLogger().severe(
                    "Error while teleporting player with enderpearl. Origin should not be null. Caused by another plugin?"
            );
            return;
        }

        final Location location = projectile.getLocation();
        final World world = location.getWorld();
        if (world == null || generalConfigHolder.isWorldDisabled(world.getName())) {
            return;
        }

        final Location playerPos = player.getLocation();
        final World playerWorld = playerPos.getWorld();

        if (generalConfigHolder.isPreventWorldSwitchTeleport() && WorldUtil.isDifferentWorld(origin, location)) {
            originManager.markBlockedWorldSwitch(player, origin, location);
            MessageUtil.sendWorldSwitchBlocked(player, messagesConfigHolder, origin, location);
            event.setCancelled(true);
            return;
        }

        final FileConfiguration config = FlamePearls.getInstance().getConfig();
        final double maxDistance = generalConfigHolder.getMaxTeleportDistance();
        if (maxDistance > 0.0D && playerWorld != null && WorldUtil.isSameWorld(playerWorld, world)) {
            final double distance = playerPos.distance(location);
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
                event.setCancelled(true);
                return;
            }
        }

        final Location safeLocation = LocationUtil.findSafeLocation(player, location, origin, world);
        if (safeLocation == null) {
            return;
        }

        if (generalConfigHolder.isPreventWorldSwitchTeleport() && WorldUtil.isDifferentWorld(origin, safeLocation)) {
            originManager.markBlockedWorldSwitch(player, origin, safeLocation);
            MessageUtil.sendWorldSwitchBlocked(player, messagesConfigHolder, origin, safeLocation);
            event.setCancelled(true);
            return;
        }

        teleportDataManager.add(player);
        final Vector originalVelocityLocation = player.getVelocity();
        final boolean gliding = Players.isGliding(player);
        final Vector direction = player.getLocation().getDirection();

        FoliaAPI.teleportPlayer(
                player,
                safeLocation.setDirection(direction),
                TeleportCause.ENDER_PEARL,
                FoliaAPI.isFolia() ? 2L : 0L
        );

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

        if (endermiteChance > Math.random()) {
            final Location spawnLoc = projectile.getLocation();
            if (!FoliaAPI.isFolia()) {
                FoliaAPI.runTaskForEntity(projectile, () -> {
                    final World spawnWorld = spawnLoc.getWorld();
                    if (spawnWorld != null) {
                        spawnWorld.spawnEntity(spawnLoc, EntityType.ENDERMITE);
                    }
                }, () -> {
                }, 1L);
            }
        }

        Sounds.play(player.getLocation(), 1.0F, 1.0F, generalConfigHolder.getPearlSounds());
        event.setCancelled(false);
    }
}