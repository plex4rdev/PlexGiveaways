package com.plex.giveaways.ui;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.util.PlexText;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PoolEditorMenu extends MenuBase {
    private final String poolName;
    private boolean saved = false;

    public PoolEditorMenu(String poolName) {
        this.poolName = poolName;
    }

    public String getPoolName() {
        return poolName;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public static void open(Player player, PlexGiveaways plugin, String poolName) {
        PoolEditorMenu holder = new PoolEditorMenu(poolName);
        Inventory inv = Bukkit.createInventory(holder, 54, PlexText.component("&#00d2ff&l❖ POOL: &#00f2fe" + poolName.toUpperCase()));
        holder.setInventory(inv);

        List<ItemStack> existing = plugin.getRewardPools().getPoolPrizes(poolName);
        for (ItemStack item : existing) {
            if (item != null && item.getType() != Material.AIR) {
                inv.addItem(item.clone());
            }
        }

        inv.setItem(45, DashboardMenu.createItem(Material.RED_DYE, "&#ff4757&l✖ CANCEL", "&#a0aec0Discard changes and return."));
        inv.setItem(53, DashboardMenu.createItem(Material.LIME_DYE, "&#2ecc71&l✔ SAVE POOL", "&#a0aec0Save items & commands to prizes.yml"));

        player.openInventory(inv);
    }
}
