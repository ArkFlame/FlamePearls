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

    private boolean disableEndermites;
    private double endermiteChance;
    private boolean resetFallDamageAfterTeleport;
    private int noDamageTicksAfterTeleport;
    private double pearlDamageSelf;
    private double pearlDamageOther;
    private double defaultPearlCooldown;

    private List<Integer> permissionCooldownTiers = Collections.emptyList();
    private Optional<Sound> pearlSound = Optional.empty();
    private Set<String> disabledWorlds = Collections.emptySet();

    @Getter(AccessLevel.NONE)
    private final Map<UUID, Double> playerCooldowns = new ConcurrentHashMap<>();

    public void load(@NotNull Configuration config) {
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
        
        pearlSound = loadEnum(config, PEARL_SOUND_PATH, Sound.class);
    }

    private <T extends Enum<T>> Optional<T> loadEnum(@NotNull Configuration config, @NotNull String path, @NotNull Class<T> enumClass) {
        String name = config.getString(path);
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(enumClass, name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            Logger logger = FlamePearls.getInstance().getLogger();
            logger.warning("Invalid " + enumClass.getSimpleName() + " name in config.yml at path '" + path + "': " + name);
            return Optional.empty();
        }
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