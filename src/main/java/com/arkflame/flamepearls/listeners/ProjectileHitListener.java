package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.managers.TeleportDataManager;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.projectiles.ProjectileSource;

import com.arkflame.flamepearls.FlamePearls;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.utils.LocationUtil;

public class ProjectileHitListener implements Listener {
    private OriginManager originManager;
    private TeleportDataManager teleportDataManager;
    private GeneralConfigHolder generalConfigHolder;
    private Sound sound;
    private double endermiteChance;

    public ProjectileHitListener(TeleportDataManager teleportDataManager, OriginManager originManager, GeneralConfigHolder generalConfigHolder) {
        this.originManager = originManager;
        this.teleportDataManager = teleportDataManager;
        this.generalConfigHolder = generalConfigHolder;
        this.sound = generalConfigHolder.getPearlSound();
        this.endermiteChance = generalConfigHolder.getEndermiteChance();
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        // Check if the projectile is an ender pearl
        if (projectile instanceof EnderPearl) {
            // Get shooter
            ProjectileSource shooter = projectile.getShooter();

            if (shooter instanceof Player) {
                // Cast the shooter of the pearl
                Player player = (Player) shooter;
                // Get the location where the pearl was thrown from
                Location origin = originManager.getOriginAndRemove(projectile);

                if (origin != null) {
                    // Get the location where the pearl landed
                    Location location = projectile.getLocation();
                    // Get the world of the location
                    World world = location.getWorld();
                    // Get disabled worlds
                    Collection<String> disabledWorlds = generalConfigHolder.getDisabledWorlds();
                    // Teleport the player to that location if not disabled
                    if (disabledWorlds.contains(world.getName())) {
                        return;
                    }
                    // Try to find the nearest safest position
                    Location safeLocation = LocationUtil.findSafeLocation(player, location, origin, world);
                    // Will teleport
                    originManager.setAsWillTeleport(player);
                    teleportDataManager.add(player);
                    player.teleport(safeLocation.setDirection(player.getLocation().getDirection()), TeleportCause.ENDER_PEARL);
                    if (generalConfigHolder.isResetFallDamageAfterTeleport()) {
                        player.setFallDistance(0);
                    }
                    // Dealing damage to the player as done in vanilla when teleporting.
                    double damage = generalConfigHolder.getPearlDamageSelf();
                    if(damage >= 0) {
                        player.damage(damage, projectile);
                    }
                    // Spawn endermite if chance is higher
                    if (endermiteChance > Math.random()) {
                        world.spawnEntity(safeLocation, org.bukkit.entity.EntityType.ENDERMITE);
                    }
                    // Check if sound is defined
                    if (sound != null) {
                        // Play sound
                        world.playSound(safeLocation, sound, 5, 1f);
                    }
                } else {
                    FlamePearls.getInstance().getLogger().severe("Error while teleporting player with enderpearl. Origin should not be null. ¿Caused by another plugin?");
                }
            }
        }
    }
}
