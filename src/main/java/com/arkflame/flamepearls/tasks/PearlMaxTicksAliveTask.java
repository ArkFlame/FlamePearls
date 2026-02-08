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
    private final OriginManager originManager;
    private final GeneralConfigHolder generalConfigHolder;

    public PearlMaxTicksAliveTask(OriginManager originManager, GeneralConfigHolder generalConfigHolder) {
        this.originManager = originManager;
        this.generalConfigHolder = generalConfigHolder;
    }

    @Override
    public void run() {
        // If the feature is disabled explicitly, do nothing.
        if (!generalConfigHolder.isMaxTicksAliveEnabled()) {
            return;
        }
        int maxTicksAlive = generalConfigHolder.getMaxTicksAlive();
        // If the configured max is 0 or less, do nothing.
        if (maxTicksAlive <= 0) {
            return;
        }
        for (Projectile projectile : originManager.getProjectiles()) {
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