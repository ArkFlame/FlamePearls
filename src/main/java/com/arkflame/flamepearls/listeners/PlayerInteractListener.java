package com.arkflame.flamepearls.listeners;

import java.text.DecimalFormat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.arkflame.flamepearls.FlamePearls;

public class PlayerInteractListener implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if the action is right click
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Get the player who interacted
            Player player = event.getPlayer();

            // Get the held item
            ItemStack heldItem = player.getInventory().getItem(player.getInventory().getHeldItemSlot());

            // Check if the player is holding an ender pearl in their main hand
            if (heldItem != null && heldItem.getType() == Material.ENDER_PEARL) {
                // Get the cooldown time remaining
                double cooldown = FlamePearls.getInstance().getCooldown(player);
                
                // Check if player has cooldown
                if (cooldown > 0) {
                    // Create a decimal format object with 0.0 pattern
                    DecimalFormat df = new DecimalFormat("0.0");
                    // Apply the format to the time
                    String cooldownSeconds = df.format(cooldown);
                    // Cancel the interaction event
                    event.setCancelled(true);
                    // Send a message to the player
                    player.sendMessage("You cannot throw ender pearls! Wait " + cooldownSeconds + "s");
                } else {
                    // Set the current time as last pearl thrown
                    FlamePearls.getInstance().updateLastPearl(player);
                }
            }
        }
    }
}
