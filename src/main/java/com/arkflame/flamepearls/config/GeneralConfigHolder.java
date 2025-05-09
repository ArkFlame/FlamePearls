package com.arkflame.flamepearls.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.bukkit.Sound;
import org.bukkit.configuration.Configuration;

import com.arkflame.flamepearls.FlamePearls;

public class GeneralConfigHolder {
    // Disable random endermite spawning
    private boolean disableEndermites = true;
    private double endermiteChance = 0.0;

    // Reset fall damage after teleport
    // It is necessary because if player fall and then teleported he receives damage from the falling too.
    private boolean resetFallDamageAfterTeleport;

    // Damage modifiers
    private double pearlDamageSelf = 5;
    private double pearlDamageOther = 2;
    private int noDamageTicksAfterTeleport = 0;

    // The pearl cooldown in seconds
    private double pearlCooldown = 0.5;

    // Sound played when teleporting
    private Sound pearlSound = null;

    private Collection<String> disabledWorlds = null;

    public void load(Configuration config) {
        // Load disable endermites
        disableEndermites = config.getBoolean("disable-endermites", disableEndermites);
        endermiteChance = config.getDouble("endermite-chance", endermiteChance);

        // Load reset fall damage value
        resetFallDamageAfterTeleport = config.getBoolean("reset-fall-damage-after-teleport", resetFallDamageAfterTeleport);

        // Load no damage ticks after teleport.
        noDamageTicksAfterTeleport = config.getInt("teleport-no-damage-ticks");

        // Load pearl damage self
        pearlDamageSelf = config.getDouble("pearl-damage-self", pearlDamageSelf);

        // Load pearl damage other
        pearlDamageOther = config.getDouble("pearl-damage-other", pearlDamageOther);

        // Load pearl cooldown
        pearlCooldown = config.getDouble("pearl-cooldown", pearlCooldown);

        // Load pearl sound name
        String pearlSoundName = config.getString("pearl-sound");

        // Try processing pearl sound
        try {
            // Process pearl sound
            pearlSound = Sound.valueOf(pearlSoundName);
        } catch (IllegalArgumentException ex) {
            FlamePearls.getInstance().getLogger().warning("Invalid pearl sound: " + pearlSoundName);
        }

        // Load disabled worlds
        disabledWorlds = config.getStringList("disabled-worlds");
        // Convert to hashset for performance
        if (disabledWorlds != null) {
            disabledWorlds = new HashSet<>(disabledWorlds);
        }
    }

    public boolean isDisableEndermites() {
        return disableEndermites;
    }

    public boolean isResetFallDamageAfterTeleport() {
        return resetFallDamageAfterTeleport;
    }

    public double getPearlDamageSelf() {
        return pearlDamageSelf;
    }

    public double getPearlDamageOther() {
        return pearlDamageOther;
    }

    public double getPearlCooldown() {
        return pearlCooldown;
    }

    public Sound getPearlSound() {
        return pearlSound;
    }

    public double getEndermiteChance() {
        return endermiteChance;
    }

    public int getNoDamageTicksAfterTeleport() {
        return noDamageTicksAfterTeleport;
    }

    public Collection<String> getDisabledWorlds() {
        return disabledWorlds == null ? Collections.emptySet() : disabledWorlds;
    }
}
