package com.arkflame.flamepearls.commands;

import com.arkflame.flamepearls.FlamePearls;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.config.MessagesConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class FlamePearlsCommand implements CommandExecutor {
    // Use final for fields initialized in the constructor to ensure they are not changed later.
    private final FlamePearls plugin;
    private final GeneralConfigHolder generalConfigHolder;
    private final MessagesConfigHolder messagesConfigHolder;
    private final OriginManager originManager;

    // Pass the main plugin instance to avoid static getInstance() calls (Dependency Injection).
    public FlamePearlsCommand(FlamePearls plugin, GeneralConfigHolder generalConfigHolder,
                              OriginManager originManager, MessagesConfigHolder messagesConfigHolder) {
        this.plugin = plugin;
        this.generalConfigHolder = generalConfigHolder;
        this.messagesConfigHolder = messagesConfigHolder;
        this.originManager = originManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check for subcommands. If "reload" is the first argument, handle it.
        // Otherwise, show the default stats message.
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            handleReload(sender);
        } else {
            showStats(sender);
        }
        // Always return true, as we are handling the command's logic here.
        return true;
    }

    /**
     * Handles the "reload" subcommand logic.
     *
     * @param sender The entity who executed the command.
     */
    private void handleReload(CommandSender sender) {
        final String permission = "flamepearls.reload";
        // Check for permission first (Guard Clause).
        if (!sender.hasPermission(permission)) {
            // Send the no-permission message and stop execution.
            String noPermMessage = messagesConfigHolder.getMessage("no-permission")
                    .replace("{permission}", permission);
            sender.sendMessage(noPermMessage);
            return;
        }

        // Reload configuration holders using the injected plugin instance.
        plugin.loadConfigurationHolders();

        // Send confirmation message.
        sender.sendMessage(messagesConfigHolder.getMessage("reloaded"));
    }

    /**
     * Handles showing the plugin's stats.
     *
     * @param sender The entity to send the stats to.
     */
    private void showStats(CommandSender sender) {
        // Get the base message and chain the replace() calls for better readability.
        String statsMessage = messagesConfigHolder.getMessage("stats")
                .replace("{damage}", String.valueOf(generalConfigHolder.getPearlDamageOther()))
                .replace("{damage-self}", String.valueOf(generalConfigHolder.getPearlDamageSelf()))
                .replace("{cooldown}", String.valueOf(generalConfigHolder.getPearlCooldown(null)))
                .replace("{thrown}", String.valueOf(originManager.getProjectileCount()));

        // Send the formatted message.
        sender.sendMessage(statsMessage);
    }
}