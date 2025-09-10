package com.arkflame.flamepearls.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A utility for playing sounds in a version-agnostic and efficient manner.
 */
public final class Sounds {

    private Sounds() {
        // Prevent instantiation of this utility class.
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // --- Core Sound Playing Methods (Most Efficient) ---

    /**
     * Plays a list of pre-parsed sounds for a specific player.
     * This is the most efficient method to use as it requires no string parsing.
     *
     * @param player The player to play the sounds for.
     * @param volume The volume of the sounds.
     * @param pitch  The pitch of the sounds.
     * @param sounds The list of Sound enums to play.
     */
    public static void play(@NotNull Player player, float volume, float pitch, @NotNull List<Sound> sounds) {
        if (sounds.isEmpty()) {
            return;
        }
        FoliaAPI.runTaskForEntity(player, () -> {
            Location loc = player.getLocation();
            for (Sound sound : sounds) {
                player.playSound(loc, sound, volume, pitch);
            }
        });
    }

    /**
     * Plays a list of pre-parsed sounds at a specific location.
     * This is the most efficient method to use as it requires no string parsing.
     *
     * @param location The location to play the sounds at.
     * @param volume   The volume of the sounds.
     * @param pitch    The pitch of the sounds.
     * @param sounds   The list of Sound enums to play.
     */
    public static void play(@NotNull Location location, float volume, float pitch, @NotNull List<Sound> sounds) {
        if (sounds.isEmpty()) {
            return;
        }
        FoliaAPI.runTaskForRegion(location, () -> {
            for (Sound sound : sounds) {
                location.getWorld().playSound(location, sound, volume, pitch);
            }
        });
    }
    
    // --- Sound Finding & String-based Playing Methods ---

    /**
     * Finds the first valid {@link Sound} from a collection of potential names.
     *
     * @param names A collection of sound names to check.
     * @return An {@link Optional} containing the first valid Sound, or an empty Optional if none are valid.
     */
    public static Optional<Sound> findFirstValid(@NotNull Collection<String> names) {
        for (String name : names) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            try {
                return Optional.of(Sound.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // This name is not valid for the current server version, try the next one.
            }
        }
        return Optional.empty();
    }

    /**
     * Plays the first valid sound from a list of names for a player.
     */
    public static void play(@NotNull Player player, float volume, float pitch, @NotNull String... soundNames) {
        findFirstValid(Arrays.asList(soundNames)).ifPresent(sound ->
            FoliaAPI.runTaskForEntity(player, () ->
                    player.playSound(player.getLocation(), sound, volume, pitch)
            )
        );
    }
    
    /**
     * Plays the first valid sound from a list of names at a specific location.
     */
    public static void play(@NotNull Location location, float volume, float pitch, @NotNull String... soundNames) {
        findFirstValid(Arrays.asList(soundNames)).ifPresent(sound ->
            FoliaAPI.runTaskForRegion(location, () ->
                    location.getWorld().playSound(location, sound, volume, pitch)
            )
        );
    }

    // --- "Play to All" Methods ---

    /**
     * Plays a list of pre-parsed sounds to all online players.
     * This is the most efficient method for broadcasting sounds.
     *
     * @param volume The volume of the sounds.
     * @param pitch  The pitch of the sounds.
     * @param sounds The list of Sound enums to play.
     */
    public static void playToAll(float volume, float pitch, @NotNull List<Sound> sounds) {
        if (sounds.isEmpty()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Delegate to the efficient per-player method
            play(player, volume, pitch, sounds);
        }
    }

    /**
     * Plays the first valid sound from a list of names to all online players.
     */
    public static void playToAll(float volume, float pitch, @NotNull Collection<String> soundNames) {
        Optional<Sound> soundOptional = findFirstValid(soundNames);
        
        soundOptional.ifPresent(sound -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                FoliaAPI.runTaskForEntity(player, () ->
                        player.playSound(player.getLocation(), sound, volume, pitch)
                );
            }
        });
    }

    /**
     * Convenience overload for {@link #playToAll(float, float, Collection)}.
     */
    public static void playToAll(float volume, float pitch, @NotNull String... soundNames) {
        playToAll(volume, pitch, Arrays.asList(soundNames));
    }
}