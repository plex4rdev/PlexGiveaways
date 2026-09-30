package com.plex.giveaways.reward;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.util.PlexText;
import com.plex.giveaways.util.TimeFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class VoucherService {
    private final PlexGiveaways plugin;
    private final File storageFile;
    private final NamespacedKey voucherIdKey;
    private final NamespacedKey expiryKey;
    private final Map<UUID, ServerVoucher> registry = new ConcurrentHashMap<>();

    public record ServerVoucher(UUID id, String command, long expiresAt, int durationMinutes) {}

    public VoucherService(PlexGiveaways plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "vouchers.yml");
        this.voucherIdKey = new NamespacedKey(plugin, "plex_voucher_id");
        this.expiryKey = new NamespacedKey(plugin, "plex_expiry");
        this.load();
    }

    public synchronized void load() {
        this.registry.clear();
        if (!this.storageFile.exists()) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(this.storageFile), StandardCharsets.UTF_8)) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(reader);
            ConfigurationSection sec = cfg.getConfigurationSection("tokens");
            if (sec != null) {
                long now = System.currentTimeMillis();
                for (String key : sec.getKeys(false)) {
                    try {
                        UUID id = UUID.fromString(key);
                        String cmd = sec.getString(key + ".command");
                        long exp = sec.getLong(key + ".expiry");
                        int dur = sec.getInt(key + ".duration", 10);
                        if (cmd != null && (exp <= 0 || exp > now)) {
                            this.registry.put(id, new ServerVoucher(id, cmd, exp, dur));
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to load vouchers.yml: " + e.getMessage());
        }
    }

    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            synchronized (this) {
                try {
                    FileConfiguration cfg = new YamlConfiguration();
                    for (Map.Entry<UUID, ServerVoucher> entry : this.registry.entrySet()) {
                        String path = "tokens." + entry.getKey().toString();
                        cfg.set(path + ".command", entry.getValue().command());
                        cfg.set(path + ".expiry", entry.getValue().expiresAt());
                        cfg.set(path + ".duration", entry.getValue().durationMinutes());
                    }
                    cfg.save(this.storageFile);
                } catch (Exception e) {
                    this.plugin.getLogger().log(Level.WARNING, "Failed to save vouchers.yml: " + e.getMessage());
                }
            }
        });
    }

    public ItemStack createVoucherItem(String command, int durationMinutes) {
        UUID id = UUID.randomUUID();
        long expiry = durationMinutes > 0
                ? System.currentTimeMillis() + ((long) durationMinutes * 60L * 1000L)
                : -1L;

        ServerVoucher voucher = new ServerVoucher(id, command, expiry, durationMinutes);
        this.registry.put(id, voucher);
        this.saveAsync();

        String matName = this.plugin.getConfig().getString("vouchers.item", "NETHER_STAR");
        Material mat = Material.getMaterial(matName.toUpperCase());
        if (mat == null || mat == Material.AIR) {
            mat = Material.NETHER_STAR;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(PlexText.component("&#00d2ff&l✦ &#00f2fe&lMYSTIC REWARD TOKEN &#00d2ff&l✦"));

            List<Component> lore = new ArrayList<>();
            List<String> rawLore = this.plugin.getConfig().getStringList("vouchers.lore-format");
            if (rawLore.isEmpty()) {
                rawLore = Arrays.asList(
                        "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                        "&#718096Right-click to claim your exclusive server reward.",
                        "",
                        "&#00f2fe⏱ &#ffffffExpires in: &#ffd200%duration%",
                        "&#00d2ff▶ &#ffffffClick with main-hand to redeem",
                        "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                );
            }

            String durationStr = durationMinutes > 0 ? durationMinutes + "m" : "Never";
            for (String line : rawLore) {
                lore.add(PlexText.component(PlexText.safeReplace(line, "%duration%", durationStr)));
            }
            meta.lore(lore);

            meta.getPersistentDataContainer().set(this.voucherIdKey, PersistentDataType.STRING, id.toString());
            if (expiry > 0) {
                meta.getPersistentDataContainer().set(this.expiryKey, PersistentDataType.LONG, expiry);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    public ServerVoucher claimVoucher(UUID id) {
        if (id == null) return null;
        ServerVoucher sv = this.registry.remove(id);
        if (sv != null) {
            this.saveAsync();
        }
        return sv;
    }

    public NamespacedKey getVoucherIdKey() {
        return voucherIdKey;
    }

    public NamespacedKey getExpiryKey() {
        return expiryKey;
    }
}
