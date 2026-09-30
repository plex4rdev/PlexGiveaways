package com.plex.giveaways.schedule;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.util.PlexText;
import com.plex.giveaways.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class AutomatedScheduler {
    private final PlexGiveaways plugin;
    private final File file;
    private FileConfiguration config;
    private BukkitTask activeTask;

    private boolean enabled = true;
    private long intervalSeconds = 1800L;
    private int minPlayers = 2;
    private List<Integer> countdownSeconds = Arrays.asList(60, 30, 15, 10, 5, 3, 2, 1);
    private long secondsRemaining = 1800L;

    public AutomatedScheduler(PlexGiveaways plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "schedule.yml");
        this.reload();
    }

    public synchronized void reload() {
        if (this.activeTask != null && !this.activeTask.isCancelled()) {
            this.activeTask.cancel();
            this.activeTask = null;
        }

        if (!this.file.exists()) {
            this.plugin.saveResource("schedule.yml", false);
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(this.file), StandardCharsets.UTF_8)) {
            this.config = YamlConfiguration.loadConfiguration(reader);
            this.enabled = this.config.getBoolean("scheduler.enabled", true);
            this.intervalSeconds = this.config.getLong("scheduler.interval-seconds", 1800L);
            this.minPlayers = this.config.getInt("scheduler.min-players", 2);
            this.countdownSeconds = this.config.getIntegerList("scheduler.countdown-announcements");
            if (this.countdownSeconds.isEmpty()) {
                this.countdownSeconds = Arrays.asList(60, 30, 15, 10, 5, 3, 2, 1);
            }
            this.secondsRemaining = this.intervalSeconds;
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to load schedule.yml: " + e.getMessage());
        }

        this.startScheduleTask();
    }

    private synchronized void startScheduleTask() {
        if (!this.enabled || this.intervalSeconds <= 0) {
            return;
        }

        this.activeTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (this.plugin.getEngine().isActive()) {
                return;
            }

            --this.secondsRemaining;

            // Broadcast countdown announcements
            if (this.countdownSeconds.contains((int) this.secondsRemaining)) {
                String timeText = TimeFormatter.formatDuration(this.secondsRemaining);
                String msg = PlexText.colorize("&#00d2ff&l❖ GIVEAWAY COUNTDOWN &8» &#ffffffAutomated event starts in &#ffd200" + timeText + "&#ffffff!");
                Bukkit.broadcastMessage(msg);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
                }
            }

            // Update countdown BossBar
            this.plugin.getDisplayBar().showCountdownBar(this.secondsRemaining, this.intervalSeconds);

            if (this.secondsRemaining <= 0) {
                this.secondsRemaining = this.intervalSeconds;

                if (Bukkit.getOnlinePlayers().size() < this.minPlayers) {
                    Bukkit.broadcastMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#ff4757Skipping scheduled giveaway; insufficient players online."));
                    return;
                }

                this.plugin.getEngine().launchGiveaway(null, null);
            }
        }, 20L, 20L);
    }

    public long getSecondsUntilNext() {
        return Math.max(0L, this.secondsRemaining);
    }

    public void setInterval(long seconds) {
        this.intervalSeconds = Math.max(0L, seconds);
        this.secondsRemaining = this.intervalSeconds;
        if (this.config != null) {
            this.config.set("scheduler.interval-seconds", this.intervalSeconds);
            try {
                this.config.save(this.file);
            } catch (Exception ignored) {}
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (this.config != null) {
            this.config.set("scheduler.enabled", this.enabled);
            try {
                this.config.save(this.file);
            } catch (Exception ignored) {}
        }
        this.reload();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public void cancelSchedule() {
        if (this.activeTask != null && !this.activeTask.isCancelled()) {
            this.activeTask.cancel();
            this.activeTask = null;
        }
    }
}
