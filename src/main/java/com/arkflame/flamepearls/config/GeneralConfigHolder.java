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
    private static final String MAX_TICKS_ALIVE_PATH = "max-ticks-alive";
    private static final String PREVENT_WORLD_BORDER_TELEPORT = "prevent-world-border-teleport";
    private static final String MAX_TICKS_ALIVE_ENABLED_PATH = "max-ticks-alive-enabled";
    private static final String PREVENT_WORLD_SWITCH_TELEPORT_PATH = "prevent-world-switch-teleport";
    private static final String MAX_TELEPORT_DISTANCE_PATH = "max-teleport-distance";

    private boolean disableEndermites;
    private double endermiteChance;
    private boolean resetFallDamageAfterTeleport;
    private int noDamageTicksAfterTeleport;
    private double pearlDamageSelf;
    private double pearlDamageOther;
    private double defaultPearlCooldown;
    private int maxTicksAlive;
    private boolean maxTicksAliveEnabled;
    private boolean preventWorldBorderTeleport;
    private boolean preventWorldSwitchTeleport;
    private boolean resetVelocityAfterTeleport;

    private List<Integer> permissionCooldownTiers = Collections.emptyList();
    private List<Sound> pearlSounds = Collections.emptyList();
    private Set<String> disabledWorlds = Collections.emptySet();

    private double maxTeleportDistance = 500.0;

    @Getter(AccessLevel.NONE)
    private final Map<UUID, Double> playerCooldowns = new ConcurrentHashMap<>();

    public void load(@NotNull Configuration config) {
        // Load existing config values.
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

        pearlSounds = loadSounds(config, PEARL_SOUND_PATH);

        // Load max ticks alive and whether the feature is enabled.
        maxTicksAlive = config.getInt(MAX_TICKS_ALIVE_PATH, 1200);
        maxTicksAliveEnabled = config.getBoolean(MAX_TICKS_ALIVE_ENABLED_PATH, true);

        // Load world-border and world-switch prevention options.
        preventWorldBorderTeleport = config.getBoolean(PREVENT_WORLD_BORDER_TELEPORT, true);
        preventWorldSwitchTeleport = config.getBoolean(PREVENT_WORLD_SWITCH_TELEPORT_PATH, false);

        // Load teleport distance limit.
        maxTeleportDistance = config.getDouble(MAX_TELEPORT_DISTANCE_PATH, 500.0);

        resetVelocityAfterTeleport = config.getBoolean("reset-velocity-after-teleport", true);
    }

    private List<Sound> loadSounds(@NotNull Configuration config, @NotNull String path) {
        List<String> soundNames;

        if (config.isString(path)) {
            soundNames = Collections.singletonList(config.getString(path));
        } else if (config.isList(path)) {
            soundNames = config.getStringList(path);
        } else {
            return Collections.emptyList();
        }

        Logger logger = FlamePearls.getInstance().getLogger();

        return soundNames.stream()
                .filter(name -> name != null && !name.isEmpty())
                .map(name -> {
                    try {
                        return Optional.of(Sound.valueOf(name.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Invalid sound name in config.yml at path '" + path + "': " + name);
                        return Optional.<Sound>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

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