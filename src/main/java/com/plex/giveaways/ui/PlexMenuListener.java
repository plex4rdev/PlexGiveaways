package com.plex.giveaways.ui;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.util.PlexText;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PlexMenuListener implements Listener {
    private final PlexGiveaways plugin;
    private final NamespacedKey animKey;

    public PlexMenuListener(PlexGiveaways plugin) {
        this.plugin = plugin;
        this.animKey = new NamespacedKey(plugin, "plex_anim_id");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuBase) {
            if (!(event.getInventory().getHolder() instanceof PoolEditorMenu)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuBase holder)) {
            return;
        }

        ClickType click = event.getClick();
        InventoryAction action = event.getAction();
        int rawSlot = event.getRawSlot();

        // 1. Read-only Menus: Dashboard, Odds, PoolSelection, Roulette
        if (holder instanceof DashboardMenu || holder instanceof AnimationOddsMenu
                || holder instanceof PoolSelectionMenu || holder instanceof RouletteTapeMenu) {

            event.setCancelled(true);

            // Block hotbar/offhand injection
            if (click == ClickType.NUMBER_KEY || click == ClickType.SWAP_OFFHAND
                    || click == ClickType.DROP || click == ClickType.CONTROL_DROP
                    || action == InventoryAction.COLLECT_TO_CURSOR) {
                return;
            }

            if (rawSlot >= top.getSize()) {
                return; // Clicked in player inventory
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            // --- DashboardMenu Actions ---
            if (holder instanceof DashboardMenu) {
                if (rawSlot == 4) {
                    // Scheduler
                    long current = this.plugin.getScheduler().getIntervalSeconds();
                    if (click.isShiftClick()) {
                        this.plugin.getScheduler().setEnabled(!this.plugin.getScheduler().isEnabled());
                    } else if (click.isLeftClick()) {
                        current += 300L;
                    } else if (click.isRightClick()) {
                        current = Math.max(60L, current - 300L);
                    }
                    this.plugin.getScheduler().setInterval(current);
                    this.playClick(player);
                    DashboardMenu.open(player, this.plugin);
                } else if (rawSlot == 20) {
                    player.closeInventory();
                    this.plugin.getEngine().launchGiveaway(player, null);
                } else if (rawSlot == 22) {
                    player.closeInventory();
                    this.plugin.getEngine().abortGiveaway();
                } else if (rawSlot == 24) {
                    PoolSelectionMenu.open(player, this.plugin);
                    this.playClick(player);
                } else if (rawSlot == 30) {
                    AnimationOddsMenu.open(player, this.plugin);
                    this.playClick(player);
                } else if (rawSlot == 32) {
                    this.plugin.reloadAll();
                    player.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#2ecc71Configurations & pools reloaded successfully!"));
                    this.playClick(player);
                    DashboardMenu.open(player, this.plugin);
                } else if (rawSlot == 40) {
                    int min = this.plugin.getConfig().getInt("general.min-participants-to-start", 1);
                    if (click.isLeftClick()) min++;
                    else if (click.isRightClick()) min = Math.max(1, min - 1);
                    this.plugin.getConfig().set("general.min-participants-to-start", min);
                    this.plugin.saveConfig();
                    this.playClick(player);
                    DashboardMenu.open(player, this.plugin);
                }
                return;
            }

            // --- AnimationOddsMenu Actions ---
            if (holder instanceof AnimationOddsMenu) {
                if (clicked.getType() == Material.ARROW) {
                    DashboardMenu.open(player, this.plugin);
                    this.playClick(player);
                    return;
                }

                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.getPersistentDataContainer().has(this.animKey, PersistentDataType.STRING)) {
                    String aId = meta.getPersistentDataContainer().get(this.animKey, PersistentDataType.STRING);
                    if (aId != null) {
                        if (clicked.getType() == Material.GOLD_NUGGET) {
                            var anims = this.plugin.getAnimations().getRegisteredAnimations();
                            int weight = 25;
                            for (var a : anims) {
                                if (a.id().equalsIgnoreCase(aId)) weight = a.weight();
                            }
                            if (click.isLeftClick()) weight += 5;
                            else if (click.isRightClick()) weight = Math.max(0, weight - 5);
                            this.plugin.getAnimations().setWeight(aId, weight);
                        } else {
                            var anims = this.plugin.getAnimations().getRegisteredAnimations();
                            boolean state = true;
                            for (var a : anims) {
                                if (a.id().equalsIgnoreCase(aId)) state = a.enabled();
                            }
                            this.plugin.getAnimations().setEnabled(aId, !state);
                        }
                        this.playClick(player);
                        AnimationOddsMenu.open(player, this.plugin);
                    }
                }
                return;
            }

            // --- PoolSelectionMenu Actions ---
            if (holder instanceof PoolSelectionMenu) {
                if (clicked.getType() == Material.ARROW) {
                    DashboardMenu.open(player, this.plugin);
                    this.playClick(player);
                    return;
                }

                if (rawSlot == 10) {
                    PoolEditorMenu.open(player, this.plugin, "roulette");
                } else if (rawSlot == 12) {
                    PoolEditorMenu.open(player, this.plugin, "vortex");
                } else if (rawSlot == 14) {
                    PoolEditorMenu.open(player, this.plugin, "overhead");
                } else if (rawSlot == 16) {
                    PoolEditorMenu.open(player, this.plugin, "instant");
                }
                this.playClick(player);
                return;
            }

            // --- RouletteTapeMenu ---
            if (holder instanceof RouletteTapeMenu) {
                return;
            }
        }

        // 2. PoolEditorMenu Actions
        if (holder instanceof PoolEditorMenu editor) {
            // Protect save/cancel control slots from hotbar swapping
            if (rawSlot == 45 || rawSlot == 53) {
                event.setCancelled(true);
                if (rawSlot == 45) {
                    // Cancel
                    editor.setSaved(true);
                    PoolSelectionMenu.open(player, this.plugin);
                    this.playClick(player);
                } else {
                    // Save
                    editor.setSaved(true);
                    this.plugin.getRewardPools().savePoolFromInventory(editor.getPoolName(), top);
                    player.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#2ecc71Saved reward pool for " + editor.getPoolName() + "!"));
                    this.playClick(player);
                    PoolSelectionMenu.open(player, this.plugin);
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof PoolEditorMenu editor) {
            if (!editor.isSaved()) {
                // Auto-save so staged rewards are never deleted!
                this.plugin.getRewardPools().savePoolFromInventory(editor.getPoolName(), event.getInventory());
                if (event.getPlayer() instanceof Player player) {
                    player.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#2ecc71Auto-saved pool changes on close."));
                }
            }
        }
    }

    private void playClick(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        } catch (Exception ignored) {}
    }
}
