package com.arkflame.flamepearls;

import com.arkflame.flamepearls.commands.FlamePearlsCommand;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.config.MessagesConfigHolder;
import com.arkflame.flamepearls.hooks.FlamePearlsPlaceholderHook;
import com.arkflame.flamepearls.listeners.*;
import com.arkflame.flamepearls.managers.CooldownManager;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.managers.TeleportDataManager;
import com.arkflame.flamepearls.tasks.PearlMaxTicksAliveTask;
import com.arkflame.flamepearls.utils.FoliaAPI;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for the FlamePearls plugin.
 * Handles the plugin's startup, shutdown, and dependency management.
 */
@Getter
public class FlamePearls extends JavaPlugin {

    @Getter private static FlamePearls instance;

    // Config Holders
    private GeneralConfigHolder generalConfigHolder = new GeneralConfigHolder();
    private MessagesConfigHolder messagesConfigHolder = new MessagesConfigHolder();

    // Managers
    private OriginManager originManager = new OriginManager();
    private CooldownManager cooldownManager = new CooldownManager();
    private TeleportDataManager teleportDataManager = new TeleportDataManager();

    // Hooks
    private FlamePearlsPlaceholderHook placeholderHook;

    @Override
    public void onLoad() {
        // Set the static instance safely and early in the plugin lifecycle.
        instance = this;
    }

    @Override
    public void onEnable() {
        getLogger().info("Enabling FlamePearls...");

        reloadConfigurations();
        registerListeners();
        registerCommands();
        registerHooks();

        PearlMaxTicksAliveTask pearlMaxTicksAliveTask = new PearlMaxTicksAliveTask(originManager, generalConfigHolder);
        FoliaAPI.runTaskTimer(obj -> pearlMaxTicksAliveTask.run(), 20L, 20L);

        getLogger().info("FlamePearls has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (placeholderHook != null && placeholderHook.isRegistered()) {
            placeholderHook.unregister();
            getLogger().info("Unregistered PlaceholderAPI hook.");
        }
        getLogger().info("FlamePearls has been disabled.");
    }

    /**
     * Loads or reloads all configuration files from disk.
     */
    public void reloadConfigurations() {
        saveDefaultConfig();
        reloadConfig();
        FileConfiguration config = getConfig();

        generalConfigHolder.load(config);
        messagesConfigHolder.load(config);
    }

    /**
     * Initializes and registers all Bukkit event listeners.
     */
    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CreatureSpawnListener(generalConfigHolder), this);
        pluginManager.registerEvents(new EntityDamageByEntityListener(generalConfigHolder), this);
        pluginManager.registerEvents(new EntityDamageListener(teleportDataManager, generalConfigHolder), this);
        pluginManager.registerEvents(new PlayerInteractListener(cooldownManager, messagesConfigHolder, generalConfigHolder), this);
        pluginManager.registerEvents(new PlayerJoinListener(), this);
        pluginManager.registerEvents(new PlayerQuitListener(teleportDataManager, cooldownManager), this);
        pluginManager.registerEvents(new PlayerTeleportListener(originManager, generalConfigHolder), this);
        pluginManager.registerEvents(new ProjectileHitListener(teleportDataManager, originManager, generalConfigHolder), this);
        pluginManager.registerEvents(new ProjectileLaunchListener(originManager), this);
    }

    /**
     * Registers all plugin command executors.
     */
    private void registerCommands() {
        getCommand("flamepearls")
                .setExecutor(new FlamePearlsCommand(this, generalConfigHolder, originManager, messagesConfigHolder));
    }

    /**
     * Detects and registers hooks for supported third-party plugins.
     */
    private void registerHooks() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI found. Registering hook...");
            placeholderHook = new FlamePearlsPlaceholderHook(this);
            placeholderHook.register();
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholders will not be available.");
        }
    }

    /**
     * A convenient utility method to run tasks asynchronously.
     * @param runnable The task to run.
     */
    public static void runAsync(Runnable runnable) {
        FoliaAPI.runTaskAsync(runnable);
    }
}