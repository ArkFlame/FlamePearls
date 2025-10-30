package com.arkflame.flamepearls.tasks;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.utils.FoliaAPI;
import org.bukkit.entity.Projectile;

/**
 * A task that runs periodically to clean up ender pearls that have
 * existed for too long or are no longer valid. This prevents pearls
 * from staying loaded in the world indefinitely.
 */
public class PearlMaxTicksAliveTask implements Runnable {
    // Use final fields to ensure dependencies are not changed after initialization.
    private final OriginManager originManager;
    private final GeneralConfigHolder generalConfigHolder;

    /**
     * Constructs a new PearlMaxTicksAliveTask.
     *
     * @param originManager       The manager that tracks thrown pearls.
     * @param generalConfigHolder The configuration holder to get the max ticks alive value from.
     */
    public PearlMaxTicksAliveTask(OriginManager originManager, GeneralConfigHolder generalConfigHolder) {
        this.originManager = originManager;
        this.generalConfigHolder = generalConfigHolder;
    }

    @Override
    public void run() {
        // Get the configured max lifetime from the config.
        int maxTicksAlive = generalConfigHolder.getMaxTicksAlive();

        // If the feature is disabled in the config (value is 0 or less), do nothing.
        if (maxTicksAlive <= 0) {
            return;
        }

        // Get the iterator directly from the manager's projectile collection.
        // Using an iterator is required to safely remove elements during the loop.

        for (Projectile projectile : originManager.getProjectiles()) {
            // Check if the pearl is dead (e.g., hit a block) or has exceeded its lifetime.
            FoliaAPI.runTaskForEntity(projectile, () -> {
                if (projectile.isDead() || projectile.getTicksLived() > maxTicksAlive) {
                    projectile.remove();
                    originManager.getProjectiles().remove(projectile);
                }
            }, () -> {
            }, 1L);
        }
    }
}