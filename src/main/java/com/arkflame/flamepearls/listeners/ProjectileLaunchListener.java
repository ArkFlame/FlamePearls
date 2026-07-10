package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.handlers.PearlFixerHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.Objects;

public final class ProjectileLaunchListener implements Listener {
    private final PearlFixerHandler pearlFixerHandler;

    public ProjectileLaunchListener(final PearlFixerHandler pearlFixerHandler) {
        this.pearlFixerHandler = Objects.requireNonNull(pearlFixerHandler, "pearlFixerHandler");
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        pearlFixerHandler.onProjectileLaunch(event);
    }
}