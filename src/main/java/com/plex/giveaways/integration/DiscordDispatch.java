package com.plex.giveaways.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plex.giveaways.PlexGiveaways;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.logging.Level;

public class DiscordDispatch {
    private final PlexGiveaways plugin;
    private final File file;
    private FileConfiguration config;

    public DiscordDispatch(PlexGiveaways plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "webhooks.yml");
        this.reload();
    }

    public synchronized void reload() {
        if (!this.file.exists()) {
            this.plugin.saveResource("webhooks.yml", false);
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(this.file), StandardCharsets.UTF_8)) {
            this.config = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to load webhooks.yml: " + e.getMessage());
            this.config = new YamlConfiguration();
        }
    }

    public void dispatch(String eventType, String winnerName, String prizeName, int participantCount) {
        if (this.config == null || !this.config.getBoolean("discord.enabled", false)) {
            return;
        }

        String webhookUrl = this.config.getString("discord.url");
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || webhookUrl.contains("YOUR_WEBHOOK_URL")) {
            return;
        }

        String path = "embeds." + eventType + ".";
        String title = this.config.getString(path + "title", "PlexGiveaways Event");
        String desc = this.config.getString(path + "description", "Giveaway status updated.")
                .replace("%winner%", winnerName != null ? winnerName : "Unknown")
                .replace("%player%", winnerName != null ? winnerName : "Unknown")
                .replace("%reward%", prizeName != null ? prizeName : "Mystery Reward")
                .replace("%prize%", prizeName != null ? prizeName : "Mystery Reward")
                .replace("%entry_count%", String.valueOf(participantCount))
                .replace("\\n", "\n");

        int color = this.config.getInt(path + "color", 53951);
        boolean showThumb = this.config.getBoolean(path + "thumbnail", true);

        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("description", desc);
        embed.addProperty("color", color);
        embed.addProperty("timestamp", Instant.now().toString());

        JsonObject footer = new JsonObject();
        footer.addProperty("text", "PlexGiveaways Engine");
        embed.add("footer", footer);

        if (showThumb && winnerName != null) {
            JsonObject thumb = new JsonObject();
            thumb.addProperty("url", "https://minotar.net/helm/" + winnerName + "/100.png");
            embed.add("thumbnail", thumb);
        }

        JsonArray embeds = new JsonArray();
        embeds.add(embed);

        JsonObject root = new JsonObject();
        root.add("embeds", embeds);

        final String payload = root.toString();

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("User-Agent", "PlexGiveaways-DiscordDispatcher");
                con.setDoOutput(true);

                try (OutputStream os = con.getOutputStream()) {
                    byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
                    os.write(bytes, 0, bytes.length);
                }

                int code = con.getResponseCode();
                if (code == 429) {
                    this.plugin.getLogger().warning("Discord webhook rate-limited (HTTP 429).");
                } else if (code < 200 || code >= 300) {
                    this.plugin.getLogger().warning("Discord webhook returned HTTP code: " + code);
                }
                con.disconnect();
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }
}
