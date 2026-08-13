package com.arkflame.flamepearls.compat.endermite;

import org.bukkit.Location;

public interface EndermiteSpawnBridge {
    boolean spawn(Location location);

    boolean preservesEnderPearlReason();
}
