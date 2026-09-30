package com.plex.giveaways.animation.impl;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.animation.GiveawayAnimation;
import com.plex.giveaways.util.PlexText;

import net.kyori.adventure.title.Title;

public class ChestVortexAnimation extends GiveawayAnimation {
    private final PlexGiveaways plugin;
    private final Player player;
    private final List<ItemStack> prizePool;
    private final ItemStack winningItem;
    private int tick = 0;
    private static final int DURATION = 70;

    public ChestVortexAnimation(PlexGiveaways plugin, Player player, List<ItemStack> prizePool) {
        this.plugin = plugin;
        this.player = player;
        this.prizePool = (prizePool != null && !prizePool.isEmpty())
                ? prizePool
                : Collections.singletonList(new ItemStack(Material.DIAMOND));
        this.winningItem = this.prizePool.get(new Random().nextInt(this.prizePool.size())).clone();
    }

    @Override
    public void run() {
        if (!this.player.isOnline()) {
            this.cancelAnimation();
            return;
        }

        ++this.tick;
        double progress = (double) this.tick / (double) DURATION;

        if (this.tick < DURATION) {
            Location base = this.player.getLocation();
            double radius = 2.8 * (1.0 - progress);
            double height = 2.2 - (progress * 1.0); // Descends from head to chest

            int strands = 3;
            double baseAngle = this.tick * 0.35 + (progress * 5.0);

            for (int s = 0; s < strands; ++s) {
                double angle = baseAngle + (s * (2.0 * Math.PI / strands));
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);

                Location pLoc = base.clone().add(x, height, z);
                this.player.getWorld().spawnParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0, 0.01);
                this.player.getWorld().spawnParticle(Particle.FIREWORK, pLoc, 1, 0, 0, 0, 0.0);
            }

            if (this.tick % 5 == 0) {
                float pitch = 0.5f + (float) (progress * 1.3);
                this.player.playSound(this.player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, pitch);
            }
        } else {
            this.concludeWin();
        }
    }

    private void concludeWin() {
        Location chestLoc = this.player.getLocation().add(0.0, 1.2, 0.0);

        // Volumetric burst
        this.player.getWorld().spawnParticle(Particle.EXPLOSION, chestLoc, 1);
        this.player.getWorld().spawnParticle(Particle.END_ROD, chestLoc, 30, 0.3, 0.3, 0.3, 0.15);
        this.player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, chestLoc, 25, 0.4, 0.4, 0.4, 0.1);

        this.player.playSound(this.player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        this.player.playSound(this.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);

        String prizeName = this.winningItem.hasItemMeta() && this.winningItem.getItemMeta().hasDisplayName()
                ? PlexText.colorize(this.winningItem.getItemMeta().getDisplayName())
                : this.winningItem.getType().name().replace("_", " ");

        Title title = Title.title(
                PlexText.component("&#00d2ff&l✦ REWARD INFUSED ✦"),
                PlexText.component("&#ffffffYou absorbed &#00f2fe&l" + prizeName),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(500))
        );
        this.player.showTitle(title);

        this.plugin.getEngine().onAnimationCompleted(this.player, this.winningItem);
        this.cancelAnimation();
    }

    @Override
    public void cancelAnimation() {
        try {
            this.cancel();
        } catch (Exception ignored) {}
    }
}
