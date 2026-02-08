package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.managers.TeleportDataManager;

import java.util.Collection;

import com.arkflame.flamepearls.utils.FoliaAPI;
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

import com.arkflame.flamepearls.FlamePearls;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.utils.LocationUtil;
import com.arkflame.flamepearls.utils.Players;
import com.arkflame.flamepearls.utils.Sounds;

import org.bukkit.ChatColor;

public class ProjectileHitListener implements Listener {
    private OriginManager originManager;
    private TeleportDataManager teleportDataManager;
    private GeneralConfigHolder generalConfigHolder;
    private double endermiteChance;

    public ProjectileHitListener(TeleportDataManager teleportDataManager, OriginManager originManager,
            GeneralConfigHolder generalConfigHolder) {
        this.originManager = originManager;
        this.teleportDataManager = teleportDataManager;
        this.generalConfigHolder = generalConfigHolder;
        this.endermiteChance = generalConfigHolder.getEndermiteChance();
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        // Only interested in ender pearls
        if (projectile instanceof EnderPearl) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                Player player = (Player) shooter;
                // Retrieve the origin of the throw
                Location origin = originManager.getOriginAndRemove(projectile);
                if (origin != null) {
                    Location location = projectile.getLocation();
                    World world = location.getWorld();
                    if (world == null) {
                        return;
                    }
                    Collection<String> disabledWorlds = generalConfigHolder.getDisabledWorlds();
                    // Skip disabled worlds
                    if (disabledWorlds.contains(world.getName())) {
                        return;
                    }
                    Location playerPos = player.getLocation();
                    World playerWorld = playerPos.getWorld();
                    // If world-switching with pearls is prevented, cancel if different world
                    if (generalConfigHolder.isPreventWorldSwitchTeleport()) {
                        if (playerWorld == null || !playerWorld.getName().equals(world.getName())) {
                            String template = FlamePearls.getInstance().getConfig()
                                    .getString("messages.teleport-world-switch-blocked",
                                            "&cYou cannot teleport from world {from} to {to}!");
                            if (template != null && !template.isEmpty()) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', template
                                        .replace("{from}", playerWorld.getName()).replace("{to}", world.getName())));
                            }
                            if (FoliaAPI.isFolia()) {
                                FoliaAPI.teleportPlayer(player, playerPos, true, 2L);
                            }
                            event.setCancelled(true);
                            return;
                        }
                    }

                    FileConfiguration config = FlamePearls.getInstance().getConfig();
                    double maxDistance = generalConfigHolder.getMaxTeleportDistance();
                    if (maxDistance > 0) {
                        if (playerWorld != null && world != null && playerWorld.getName().equals(world.getName())) {
                            double distance = playerPos.distance(location);
                            if (distance > maxDistance) {
                                // Build and send configured distance-exceeded message
                                String template = config
                                        .getString("messages.teleport-distance-exceeded",
                                                "&cTeleport blocked: distance &e{distance}&c > &e{limit}");
                                String filled = template.replace("{distance}", String.valueOf(Math.round(distance)))
                                        .replace("{limit}", String.valueOf(Math.round(maxDistance)));
                                if (template != null && !template.isEmpty()) {
                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', filled));
                                }
                                if (FoliaAPI.isFolia()) {
                                    FoliaAPI.teleportPlayer(player, playerPos, true, 2L);
                                }
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }

                    // Find the safe target location
                    Location safeLocation = LocationUtil.findSafeLocation(player, location, origin, world);
                    if (safeLocation == null) {
                        return;
                    }
                    // Proceed with teleport and effects
                    teleportDataManager.add(player);
                    Vector originalVelocityLocation = player.getVelocity();
                    boolean gliding = Players.isGliding(player);
                    Vector dir = player.getLocation().getDirection();
                    FoliaAPI.teleportPlayer(player, safeLocation.setDirection(dir),
                            TeleportCause.ENDER_PEARL, FoliaAPI.isFolia() ? 2L : 0L);
                    if (!generalConfigHolder.isResetVelocityAfterTeleport()) {
                        player.setVelocity(originalVelocityLocation);
                    }
                    if (generalConfigHolder.isResetFallDamageAfterTeleport()) {
                        player.setFallDistance(0);
                        Players.setGliding(player, gliding);
                    }
                    double damage = generalConfigHolder.getPearlDamageSelf();
                    if (damage >= 0) {
                        player.damage(damage, projectile);
                    }
                    if (endermiteChance > Math.random()) {
                        final Location spawnLoc = projectile.getLocation();
                        if (!FoliaAPI.isFolia()) {
                            FoliaAPI.runTaskForEntity(
                                    projectile, () -> {
                                        World spawnWorld = spawnLoc.getWorld();
                                        if (spawnWorld != null) {
                                            spawnWorld.spawnEntity(spawnLoc, EntityType.ENDERMITE);
                                        }
                                    }, () -> {
                                    }, 1L);
                        }
                    }
                    Sounds.play(player.getLocation(), 1.0f, 1.0f, generalConfigHolder.getPearlSounds());
                    event.setCancelled(false);
                } else {
                    FlamePearls.getInstance().getLogger().severe(
                            "Error while teleporting player with enderpearl. Origin should not be null. ¿Caused by another plugin?");
                }
            }
        }
    }
}