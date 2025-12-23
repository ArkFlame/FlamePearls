package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.managers.TeleportDataManager;

import java.util.Collection;

import com.arkflame.flamepearls.utils.FoliaAPI;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
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
import com.arkflame.flamepearls.utils.Sounds;

public class ProjectileHitListener implements Listener {
    private OriginManager originManager;
    private TeleportDataManager teleportDataManager;
    private GeneralConfigHolder generalConfigHolder;
    private double endermiteChance;

    public ProjectileHitListener(TeleportDataManager teleportDataManager, OriginManager originManager, GeneralConfigHolder generalConfigHolder) {
        this.originManager = originManager;
        this.teleportDataManager = teleportDataManager;
        this.generalConfigHolder = generalConfigHolder;
        this.endermiteChance = generalConfigHolder.getEndermiteChance();
    }

    @EventHandler
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
                    FoliaAPI.teleportPlayer(player, safeLocation.setDirection(player.getLocation().getDirection()), TeleportCause.ENDER_PEARL);
                    originManager.setAsTeleported(player);
                    if (generalConfigHolder.isResetFallDamageAfterTeleport()) {
                        player.setFallDistance(0);
                    }
                    // Dealing damage to the player as done in vanilla when teleporting.
                    double damage = generalConfigHolder.getPearlDamageSelf();
                    if (damage >= 0) {
                        player.damage(damage, projectile);
                    }
                    // Spawn endermite if chance is higher
                    if (endermiteChance > Math.random()) {
                        final Location spawnLoc = projectile.getLocation();
                        FoliaAPI.runTaskForEntity(
                                projectile, () -> {
                                    World spawnWorld = spawnLoc.getWorld();
                                    if (spawnWorld != null) {
                                        spawnWorld.spawnEntity(spawnLoc, EntityType.ENDERMITE);
                                    }
                                }, () -> {}, 1L
                        );
                    }
                    Sounds.play(player.getLocation(), 1.0f, 1.0f, generalConfigHolder.getPearlSounds());
                } else {
                    FlamePearls.getInstance().getLogger().severe("Error while teleporting player with enderpearl. Origin should not be null. ¿Caused by another plugin?");
                }
            }
        }
    }
}
