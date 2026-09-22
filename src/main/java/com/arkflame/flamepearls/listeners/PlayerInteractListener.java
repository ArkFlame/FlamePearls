package com.arkflame.flamepearls.listeners;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.config.MessagesConfigHolder;
import com.arkflame.flamepearls.managers.CooldownManager;
import com.arkflame.flamepearls.utils.MessageUtil;
import com.arkflame.flamepearls.utils.WorldUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;

public class PlayerInteractListener implements Listener {
    private final CooldownManager cooldownManager;
    private final MessagesConfigHolder messagesConfigHolder;
    private final GeneralConfigHolder generalConfigHolder;

    public PlayerInteractListener(final CooldownManager cooldownManager,
                                  final MessagesConfigHolder messagesConfigHolder,
                                  final GeneralConfigHolder generalConfigHolder) {
        this.cooldownManager = cooldownManager;
        this.messagesConfigHolder = messagesConfigHolder;
        this.generalConfigHolder = generalConfigHolder;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Player player = event.getPlayer();
        if (generalConfigHolder.isWorldDisabled(WorldUtil.getWorldName(player.getLocation()))) {
            return;
        }

        final ItemStack heldItem = event.getItem();
        if (heldItem == null || heldItem.getType() != Material.ENDER_PEARL) {
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK) {
            if (generalConfigHolder.isPreventPearlOnClickBlock()) {
                event.setCancelled(true);
                event.setUseItemInHand(Event.Result.DENY);
                return;
            }

            allowPearlUseOnClickedBlock(event);
        }

        if (!generalConfigHolder.isPearlCooldownEnabled()) {
            return;
        }

        final double cooldown = cooldownManager.getCooldown(player);
        if (cooldown > 0.0D) {
            final DecimalFormat decimalFormat = new DecimalFormat("0.0");
            final String cooldownSeconds = decimalFormat.format(cooldown);
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            MessageUtil.sendMessage(player, messagesConfigHolder.getMessage("cooldown")
                    .replace("{time}", cooldownSeconds));
            return;
        }

        cooldownManager.updateLastPearl(player);
    }

    private void allowPearlUseOnClickedBlock(final PlayerInteractEvent event) {
        if (event.useItemInHand() != Event.Result.DENY) {
            event.setUseItemInHand(Event.Result.ALLOW);
        }
        event.setUseInteractedBlock(Event.Result.DENY);
    }
}
