package com.arkflame.flamepearls;

import org.bukkit.configuration.Configuration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.arkflame.flamepearls.commands.FlamePearlsCommand;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.config.MessagesConfigHolder;
import com.arkflame.flamepearls.listeners.CreatureSpawnListener;
import com.arkflame.flamepearls.listeners.EntityDamageByEntityListener;
import com.arkflame.flamepearls.listeners.PlayerInteractListener;
import com.arkflame.flamepearls.listeners.PlayerQuitListener;
import com.arkflame.flamepearls.listeners.PlayerTeleportListener;
import com.arkflame.flamepearls.listeners.ProjectileHitListener;
import com.arkflame.flamepearls.listeners.ProjectileLaunchListener;
import com.arkflame.flamepearls.managers.CooldownManager;
import com.arkflame.flamepearls.managers.OriginManager;

public class FlamePearls extends JavaPlugin implements Listener {
    // Managers
    private OriginManager originManager;
    private CooldownManager cooldownManager;

    // Config
    private GeneralConfigHolder generalConfigHolder;
    private MessagesConfigHolder messagesConfigHolder;

    @Override
    public void onEnable() {
        // Set static instance
        FlamePearls.instance = this;

        // Get the plugin manager
        PluginManager pluginManager = getServer().getPluginManager();

        // Create general config
        generalConfigHolder = new GeneralConfigHolder();

        // Create messages config
        messagesConfigHolder = new MessagesConfigHolder();

        // Reload configurations
        loadConfigurationHolders();

        // Create the origin manager
        originManager = new OriginManager();

        // Create the cooldown manager
        cooldownManager = new CooldownManager(generalConfigHolder);

        // Register the event listener
        pluginManager.registerEvents(this, this);
        // Register CreatureSpawnListener
        pluginManager.registerEvents(new CreatureSpawnListener(generalConfigHolder), this);
        // Register EntityDamageByEntityListener
        pluginManager.registerEvents(new EntityDamageByEntityListener(generalConfigHolder), this);
        // Register Player Interact Listener
        pluginManager.registerEvents(new PlayerInteractListener(cooldownManager, messagesConfigHolder), this);
        // Register Player quit listener
        pluginManager.registerEvents(new PlayerQuitListener(cooldownManager), this);
        // Register PlayerTeleportListener
        pluginManager.registerEvents(new PlayerTeleportListener(generalConfigHolder, originManager), this);
        // Register ProjectileHitListener
        pluginManager.registerEvents(new ProjectileHitListener(originManager, generalConfigHolder), this);
        // Register ProjectileLaunchListener
        pluginManager.registerEvents(new ProjectileLaunchListener(originManager), this);

        // Register FlamePearls command
        getCommand("flamepearls").setExecutor(new FlamePearlsCommand(generalConfigHolder, originManager, messagesConfigHolder));
    }

    public void loadConfigurationHolders() {
        // Regenerate config
        saveDefaultConfig();
        // Reload config
        reloadConfig();
        // Get config
        Configuration config = getConfig();
        // Load general config
        generalConfigHolder.load(config);
        // Load messages config
        messagesConfigHolder.load(config);
    }

    private static FlamePearls instance;

    public static FlamePearls getInstance() {
        return FlamePearls.instance;
    }

    /**
     * Use this to get and manage the origin of ender pearls thrown.
     */
    public OriginManager getOriginManager() {
        return originManager;
    }

    /**
     * Use this to get and manage the ender pearl cooldown of players
     */
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}