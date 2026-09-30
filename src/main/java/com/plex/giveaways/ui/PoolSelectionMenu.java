package com.plex.giveaways.ui;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.util.PlexText;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PoolSelectionMenu extends MenuBase {
    public static void open(Player player, PlexGiveaways plugin) {
        PoolSelectionMenu holder = new PoolSelectionMenu();
        Inventory inv = Bukkit.createInventory(holder, 27, PlexText.component("&#00d2ff&l❖ &#00f2fe&lSELECT REWARD POOL &#00d2ff&l❖"));
        holder.setInventory(inv);

        ItemStack border = DashboardMenu.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; ++i) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        inv.setItem(10, DashboardMenu.createItem(Material.HOPPER, "&#00d2ff&lROULETTE POOL", "&#a0aec0Edit prizes for GUI Roulette."));
        inv.setItem(12, DashboardMenu.createItem(Material.NETHER_STAR, "&#ffffff&lVORTEX POOL", "&#a0aec0Edit prizes for Particle Vortex."));
        inv.setItem(14, DashboardMenu.createItem(Material.GOLDEN_HELMET, "&#ffd200&lOVERHEAD SHUFFLE POOL", "&#a0aec0Edit prizes for Crown Shuffle."));
        inv.setItem(16, DashboardMenu.createItem(Material.CLOCK, "&#00f2fe&lINSTANT ROLL POOL", "&#a0aec0Edit prizes for Instant Roll."));

        inv.setItem(22, DashboardMenu.createItem(Material.ARROW, "&#ff4757&l⬅ RETURN", "&#a0aec0Return to main dashboard"));
        player.openInventory(inv);
    }
}
