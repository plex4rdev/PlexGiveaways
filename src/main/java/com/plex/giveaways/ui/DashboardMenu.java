package com.plex.giveaways.ui;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.util.PlexText;
import com.plex.giveaways.util.TimeFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class DashboardMenu extends MenuBase {
    public static void open(Player player, PlexGiveaways plugin) {
        DashboardMenu holder = new DashboardMenu();
        Inventory inv = Bukkit.createInventory(holder, 54, PlexText.component("&#00d2ff&l❖ &#00f2fe&lPLEX DASHBOARD &#00d2ff&l❖"));
        holder.setInventory(inv);

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; ++i) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        // Slot 4: Scheduler Overview
        boolean schedOn = plugin.getScheduler().isEnabled();
        long interval = plugin.getScheduler().getIntervalSeconds();
        long nextSec = plugin.getScheduler().getSecondsUntilNext();
        inv.setItem(4, createItem(
                Material.CLOCK,
                "&#ffd200&lAUTOMATED SCHEDULER: " + (schedOn ? "&#2ecc71&lACTIVE" : "&#ff4757&lDISABLED"),
                "&#a0aec0Cycle Interval: &#ffffff" + (interval / 60) + " minutes",
                "&#a0aec0Next Event: &#00f2fe" + TimeFormatter.formatDuration(nextSec),
                "",
                "&#2ecc71[Left-Click] &#ffffff+5 Minutes",
                "&#ff4757[Right-Click] &#ffffff-5 Minutes",
                "&#ffd200[Shift-Click] &#ffffffToggle State"
        ));

        // Slot 20: Quick Launch
        inv.setItem(20, createItem(
                Material.EMERALD_BLOCK,
                "&#2ecc71&lLAUNCH GIVEAWAY",
                "&#a0aec0Click to immediately trigger",
                "&#a0aec0a server-wide giveaway event."
        ));

        // Slot 22: Abort
        inv.setItem(22, createItem(
                Material.REDSTONE_BLOCK,
                "&#ff4757&lABORT ACTIVE EVENT",
                "&#a0aec0Immediately halt any spinning animation,",
                "&#a0aec0close menus, and despawn visuals."
        ));

        // Slot 24: Prize Pools
        inv.setItem(24, createItem(
                Material.CHEST,
                "&#00d2ff&lREWARD POOL MANAGER",
                "&#a0aec0Open the interactive item & voucher",
                "&#a0aec0editor for all 4 animation pools."
        ));

        // Slot 30: Animation Settings
        inv.setItem(30, createItem(
                Material.COMPARATOR,
                "&#00f2fe&lANIMATION ODDS & TOGGLES",
                "&#a0aec0Configure weights and toggle",
                "&#a0aec0Roulette, Vortex, Overhead, & Instant."
        ));

        // Slot 32: Reload
        inv.setItem(32, createItem(
                Material.NETHER_STAR,
                "&#ffd200&lHOT-RELOAD SYSTEM",
                "&#a0aec0Reload configs, language, schedules,",
                "&#a0aec0and rewards without a server restart."
        ));

        // Slot 40: Min players
        int minP = plugin.getConfig().getInt("general.min-participants-to-start", 1);
        inv.setItem(40, createItem(
                Material.PLAYER_HEAD,
                "&#00d2ff&lMINIMUM PARTICIPANTS: &#ffd200" + minP,
                "&#a0aec0Required online players to trigger.",
                "",
                "&#2ecc71[Left-Click] &#ffffff+1 Player",
                "&#ff4757[Right-Click] &#ffffff-1 Player"
        ));

        player.openInventory(inv);
    }

    public static ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(PlexText.component(name));
            if (loreLines != null && loreLines.length > 0) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(PlexText.component(line));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
