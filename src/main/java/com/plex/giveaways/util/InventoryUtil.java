package com.plex.giveaways.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class InventoryUtil {
    private InventoryUtil() {}

    public static void giveOrDrop(Player player, ItemStack item, String overflowMessage) {
        if (player == null || !player.isOnline() || item == null) {
            return;
        }

        Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
        if (!remaining.isEmpty()) {
            for (ItemStack leftover : remaining.values()) {
                if (leftover != null && leftover.getAmount() > 0) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
            if (overflowMessage != null && !overflowMessage.isEmpty()) {
                player.sendMessage(PlexText.colorize(overflowMessage));
            }
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
            } catch (Exception ignored) {}
        }
    }
}
