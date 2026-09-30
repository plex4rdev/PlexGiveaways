package com.plex.giveaways.animation.impl;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.animation.GiveawayAnimation;
import com.plex.giveaways.util.PlexText;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class QuickPulseAnimation extends GiveawayAnimation {
    private final PlexGiveaways plugin;
    private final Player player;
    private final List<ItemStack> prizePool;
    private final ItemStack winningItem;
    private int tick = 0;

    public QuickPulseAnimation(PlexGiveaways plugin, Player player, List<ItemStack> prizePool) {
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

        if (this.tick == 1) {
            this.showCountdown("&#00d2ff&l🎲 ROLLING.");
            this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.0f);
        } else if (this.tick == 15) {
            this.showCountdown("&#00d2ff&l🎲 ROLLING..");
            this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.2f);
        } else if (this.tick == 30) {
            this.showCountdown("&#00d2ff&l🎲 ROLLING...");
            this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
        } else if (this.tick >= 45) {
            this.concludeWin();
        }
    }

    private void showCountdown(String title) {
        Title t = Title.title(
                PlexText.component(title),
                PlexText.component("&#a0aec0Selecting lucky winner..."),
                Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(800), Duration.ofMillis(100))
        );
        this.player.showTitle(t);
    }

    private void concludeWin() {
        String prizeName = this.winningItem.hasItemMeta() && this.winningItem.getItemMeta().hasDisplayName()
                ? PlexText.colorize(this.winningItem.getItemMeta().getDisplayName())
                : this.winningItem.getType().name().replace("_", " ");

        Title title = Title.title(
                PlexText.component("&#ffd200&l★ WINNER! ★"),
                PlexText.component("&#ffffffYou won &#00f2fe&l" + prizeName),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(2500), Duration.ofMillis(500))
        );
        this.player.showTitle(title);

        this.player.playSound(this.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        this.player.playSound(this.player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        this.player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, this.player.getLocation().add(0, 1.2, 0), 25, 0.4, 0.4, 0.4, 0.1);

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
