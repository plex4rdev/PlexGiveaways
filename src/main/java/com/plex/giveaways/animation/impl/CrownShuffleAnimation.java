package com.plex.giveaways.animation.impl;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.animation.GiveawayAnimation;
import com.plex.giveaways.util.PlexText;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CrownShuffleAnimation extends GiveawayAnimation {
    private final PlexGiveaways plugin;
    private final Player player;
    private final List<ItemStack> prizePool;
    private final ItemStack winningItem;
    private final NamespacedKey entityMarker;
    private final Random random = new Random();

    private Entity displayEntity;
    private TextDisplay labelDisplay;

    private int tick = 0;
    private int lastShuffle = 0;
    private int itemIndex = 0;
    private double hoverHeight = 2.2;
    private boolean locked = false;

    private static final int SHUFFLE_LIMIT = 68;
    private static final int TOTAL_LIMIT = 90;

    public CrownShuffleAnimation(PlexGiveaways plugin, Player player, List<ItemStack> prizePool) {
        this.plugin = plugin;
        this.player = player;
        this.prizePool = (prizePool != null && !prizePool.isEmpty())
                ? new ArrayList<>(prizePool)
                : Collections.singletonList(new ItemStack(Material.DIAMOND));
        this.winningItem = this.prizePool.get(this.random.nextInt(this.prizePool.size())).clone();
        this.entityMarker = new NamespacedKey(plugin, "plex_entity");

        this.spawnEntities();
    }

    private void spawnEntities() {
        if (!this.player.isOnline()) return;

        Location spawnLoc = this.player.getLocation().add(0.0, this.hoverHeight, 0.0);
        ItemStack firstItem = this.prizePool.get(0);

        try {
            this.displayEntity = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
                d.setItemStack(firstItem);
                d.setBillboard(Display.Billboard.CENTER);
                d.setPersistent(false);
                d.getPersistentDataContainer().set(this.entityMarker, PersistentDataType.BYTE, (byte) 1);
            });

            this.labelDisplay = spawnLoc.getWorld().spawn(spawnLoc.clone().add(0.0, 0.65, 0.0), TextDisplay.class, t -> {
                t.text(PlexText.component("&#00d2ff&l🎲 SHUFFLING..."));
                t.setBillboard(Display.Billboard.CENTER);
                t.setPersistent(false);
                t.getPersistentDataContainer().set(this.entityMarker, PersistentDataType.BYTE, (byte) 1);
            });

            this.plugin.getEngine().registerTrackedEntity(this.displayEntity);
            this.plugin.getEngine().registerTrackedEntity(this.labelDisplay);
            return;
        } catch (Throwable ignored) {
            // Display classes not present (legacy fallback)
        }

        this.displayEntity = spawnLoc.getWorld().spawn(spawnLoc.clone().subtract(0.0, 1.2, 0.0), ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setPersistent(false);
            if (stand.getEquipment() != null) {
                stand.getEquipment().setHelmet(firstItem);
            }
            stand.getPersistentDataContainer().set(this.entityMarker, PersistentDataType.BYTE, (byte) 1);
        });
        this.plugin.getEngine().registerTrackedEntity(this.displayEntity);
    }

    @Override
    public void run() {
        if (!this.player.isOnline() || this.displayEntity == null || !this.displayEntity.isValid()) {
            this.cancelAnimation();
            return;
        }

        ++this.tick;

        // 1. Shuffling Phase
        if (this.tick < SHUFFLE_LIMIT) {
            int interval = (this.tick < 26) ? 2 : (this.tick < 44) ? 3 : (this.tick < 56) ? 5 : 7;

            if (this.tick - this.lastShuffle >= interval) {
                this.lastShuffle = this.tick;
                ItemStack nextItem = (this.tick >= 58)
                        ? this.winningItem
                        : this.prizePool.get(this.itemIndex % this.prizePool.size());
                ++this.itemIndex;

                this.applyItem(nextItem);

                float pitch = 0.8f + ((float) this.tick / (float) SHUFFLE_LIMIT) * 0.8f;
                this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, pitch);
                this.player.getWorld().spawnParticle(Particle.CRIT, this.displayEntity.getLocation().add(0, 0.2, 0), 2, 0.15, 0.15, 0.15, 0.02);
            }

            this.syncPosition(this.hoverHeight);
            return;
        }

        // 2. Lock-in Phase
        if (this.tick == SHUFFLE_LIMIT) {
            this.locked = true;
            this.applyItem(this.winningItem);

            String prizeName = this.winningItem.hasItemMeta() && this.winningItem.getItemMeta().hasDisplayName()
                    ? PlexText.colorize(this.winningItem.getItemMeta().getDisplayName())
                    : this.winningItem.getType().name().replace("_", " ");

            if (this.labelDisplay != null && this.labelDisplay.isValid()) {
                this.labelDisplay.text(PlexText.component("&#ffd200&l★ " + prizeName + " ★"));
            }

            Location loc = this.displayEntity.getLocation();
            this.player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 25, 0.25, 0.25, 0.25, 0.08);
            this.player.getWorld().spawnParticle(Particle.FIREWORK, loc, 15, 0.2, 0.2, 0.2, 0.05);

            this.player.playSound(this.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            this.player.playSound(this.player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.4f);

            Title title = Title.title(
                    PlexText.component("&#ffd200&l★ WINNER! ★"),
                    PlexText.component("&#ffffffYou won &#00f2fe&l" + prizeName),
                    Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(2000), Duration.ofMillis(400))
            );
            this.player.showTitle(title);

            this.syncPosition(this.hoverHeight);
            return;
        }

        // 3. Descent Phase
        if (this.tick < TOTAL_LIMIT) {
            double progress = (double) (this.tick - SHUFFLE_LIMIT) / (double) (TOTAL_LIMIT - SHUFFLE_LIMIT);
            this.hoverHeight = 2.2 - (progress * 1.3);

            this.syncPosition(this.hoverHeight);
            this.player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, this.displayEntity.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);

            if (this.labelDisplay != null && this.tick >= 78) {
                this.plugin.getEngine().unregisterTrackedEntity(this.labelDisplay.getUniqueId());
                this.labelDisplay.remove();
                this.labelDisplay = null;
            }
            return;
        }

        // 4. Conclude
        this.concludeWin();
    }

    private void applyItem(ItemStack item) {
        if (this.displayEntity instanceof ItemDisplay id) {
            id.setItemStack(item);
        } else if (this.displayEntity instanceof ArmorStand as) {
            if (as.getEquipment() != null) {
                as.getEquipment().setHelmet(item);
            }
        }
    }

    private void syncPosition(double offset) {
        if (this.displayEntity == null || !this.displayEntity.isValid()) return;
        double base = (this.displayEntity instanceof ArmorStand) ? (offset - 1.2) : offset;
        Location target = this.player.getLocation().add(0.0, base, 0.0);
        this.displayEntity.teleport(target);

        if (this.labelDisplay != null && this.labelDisplay.isValid()) {
            this.labelDisplay.teleport(this.player.getLocation().add(0.0, offset + 0.65, 0.0));
        }
    }

    private void concludeWin() {
        Location chest = this.player.getLocation().add(0.0, 1.0, 0.0);
        this.player.playSound(this.player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.9f);
        this.player.playSound(this.player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.6f);
        this.player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, chest, 20, 0.25, 0.25, 0.25, 0.05);

        this.cleanupEntities();
        this.plugin.getEngine().onAnimationCompleted(this.player, this.winningItem);
        this.cancelAnimation();
    }

    private void cleanupEntities() {
        if (this.labelDisplay != null && this.labelDisplay.isValid()) {
            this.plugin.getEngine().unregisterTrackedEntity(this.labelDisplay.getUniqueId());
            this.labelDisplay.remove();
            this.labelDisplay = null;
        }
        if (this.displayEntity != null && this.displayEntity.isValid()) {
            this.plugin.getEngine().unregisterTrackedEntity(this.displayEntity.getUniqueId());
            this.displayEntity.remove();
            this.displayEntity = null;
        }
    }

    @Override
    public void cancelAnimation() {
        this.cleanupEntities();
        try {
            this.cancel();
        } catch (Exception ignored) {}
    }
}
