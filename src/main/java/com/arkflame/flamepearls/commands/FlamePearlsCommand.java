package com.arkflame.flamepearls.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.arkflame.flamepearls.FlamePearls;
import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.config.MessagesConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;

public class FlamePearlsCommand implements CommandExecutor {
    private GeneralConfigHolder generalConfigHolder;
    private MessagesConfigHolder messagesConfigHolder;
    private OriginManager originManager;

    public FlamePearlsCommand(GeneralConfigHolder generalConfigHolder, OriginManager originManager,
            MessagesConfigHolder messagesConfigHolder) {
        this.generalConfigHolder = generalConfigHolder;
        this.messagesConfigHolder = messagesConfigHolder;
        this.originManager = originManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if there is an argument
        if (args.length > 0) {
            // Switch over argument
            switch (args[0].toUpperCase()) {
                // Reload argument
                case "RELOAD": {
                    // Check if player has permission
                    if (sender.hasPermission("flamepearls.reload")) {
                        // Load configuration holders
                        FlamePearls.getInstance().loadConfigurationHolders();

                        // Send message
                        sender.sendMessage(messagesConfigHolder.getMessage("reloaded"));
                    } else {
                        // Send message
                        sender.sendMessage(messagesConfigHolder.getMessage("no-permission").replace("{permission}",
                                "flamepearls.reload"));
                    }

                    // Return
                    return true;
                }
                default: {
                    // Just break
                    break;
                }
            }
        }

        // Get stats message
        String statsMessage = messagesConfigHolder.getMessage("stats");
        // Set the damage
        statsMessage = statsMessage.replace("{damage}", String.valueOf(generalConfigHolder.getPearlDamageOther()));
        // Set the damage
        statsMessage = statsMessage.replace("{damage-self}", String.valueOf(generalConfigHolder.getPearlDamageSelf()));
        // Set the damage
        statsMessage = statsMessage.replace("{cooldown}", String.valueOf(generalConfigHolder.getPearlCooldown(null)));
        // Set the damage
        statsMessage = statsMessage.replace("{thrown}", String.valueOf(originManager.getProjectileCount()));
        // Send the message
        sender.sendMessage(statsMessage);

        // Return
        return true;
    }
}
