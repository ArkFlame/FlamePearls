package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.services.EndermiteSpawnService;
import org.bukkit.entity.Endermite;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Objects;

public class CreatureSpawnListener implements Listener {
    private final GeneralConfigHolder generalConfigHolder;
    private final EndermiteSpawnService endermiteSpawnService;

    public CreatureSpawnListener(final GeneralConfigHolder generalConfigHolder,
                                 final EndermiteSpawnService endermiteSpawnService) {
        this.generalConfigHolder = Objects.requireNonNull(generalConfigHolder, "generalConfigHolder");
        this.endermiteSpawnService = Objects.requireNonNull(endermiteSpawnService, "endermiteSpawnService");
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(final CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Endermite)) {
            return;
        }

        if (generalConfigHolder.isEndermitesEnabled()) {
            return;
        }

        if (endermiteSpawnService.isPearlRelated(event)) {
            event.setCancelled(true);
        }
    }
}
