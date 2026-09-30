package com.plex.giveaways.ui;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.animation.AnimationRegistry;
import com.plex.giveaways.util.PlexText;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class AnimationOddsMenu extends MenuBase {
    public static void open(Player player, PlexGiveaways plugin) {
        AnimationOddsMenu holder = new AnimationOddsMenu();
        Inventory inv = Bukkit.createInventory(holder, 36, PlexText.component("&#00d2ff&l❖ &#00f2fe&lANIMATION CONFIG &#00d2ff&l❖"));
        holder.setInventory(inv);

        ItemStack border = DashboardMenu.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; ++i) {
            if (i < 9 || i >= 27 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        List<AnimationRegistry.AnimationDescriptor> anims = plugin.getAnimations().getRegisteredAnimations();
        int[] topSlots = {10, 12, 14, 16};
        int[] botSlots = {19, 21, 23, 25};

        NamespacedKey animKey = new NamespacedKey(plugin, "plex_anim_id");

        for (int i = 0; i < anims.size() && i < topSlots.length; ++i) {
            AnimationRegistry.AnimationDescriptor desc = anims.get(i);
            int tSlot = topSlots[i];
            int bSlot = botSlots[i];

            // Toggle item
            ItemStack toggle = DashboardMenu.createItem(
                    desc.icon(),
                    desc.title(),
                    "&#a0aec0Status: " + (desc.enabled() ? "&#2ecc71&lENABLED" : "&#ff4757&lDISABLED"),
                    "",
                    "&#ffd200[Click] &#ffffffToggle on/off"
            );
            ItemMeta tMeta = toggle.getItemMeta();
            if (tMeta != null) {
                tMeta.getPersistentDataContainer().set(animKey, PersistentDataType.STRING, desc.id());
                toggle.setItemMeta(tMeta);
            }
            inv.setItem(tSlot, toggle);

            // Weight item
            ItemStack weightItem = DashboardMenu.createItem(
                    Material.GOLD_NUGGET,
                    "&#ffd200&lSELECTION WEIGHT: &#00f2fe" + desc.weight() + "%",
                    "&#a0aec0Relative chance to be selected.",
                    "",
                    "&#2ecc71[Left-Click] &#ffffff+5% Chance",
                    "&#ff4757[Right-Click] &#ffffff-5% Chance"
            );
            ItemMeta wMeta = weightItem.getItemMeta();
            if (wMeta != null) {
                wMeta.getPersistentDataContainer().set(animKey, PersistentDataType.STRING, desc.id());
                weightItem.setItemMeta(wMeta);
            }
            inv.setItem(bSlot, weightItem);
        }

        inv.setItem(31, DashboardMenu.createItem(Material.ARROW, "&#ff4757&l⬅ RETURN", "&#a0aec0Return to main dashboard"));
        player.openInventory(inv);
    }
}
