package com.plex.giveaways.animation.impl;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.animation.GiveawayAnimation;
import com.plex.giveaways.ui.RouletteTapeMenu;
import com.plex.giveaways.util.PlexText;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.*;

public class RouletteTapeAnimation extends GiveawayAnimation {
    private final PlexGiveaways plugin;
    private final Player winner;
    private final List<ItemStack> prizePool;
    private final ItemStack winningPrize;
    private final List<Inventory> activeInventories = new ArrayList<>();
    private final List<ItemStack> rollingItems = new ArrayList<>();

    private int step = 0;
    private int shiftIndex = 0;
    private int nextTickThreshold = 0;
    private int currentInterval = 1;
    private boolean finished = false;

    private static final int SELECTOR_SLOT = 13;
    private static final int MAX_STEPS = 45;

    public RouletteTapeAnimation(PlexGiveaways plugin, Player winner, List<ItemStack> prizePool) {
        this.plugin = plugin;
        this.winner = winner;
        this.prizePool = (prizePool != null && !prizePool.isEmpty())
                ? prizePool
                : Collections.singletonList(new ItemStack(Material.DIAMOND, 1));
        this.winningPrize = this.prizePool.get(new Random().nextInt(this.prizePool.size())).clone();

        this.prepareRollingTape();
        this.openMenusForAll();
    }

    private void prepareRollingTape() {
        Random rnd = new Random();
        List<ItemStack> poolCopy = new ArrayList<>(this.prizePool);
        for (int i = 0; i < 70; ++i) {
            this.rollingItems.add(poolCopy.get(rnd.nextInt(poolCopy.size())).clone());
        }
        // Place the determined winning prize at the stopping index
        int finalTapeIndex = MAX_STEPS + (SELECTOR_SLOT - 9);
        if (finalTapeIndex < this.rollingItems.size()) {
            this.rollingItems.set(finalTapeIndex, this.winningPrize.clone());
        }
    }

    private void openMenusForAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            RouletteTapeMenu holder = new RouletteTapeMenu();
            Inventory inv = Bukkit.createInventory(holder, 27, PlexText.component("&#00d2ff&l❖ &#00f2fe&lGIVEAWAY ROULETTE &#00d2ff&l❖"));
            holder.setInventory(inv);

            this.fillBorders(inv, Material.CYAN_STAINED_GLASS_PANE);
            this.renderTape(inv, 0);

            p.openInventory(inv);
            this.activeInventories.add(inv);
        }
    }

    private void fillBorders(Inventory inv, Material borderMat) {
        ItemStack border = new ItemStack(borderMat);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(PlexText.component(" "));
            border.setItemMeta(meta);
        }

        for (int slot = 0; slot < 27; ++slot) {
            if (slot < 9 || slot >= 18) {
                inv.setItem(slot, border);
            }
        }

        // Indicator markers above and below center selector slot (slot 13)
        ItemStack indicator = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta indMeta = indicator.getItemMeta();
        if (indMeta != null) {
            indMeta.displayName(PlexText.component("&#ffd200&l▼ SELECTOR ▼"));
            indicator.setItemMeta(indMeta);
        }
        inv.setItem(4, indicator);

        ItemStack bottomInd = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta bMeta = bottomInd.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(PlexText.component("&#ffd200&l▲ SELECTOR ▲"));
            bottomInd.setItemMeta(bMeta);
        }
        inv.setItem(22, bottomInd);
    }

    private void renderTape(Inventory inv, int offset) {
        for (int slot = 9; slot <= 17; ++slot) {
            int itemIdx = (offset + (slot - 9)) % this.rollingItems.size();
            inv.setItem(slot, this.rollingItems.get(itemIdx));
        }
    }

    @Override
    public void run() {
        if (this.finished) {
            return;
        }

        if (this.step >= MAX_STEPS) {
            this.concludeWin();
            return;
        }

        if (this.nextTickThreshold <= 0) {
            ++this.step;
            ++this.shiftIndex;

            // Non-linear deceleration
            if (this.step < 20) {
                this.currentInterval = 1;
            } else if (this.step < 30) {
                this.currentInterval = 2;
            } else if (this.step < 38) {
                this.currentInterval = 4;
            } else {
                this.currentInterval = 7;
            }
            this.nextTickThreshold = this.currentInterval;

            for (Inventory inv : this.activeInventories) {
                this.renderTape(inv, this.shiftIndex);
            }

            float pitch = 0.6f + ((float) this.step / (float) MAX_STEPS) * 0.9f;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, pitch);
            }
        } else {
            --this.nextTickThreshold;
        }
    }

    private void concludeWin() {
        this.finished = true;

        for (Inventory inv : this.activeInventories) {
            this.fillBorders(inv, Material.LIME_STAINED_GLASS_PANE);
            inv.setItem(SELECTOR_SLOT, this.winningPrize);
        }

        String prizeTitle = this.winningPrize.hasItemMeta() && this.winningPrize.getItemMeta().hasDisplayName()
                ? PlexText.colorize(this.winningPrize.getItemMeta().getDisplayName())
                : this.winningPrize.getType().name().replace("_", " ");

        Title winTitle = Title.title(
                PlexText.component("&#ffd200&l★ WINNER! ★"),
                PlexText.component("&#00f2fe" + this.winner.getName() + " &#ffffffwon &#ffd200" + prizeTitle),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(600))
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(winTitle);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);
        }

        // Close inventories after 2.5 seconds
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getOpenInventory().getTopInventory().getHolder() instanceof RouletteTapeMenu) {
                    p.closeInventory();
                }
            }
            this.activeInventories.clear();
        }, 50L);

        this.plugin.getEngine().onAnimationCompleted(this.winner, this.winningPrize);
        this.cancel();
    }

    @Override
    public void cancelAnimation() {
        this.finished = true;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof RouletteTapeMenu) {
                p.closeInventory();
            }
        }
        this.activeInventories.clear();
        try {
            this.cancel();
        } catch (Exception ignored) {}
    }
}
