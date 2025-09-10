package com.arkflame.flamepearls.config;

import com.arkflame.flamepearls.FlamePearls;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
public class GeneralConfigHolder {

    // ... (Constants remain the same) ...
    private static final String DISABLE_ENDERMITES_PATH = "disable-endermites";
    private static final String ENDERMITE_CHANCE_PATH = "endermite-chance";
    private static final String RESET_FALL_DAMAGE_PATH = "reset-fall-damage-after-teleport";
    private static final String NO_DAMAGE_TICKS_PATH = "teleport-no-damage-ticks";
    private static final String PEARL_DAMAGE_SELF_PATH = "pearl-damage-self";
    private static final String PEARL_DAMAGE_OTHER_PATH = "pearl-damage-other";
    private static final String PEARL_COOLDOWN_PATH = "pearl-cooldown";
    private static final String PEARL_COOLDOWN_PERMS_PATH = "pearl-cooldowns-perms";
    private static final String PEARL_SOUND_PATH = "pearl-sound";
    private static final String DISABLED_WORLDS_PATH = "disabled-worlds";

    private boolean disableEndermites;
    private double endermiteChance;
    private boolean resetFallDamageAfterTeleport;
    private int noDamageTicksAfterTeleport;
    private double pearlDamageSelf;
    private double pearlDamageOther;
    private double defaultPearlCooldown;

    private List<Integer> permissionCooldownTiers = Collections.emptyList();
    // The field is now a List of Sounds to support multiple sounds.
    private List<Sound> pearlSounds = Collections.emptyList();
    private Set<String> disabledWorlds = Collections.emptySet();

    @Getter(AccessLevel.NONE)
    private final Map<UUID, Double> playerCooldowns = new ConcurrentHashMap<>();

    public void load(@NotNull Configuration config) {
        // ... (other config loading remains the same) ...
        disableEndermites = config.getBoolean(DISABLE_ENDERMITES_PATH, true);
        endermiteChance = config.getDouble(ENDERMITE_CHANCE_PATH, 0.0);
        resetFallDamageAfterTeleport = config.getBoolean(RESET_FALL_DAMAGE_PATH, true);
        noDamageTicksAfterTeleport = config.getInt(NO_DAMAGE_TICKS_PATH, 0);
        pearlDamageSelf = config.getDouble(PEARL_DAMAGE_SELF_PATH, 5.0);
        pearlDamageOther = config.getDouble(PEARL_DAMAGE_OTHER_PATH, 2.0);
        defaultPearlCooldown = config.getDouble(PEARL_COOLDOWN_PATH, 0.5);

        permissionCooldownTiers = config.getIntegerList(PEARL_COOLDOWN_PERMS_PATH)
                .stream()
                .sorted()
                .collect(Collectors.toList());

        disabledWorlds = new HashSet<>(config.getStringList(DISABLED_WORLDS_PATH));
        
        // Use the new, more flexible method to load sounds.
        pearlSounds = loadSounds(config, PEARL_SOUND_PATH);
    }

    /**
     * Gracefully loads one or more sounds from the configuration.
     * This method is backward-compatible and supports both a single string
     * and a list of strings for the sound path.
     *
     * @param config The configuration to load from.
     * @param path   The path to the sound(s).
     * @return A list of valid Sound enums. Returns an empty list if none are found or valid.
     */
    private List<Sound> loadSounds(@NotNull Configuration config, @NotNull String path) {
        List<String> soundNames;

        if (config.isString(path)) {
            // Backward compatibility: Handle the old single-string format.
            soundNames = Collections.singletonList(config.getString(path));
        } else if (config.isList(path)) {
            // New format: Handle a list of strings.
            soundNames = config.getStringList(path);
        } else {
            // Path is missing or is not a String/List, return empty.
            return Collections.emptyList();
        }

        Logger logger = FlamePearls.getInstance().getLogger();

        // Use Java 8 Streams to parse and validate each sound name.
        return soundNames.stream()
                .filter(name -> name != null && !name.isEmpty()) // Ensure name is not null or empty
                .map(name -> {
                    try {
                        return Optional.of(Sound.valueOf(name.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Invalid sound name in config.yml at path '" + path + "': " + name);
                        return Optional.<Sound>empty();
                    }
                })
                .filter(Optional::isPresent) // Filter out any invalid sounds
                .map(Optional::get)         // Unwrap the valid sounds from the Optional
                .collect(Collectors.toList()); // Collect them into a list
    }
    
    // ... (other methods remain the same) ...
    public double getPearlCooldown(Player player) {
        if (player == null) {
            return defaultPearlCooldown;
        }
        return playerCooldowns.getOrDefault(player.getUniqueId(), defaultPearlCooldown);
    }

    public boolean isWorldDisabled(@NotNull String worldName) {
        return disabledWorlds.contains(worldName);
    }

    public void updateCooldown(@NotNull Player player) {
        for (int cooldownTier : permissionCooldownTiers) {
            if (player.hasPermission("flamepearls.cooldown." + cooldownTier)) {
                playerCooldowns.put(player.getUniqueId(), Math.min(defaultPearlCooldown, cooldownTier));
                return;
            }
        }
        playerCooldowns.remove(player.getUniqueId());
    }

    public void removeCooldown(@NotNull Player player) {
        playerCooldowns.remove(player.getUniqueId());
    }
}