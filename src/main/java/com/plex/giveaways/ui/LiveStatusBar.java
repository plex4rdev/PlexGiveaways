package com.plex.giveaways.ui;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.util.PlexText;
import com.plex.giveaways.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class LiveStatusBar {
    private final PlexGiveaways plugin;
    private BossBar activeBar;

    public LiveStatusBar(PlexGiveaways plugin) {
        this.plugin = plugin;
    }

    public synchronized void showRollingBar() {
        this.hideAll();
        if (!this.plugin.getConfig().getBoolean("display-bar.active.enabled", true)) {
            return;
        }

        String rawTitle = this.plugin.getConfig().getString("display-bar.active.text", "&#00d2ff&l⚡ PLEX GIVEAWAY &8» &#ffffffSelecting Winner...");
        BarColor color = this.parseColor(this.plugin.getConfig().getString("display-bar.active.color", "BLUE"));
        BarStyle style = this.parseStyle(this.plugin.getConfig().getString("display-bar.active.style", "SOLID"));

        this.activeBar = Bukkit.createBossBar(PlexText.colorize(rawTitle), color, style);
        this.activeBar.setProgress(1.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.activeBar.addPlayer(p);
        }
    }

    public synchronized void showWinnerBar(String winnerName, String prizeName) {
        if (this.activeBar == null) {
            String rawTitle = this.plugin.getConfig().getString("display-bar.winner.text", "&#ffd200&l★ CONGRATULATIONS &8» &#00f2fe%winner% &#ffffffwon &#ffd200%reward%!");
            BarColor color = this.parseColor(this.plugin.getConfig().getString("display-bar.winner.color", "YELLOW"));
            BarStyle style = this.parseStyle(this.plugin.getConfig().getString("display-bar.winner.style", "SOLID"));
            this.activeBar = Bukkit.createBossBar(PlexText.colorize(rawTitle), color, style);
            for (Player p : Bukkit.getOnlinePlayers()) {
                this.activeBar.addPlayer(p);
            }
        }

        String title = this.plugin.getConfig().getString("display-bar.winner.text", "&#ffd200&l★ CONGRATULATIONS &8» &#00f2fe%winner% &#ffffffwon &#ffd200%reward%!")
                .replace("%winner%", winnerName)
                .replace("%reward%", prizeName);

        this.activeBar.setTitle(PlexText.colorize(title));
        this.activeBar.setColor(BarColor.YELLOW);
        this.activeBar.setProgress(1.0);

        Bukkit.getScheduler().runTaskLater(this.plugin, this::hideAll, 140L); // Auto-hide after 7 seconds
    }

    public synchronized void showCountdownBar(long secondsLeft, long totalSeconds) {
        if (!this.plugin.getConfig().getBoolean("display-bar.active.enabled", true)) {
            return;
        }

        String title = "&#00d2ff&l❖ NEXT GIVEAWAY &8» &#00f2fe" + TimeFormatter.formatDigital(secondsLeft);
        if (this.activeBar == null) {
            this.activeBar = Bukkit.createBossBar(PlexText.colorize(title), BarColor.BLUE, BarStyle.SOLID);
            for (Player p : Bukkit.getOnlinePlayers()) {
                this.activeBar.addPlayer(p);
            }
        } else {
            this.activeBar.setTitle(PlexText.colorize(title));
        }

        double progress = totalSeconds > 0 ? (double) secondsLeft / (double) totalSeconds : 1.0;
        this.activeBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    public synchronized void addPlayer(Player player) {
        if (this.activeBar != null && player != null) {
            this.activeBar.addPlayer(player);
        }
    }

    public synchronized void removePlayer(Player player) {
        if (this.activeBar != null && player != null) {
            this.activeBar.removePlayer(player);
        }
    }

    public synchronized void hideAll() {
        if (this.activeBar != null) {
            this.activeBar.removeAll();
            this.activeBar = null;
        }
    }

    private BarColor parseColor(String name) {
        try {
            return BarColor.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return BarColor.BLUE;
        }
    }

    private BarStyle parseStyle(String name) {
        try {
            return BarStyle.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return BarStyle.SOLID;
        }
    }
}
