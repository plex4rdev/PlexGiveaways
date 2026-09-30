package com.plex.giveaways.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.animation.impl.ChestVortexAnimation;
import com.plex.giveaways.animation.impl.CrownShuffleAnimation;
import com.plex.giveaways.animation.impl.QuickPulseAnimation;
import com.plex.giveaways.animation.impl.RouletteTapeAnimation;

public class AnimationRegistry {
    private final PlexGiveaways plugin;
    private final Random random = new Random();

    public record AnimationDescriptor(String id, String title, Material icon, boolean enabled, int weight) {}

    public AnimationRegistry(PlexGiveaways plugin) {
        this.plugin = plugin;
    }

    public GiveawayAnimation create(String key, Player winner, List<ItemStack> prizes) {
        String normalized = key != null ? key.trim().toLowerCase() : "roulette";

        return switch (normalized) {
            case "vortex" -> new ChestVortexAnimation(this.plugin, winner, prizes);
            case "overhead", "crown", "head", "head_shuffle" -> new CrownShuffleAnimation(this.plugin, winner, prizes);
            case "instant", "pulse", "none", "title", "simple" -> new QuickPulseAnimation(this.plugin, winner, prizes);
            default -> new RouletteTapeAnimation(this.plugin, winner, prizes);
        };
    }

    public String selectWeightedKey() {
        List<AnimationDescriptor> active = new ArrayList<>();
        int totalWeight = 0;

        for (AnimationDescriptor desc : this.getRegisteredAnimations()) {
            if (desc.enabled() && desc.weight() > 0) {
                active.add(desc);
                totalWeight += desc.weight();
            }
        }

        if (active.isEmpty() || totalWeight <= 0) {
            return "roulette";
        }

        int roll = this.random.nextInt(totalWeight);
        int cursor = 0;
        for (AnimationDescriptor desc : active) {
            cursor += desc.weight();
            if (roll < cursor) {
                return desc.id();
            }
        }

        return active.get(0).id();
    }

    public List<AnimationDescriptor> getRegisteredAnimations() {
        List<AnimationDescriptor> list = new ArrayList<>();
        FileConfiguration config = this.plugin.getConfig();
        ConfigurationSection sec = config.getConfigurationSection("animations");

        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                boolean enabled = sec.getBoolean(id + ".enabled", true);
                int weight = sec.getInt(id + ".weight", 25);
                String title = sec.getString(id + ".display", id.toUpperCase());
                String iconName = sec.getString(id + ".icon", "NETHER_STAR");
                Material mat = Material.getMaterial(iconName.toUpperCase());
                if (mat == null) mat = Material.NETHER_STAR;

                list.add(new AnimationDescriptor(id.toLowerCase(), title, mat, enabled, weight));
            }
        }

        if (list.isEmpty()) {
            list.add(new AnimationDescriptor("roulette", "&#00d2ff&lUI Roulette Wheel", Material.HOPPER, true, 35));
            list.add(new AnimationDescriptor("vortex", "&#ffffff&lParticle Vortex", Material.NETHER_STAR, true, 35));
            list.add(new AnimationDescriptor("overhead", "&#ffd200&lOverhead Crown Shuffle", Material.GOLDEN_HELMET, true, 15));
            list.add(new AnimationDescriptor("instant", "&#00f2fe&lInstant Roll", Material.CLOCK, true, 15));
        }

        return list;
    }

    public void setEnabled(String id, boolean enabled) {
        this.plugin.getConfig().set("animations." + id.toLowerCase() + ".enabled", enabled);
        this.plugin.saveConfig();
    }

    public void setWeight(String id, int weight) {
        int clamped = Math.max(0, Math.min(100, weight));
        this.plugin.getConfig().set("animations." + id.toLowerCase() + ".weight", clamped);
        this.plugin.saveConfig();
    }
}
