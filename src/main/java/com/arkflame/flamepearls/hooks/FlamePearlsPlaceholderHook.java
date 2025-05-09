package com.arkflame.flamepearls.hooks;

import com.arkflame.flamepearls.FlamePearls;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI hook for FlamePearls plugin.
 */
public class FlamePearlsPlaceholderHook extends PlaceholderExpansion {

    private final FlamePearls plugin;

    public FlamePearlsPlaceholderHook(FlamePearls plugin) {
        this.plugin = plugin;
    }

    /**
     * This method is called by PlaceholderAPI to check if your expansion is
     * supported and can be loaded.
     */
    @Override
    public boolean canRegister() {
        return plugin != null && plugin.isEnabled();
    }

    /**
     * The unique identifier for your placeholders (without the % signs).
     */
    @Override
    public String getIdentifier() {
        return "flamepearls";
    }

    /**
     * The author of this expansion.
     */
    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    /**
     * The version of this expansion.
     */
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Called when a placeholder with our identifier is found and needs a value.
     *
     * @param player       The player for which the placeholder is requested (may be null)
     * @param params       The part inside the placeholder after the identifier
     *                     e.g. for %flamepearls_name% this value is "name"
     * @return The result to replace the placeholder with, or null if not found
     */
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        switch (params.toLowerCase()) {
            case "cooldown":
                return FlamePearls.getInstance().getCooldownManager().getFancyCooldown(player);
            default:
                return null;
        }
    }
}