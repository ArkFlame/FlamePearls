package com.arkflame.flamepearls.listeners;

import java.text.DecimalFormat;
import java.util.Collection;

import com.arkflame.flamepearls.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.config.MessagesConfigHolder;
import com.arkflame.flamepearls.managers.CooldownManager;

public class PlayerInteractListener implements Listener {
    private CooldownManager cooldownManager;
    private MessagesConfigHolder messagesConfigHolder;
    private GeneralConfigHolder generalConfigHolder;

    public PlayerInteractListener(CooldownManager cooldownManager, MessagesConfigHolder messagesConfigHolder, GeneralConfigHolder generalConfigHolder) {
        this.cooldownManager = cooldownManager;
        this.messagesConfigHolder = messagesConfigHolder;
        this.generalConfigHolder = generalConfigHolder;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if the action is right click
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Get the player who interacted
            Player player = event.getPlayer();
            // Get the world
            World world = player.getLocation().getWorld();
            // Get disabled worlds
            Collection<String> disabledWorlds = generalConfigHolder.getDisabledWorlds();
            // This world is disabled
            if (disabledWorlds.contains(world.getName())) {
                return;
            }

            // Get the player inventory
            PlayerInventory inventory = player.getInventory();

            // Get the held item
            ItemStack heldItem = inventory.getItem(inventory.getHeldItemSlot());

            // Check if the player is holding an ender pearl in their main hand
            if (heldItem != null && heldItem.getType() == Material.ENDER_PEARL) {
                if (!generalConfigHolder.isPearlCooldownEnabled()) {
                    return;
                }
                // Get the cooldown time remaining
                double cooldown = cooldownManager.getCooldown(player);
                
                // Check if player has cooldown
                if (cooldown > 0) {
                    // Create a decimal format object with 0.0 pattern
                    DecimalFormat df = new DecimalFormat("0.0");
                    // Apply the format to the time
                    String cooldownSeconds = df.format(cooldown);
                    // Cancel the interaction event
                    event.setCancelled(true);
                    // Update pearl item
                    inventory.setItem(inventory.getHeldItemSlot(), heldItem);
                    // Send a message to the player
                    MessageUtil.sendMessage(player, messagesConfigHolder.getMessage("cooldown")
                            .replace("{time}", cooldownSeconds));
                } else {
                    // Set the current time as last pearl thrown
                    cooldownManager.updateLastPearl(player);
                }
            }
        }
    }
}
