package com.arkflame.flamepearls.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Sound;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

import com.arkflame.flamepearls.FlamePearls;

public class GeneralConfigHolder {
    // Disable random endermite spawning
    private boolean disableEndermites = true;
    private double endermiteChance = 0.0;

    // Reset fall damage after teleport
    // It is necessary because if player fall and then teleported he receives damage
    // from the falling too.
    private boolean resetFallDamageAfterTeleport;

    // Damage modifiers
    private double pearlDamageSelf = 5;
    private double pearlDamageOther = 2;
    private int noDamageTicksAfterTeleport = 0;

    // The pearl cooldown in seconds
    private double pearlCooldown = 0.5;
    private List<Integer> pearlCooldowns = new ArrayList<>();

    // Sound played when teleporting
    private Sound pearlSound = null;

    private Collection<String> disabledWorlds = null;

    private Map<UUID, Double> playerCooldowns = new ConcurrentHashMap<>();

    public void load(Configuration config) {
        // Load disable endermites
        disableEndermites = config.getBoolean("disable-endermites", disableEndermites);
        endermiteChance = config.getDouble("endermite-chance", endermiteChance);

        // Load reset fall damage value
        resetFallDamageAfterTeleport = config.getBoolean("reset-fall-damage-after-teleport",
                resetFallDamageAfterTeleport);

        // Load no damage ticks after teleport.
        noDamageTicksAfterTeleport = config.getInt("teleport-no-damage-ticks");

        // Load pearl damage self
        pearlDamageSelf = config.getDouble("pearl-damage-self", pearlDamageSelf);

        // Load pearl damage other
        pearlDamageOther = config.getDouble("pearl-damage-other", pearlDamageOther);

        // Load pearl cooldown
        pearlCooldown = config.getDouble("pearl-cooldown", pearlCooldown);
        // Load pearl cooldowns
        pearlCooldowns = config.getIntegerList("pearl-cooldowns-perms");
        if (pearlCooldowns != null) {
            Collections.sort(pearlCooldowns);
        }

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

    public double getPearlCooldown(Player player) {
        return playerCooldowns.getOrDefault(player.getUniqueId(), pearlCooldown);
    }

    public void updateCooldown(Player player) {
        if (player != null && player.isOnline()) {
            // Check if player has any of the cooldown permissions
            for (int cooldown : pearlCooldowns) {
                if (player.hasPermission("flamepearls.cooldown." + cooldown)) {
                    playerCooldowns.put(player.getUniqueId(), Math.min(pearlCooldown, cooldown));
                    return;
                }
            }
            playerCooldowns.put(player.getUniqueId(), pearlCooldown);
        }
    }

    public void removeCooldown(Player player) {
        if (player != null) {
            playerCooldowns.remove(player.getUniqueId());
        }
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
