package com.plex.giveaways.reward;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.util.PlexText;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class RewardPoolService {
    private final PlexGiveaways plugin;
    private final File file;
    private FileConfiguration config;

    public RewardPoolService(PlexGiveaways plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "prizes.yml");
        this.loadPools();
    }

    public synchronized void loadPools() {
        if (!this.file.exists()) {
            this.generateDefaultPools();
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(this.file), StandardCharsets.UTF_8)) {
            this.config = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to load prizes.yml: " + e.getMessage());
            this.config = new YamlConfiguration();
        }

        boolean dirty = false;
        if (!this.config.contains("pools.roulette.items")) {
            this.setupRouletteDefaults();
            dirty = true;
        }
        if (!this.config.contains("pools.vortex.items")) {
            this.setupVortexDefaults();
            dirty = true;
        }
        if (!this.config.contains("pools.overhead.items")) {
            this.setupOverheadDefaults();
            dirty = true;
        }
        if (!this.config.contains("pools.instant.items")) {
            this.setupInstantDefaults();
            dirty = true;
        }

        if (dirty) {
            this.savePoolsAsync();
        }
    }

    private void generateDefaultPools() {
        try {
            if (this.file.getParentFile() != null && !this.file.getParentFile().exists()) {
                this.file.getParentFile().mkdirs();
            }
            this.config = new YamlConfiguration();
            this.config.setComments("pools", Arrays.asList(
                    "====================================================================",
                    "                   PLEXGIVEAWAYS REWARD POOLS",
                    "====================================================================",
                    "Configure item rewards and command vouchers for each giveaway animation.",
                    "Items retain 100% of their CustomModelData, potions, enchants, and components.",
                    "Commands will be awarded as server-side verified vouchers."
            ));
            this.setupRouletteDefaults();
            this.setupVortexDefaults();
            this.setupOverheadDefaults();
            this.setupInstantDefaults();
            this.config.save(this.file);
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not initialize prizes.yml: " + e.getMessage());
        }
    }

    private void setupRouletteDefaults() {
        List<ItemStack> items = new ArrayList<>();
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.displayName(PlexText.component("&#00d2ff&l✦ Mythic Netherite Blade ✦"));
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            sword.setItemMeta(meta);
        }
        items.add(sword);
        items.add(new ItemStack(Material.TOTEM_OF_UNDYING, 2));
        items.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 5));
        items.add(new ItemStack(Material.NETHERITE_INGOT, 4));
        this.config.set("pools.roulette.items", items);

        List<String> cmds = Arrays.asList(
                "give %player% diamond 32",
                "give %player% emerald 64",
                "eco give %player% 5000"
        );
        this.config.set("pools.roulette.commands", cmds);
    }

    private void setupVortexDefaults() {
        List<ItemStack> items = new ArrayList<>();
        ItemStack pick = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = pick.getItemMeta();
        if (meta != null) {
            meta.displayName(PlexText.component("&#ffd200&l✦ Celestial Drill ✦"));
            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            meta.addEnchant(Enchantment.FORTUNE, 3, true);
            pick.setItemMeta(meta);
        }
        items.add(pick);
        items.add(new ItemStack(Material.ELYTRA, 1));
        items.add(new ItemStack(Material.BEACON, 1));
        items.add(new ItemStack(Material.NETHER_STAR, 2));
        this.config.set("pools.vortex.items", items);

        List<String> cmds = Arrays.asList(
                "give %player% netherite_scrap 8",
                "give %player% experience_bottle 64",
                "eco give %player% 7500"
        );
        this.config.set("pools.vortex.commands", cmds);
    }

    private void setupOverheadDefaults() {
        List<ItemStack> items = new ArrayList<>();
        ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta meta = helmet.getItemMeta();
        if (meta != null) {
            meta.displayName(PlexText.component("&#ffd200&l✦ Crown of the Victor ✦"));
            meta.addEnchant(Enchantment.PROTECTION, 4, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            helmet.setItemMeta(meta);
        }
        items.add(helmet);
        items.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        items.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3));
        items.add(new ItemStack(Material.DIAMOND_BLOCK, 4));
        this.config.set("pools.overhead.items", items);

        List<String> cmds = Arrays.asList(
                "give %player% netherite_ingot 4",
                "give %player% emerald_block 8",
                "eco give %player% 6000"
        );
        this.config.set("pools.overhead.commands", cmds);
    }

    private void setupInstantDefaults() {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(Material.DIAMOND, 64));
        items.add(new ItemStack(Material.EMERALD, 64));
        items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        items.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
        this.config.set("pools.instant.items", items);

        List<String> cmds = Arrays.asList(
                "give %player% diamond 48",
                "eco give %player% 3000"
        );
        this.config.set("pools.instant.commands", cmds);
    }

    public List<ItemStack> getPoolPrizes(String poolName) {
        String key = poolName != null ? poolName.toLowerCase() : "roulette";
        List<ItemStack> list = new ArrayList<>();

        if (this.config != null && this.config.contains("pools." + key + ".items")) {
            List<?> raw = this.config.getList("pools." + key + ".items");
            if (raw != null) {
                for (Object obj : raw) {
                    if (obj instanceof ItemStack stack) {
                        list.add(stack.clone());
                    }
                }
            }
        }

        if (this.config != null && this.config.contains("pools." + key + ".commands")) {
            List<String> cmds = this.config.getStringList("pools." + key + ".commands");
            int dur = this.plugin.getConfig().getInt("vouchers.default-expiration-minutes", 10);
            for (String cmd : cmds) {
                list.add(this.plugin.getVouchers().createVoucherItem(cmd, dur));
            }
        }

        if (list.isEmpty()) {
            list.add(new ItemStack(Material.DIAMOND, 1));
        }

        return list;
    }

    public void savePoolFromInventory(String poolName, Inventory inv) {
        String key = poolName != null ? poolName.toLowerCase() : "roulette";
        List<ItemStack> items = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        for (int slot = 0; slot < inv.getSize(); ++slot) {
            if (slot == 45 || slot == 53) continue; // Skip Cancel and Save dye controls
            ItemStack it = inv.getItem(slot);
            if (it == null || it.getType() == Material.AIR) continue;

            ItemMeta m = it.getItemMeta();
            if (m != null && m.getPersistentDataContainer().has(this.plugin.getVouchers().getVoucherIdKey(), org.bukkit.persistence.PersistentDataType.STRING)) {
                String idStr = m.getPersistentDataContainer().get(this.plugin.getVouchers().getVoucherIdKey(), org.bukkit.persistence.PersistentDataType.STRING);
                try {
                    UUID id = UUID.fromString(idStr);
                    var sv = this.plugin.getVouchers().claimVoucher(id);
                    if (sv != null) {
                        commands.add(sv.command());
                        continue;
                    }
                } catch (Exception ignored) {}
            }
            items.add(it.clone());
        }

        this.config.set("pools." + key + ".items", items);
        this.config.set("pools." + key + ".commands", commands);
        this.savePoolsAsync();
    }

    public void addCommandReward(String poolName, String command) {
        String key = poolName != null ? poolName.toLowerCase() : "roulette";
        List<String> list = this.config.getStringList("pools." + key + ".commands");
        list.add(command);
        this.config.set("pools." + key + ".commands", list);
        this.savePoolsAsync();
    }

    public void savePoolsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            synchronized (this) {
                try {
                    this.config.save(this.file);
                } catch (Exception e) {
                    this.plugin.getLogger().log(Level.WARNING, "Failed to save prizes.yml: " + e.getMessage());
                }
            }
        });
    }
}
