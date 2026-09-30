package com.plex.giveaways.listener;

import com.plex.giveaways.PlexGiveaways;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSessionListener implements Listener {
    private final PlexGiveaways plugin;

    public PlayerSessionListener(PlexGiveaways plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (this.plugin.getEngine().isActive()) {
            this.plugin.getDisplayBar().addPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.getEngine().handlePlayerDisconnect(event.getPlayer());
        this.plugin.getDisplayBar().removePlayer(event.getPlayer());
    }
}
