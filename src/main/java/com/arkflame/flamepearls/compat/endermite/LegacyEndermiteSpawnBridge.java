package com.arkflame.flamepearls.compat.endermite;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public final class LegacyEndermiteSpawnBridge implements EndermiteSpawnBridge {
    @Override
    public boolean spawn(final Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        location.getWorld().spawnEntity(location, EntityType.ENDERMITE);
        return true;
    }

    @Override
    public boolean preservesEnderPearlReason() {
        return false;
    }
}
