package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.handlers.PearlFixerHandler;
import com.arkflame.flamepearls.managers.CooldownManager;
import com.arkflame.flamepearls.utils.FoliaAPI;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.Objects;

public final class ProjectileLaunchListener implements Listener {
    private final PearlFixerHandler pearlFixerHandler;
    private final CooldownManager cooldownManager;

    public ProjectileLaunchListener(final PearlFixerHandler pearlFixerHandler,
                                    final CooldownManager cooldownManager) {
        this.pearlFixerHandler = Objects.requireNonNull(pearlFixerHandler, "pearlFixerHandler");
        this.cooldownManager = Objects.requireNonNull(cooldownManager, "cooldownManager");
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        pearlFixerHandler.onProjectileLaunch(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSuccessfulPearlLaunch(final ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl)
                || !(event.getEntity().getShooter() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getEntity().getShooter();
        FoliaAPI.runTaskForEntity(player,
                () -> cooldownManager.syncModernCooldown(player),
                () -> { },
                1L);
    }
}
