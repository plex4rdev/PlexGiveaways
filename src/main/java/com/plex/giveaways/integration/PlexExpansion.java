package com.plex.giveaways.integration;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.util.TimeFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlexExpansion extends PlaceholderExpansion {
    private final PlexGiveaways plugin;

    public PlexExpansion(PlexGiveaways plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "plexgiveaways";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Plex";
    }

    @Override
    public @NotNull String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "status" -> this.plugin.getEngine().isActive() ? "Active" : "Idle";
            case "winner" -> this.plugin.getEngine().getLastWinnerName();
            case "reward", "prize" -> this.plugin.getEngine().getLastPrizeName();
            case "time", "timer" -> TimeFormatter.formatDigital(this.plugin.getScheduler().getSecondsUntilNext());
            default -> null;
        };
    }
}
