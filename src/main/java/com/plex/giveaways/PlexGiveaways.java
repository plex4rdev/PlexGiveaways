package com.plex.giveaways;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.plex.giveaways.animation.AnimationRegistry;
import com.plex.giveaways.command.PlexGiveawayCommand;
import com.plex.giveaways.core.GiveawayEngine;
import com.plex.giveaways.integration.DiscordDispatch;
import com.plex.giveaways.integration.PlexExpansion;
import com.plex.giveaways.listener.PlayerSessionListener;
import com.plex.giveaways.listener.VoucherRedeemListener;
import com.plex.giveaways.reward.RewardPoolService;
import com.plex.giveaways.reward.VoucherService;
import com.plex.giveaways.schedule.AutomatedScheduler;
import com.plex.giveaways.ui.LiveStatusBar;
import com.plex.giveaways.ui.MenuBase;
import com.plex.giveaways.ui.PlexMenuListener;
import com.plex.giveaways.util.PlexText;

public final class PlexGiveaways extends JavaPlugin {
    private static PlexGiveaways instance;

    private GiveawayEngine engine;
    private AnimationRegistry animations;
    private RewardPoolService rewardPools;
    private VoucherService vouchers;
    private AutomatedScheduler scheduler;
    private LiveStatusBar displayBar;
    private DiscordDispatch discord;
    private PlexExpansion expansion;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Ensure default configurations exist
        this.saveDefaultConfig();
        this.saveResource("messages.yml", false);
        this.saveResource("prizes.yml", false);
        this.saveResource("schedule.yml", false);
        this.saveResource("webhooks.yml", false);

        this.loadMessages();

        // Initialize Services
        this.engine = new GiveawayEngine(this);
        this.animations = new AnimationRegistry(this);
        this.vouchers = new VoucherService(this);
        this.rewardPools = new RewardPoolService(this);
        this.displayBar = new LiveStatusBar(this);
        this.discord = new DiscordDispatch(this);
        this.scheduler = new AutomatedScheduler(this);

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VoucherRedeemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlexMenuListener(this), this);

        // Register Command
        PluginCommand cmd = this.getCommand("giveaway");
        if (cmd != null) {
            PlexGiveawayCommand executor = new PlexGiveawayCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // PlaceholderAPI Expansion Hook
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.expansion = new PlexExpansion(this);
            this.expansion.register();
            this.getLogger().info("Successfully hooked into PlaceholderAPI.");
        }

        this.getLogger().info("PlexGiveaways v" + this.getDescription().getVersion() + " enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Unregister PlaceholderAPI
        if (this.expansion != null) {
            try {
                this.expansion.unregister();
            } catch (Exception ignored) {}
            this.expansion = null;
        }

        // Abort ongoing giveaway & cleanup entities
        if (this.engine != null) {
            this.engine.abortGiveaway();
            this.engine.cleanupAllEntities();
        }

        // Cancel scheduler
        if (this.scheduler != null) {
            this.scheduler.cancelSchedule();
        }

        // Remove BossBars
        if (this.displayBar != null) {
            this.displayBar.hideAll();
        }

        // Close open Plex menus
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof MenuBase) {
                p.closeInventory();
            }
        }

        this.getLogger().info("PlexGiveaways disabled and cleaned up.");
    }

    public void reloadAll() {
        this.reloadConfig();
        this.loadMessages();
        if (this.rewardPools != null) this.rewardPools.loadPools();
        if (this.vouchers != null) this.vouchers.load();
        if (this.discord != null) this.discord.reload();
        if (this.scheduler != null) this.scheduler.reload();
    }

    private void loadMessages() {
        this.messagesFile = new File(this.getDataFolder(), "messages.yml");
        if (!this.messagesFile.exists()) {
            this.saveResource("messages.yml", false);
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(this.messagesFile), StandardCharsets.UTF_8)) {
            this.messagesConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            this.getLogger().log(Level.WARNING, "Failed to load messages.yml: " + e.getMessage());
            this.messagesConfig = new YamlConfiguration();
        }
    }

    public String getMessage(String path, String fallback) {
        if (this.messagesConfig == null) {
            return PlexText.colorize(fallback);
        }
        String val = this.messagesConfig.getString(path, fallback);
        return PlexText.colorize(val);
    }

    public static PlexGiveaways getInstance() {
        return instance;
    }

    public GiveawayEngine getEngine() {
        return engine;
    }

    public AnimationRegistry getAnimations() {
        return animations;
    }

    public RewardPoolService getRewardPools() {
        return rewardPools;
    }

    public VoucherService getVouchers() {
        return vouchers;
    }

    public AutomatedScheduler getScheduler() {
        return scheduler;
    }

    public LiveStatusBar getDisplayBar() {
        return displayBar;
    }

    public DiscordDispatch getDiscord() {
        return discord;
    }
}
