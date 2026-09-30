package com.plex.giveaways.core;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.animation.GiveawayAnimation;
import com.plex.giveaways.util.InventoryUtil;
import com.plex.giveaways.util.PlexText;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class GiveawayEngine {
    private final PlexGiveaways plugin;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final NamespacedKey entityTag;
    private final Set<UUID> trackedEntityIds = ConcurrentHashMap.newKeySet();
    private final List<BukkitTask> activeTasks = new CopyOnWriteArrayList<>();

    private volatile UUID currentWinnerId;
    private volatile String lastWinnerName = "None";
    private volatile String lastPrizeName = "None";
    private volatile long lastEndTime = 0L;
    private volatile GiveawayAnimation currentAnimation;
    private volatile List<ItemStack> activePool;

    public GiveawayEngine(PlexGiveaways plugin) {
        this.plugin = plugin;
        this.entityTag = new NamespacedKey(plugin, "plex_entity");
    }

    public boolean isActive() {
        return this.active.get();
    }

    public boolean launchGiveaway(Player initiator, List<ItemStack> overridePrizes) {
        if (!this.active.compareAndSet(false, true)) {
            if (initiator != null) {
                initiator.sendMessage(this.plugin.getMessage("chat.already-active", "&#ff4757A giveaway is already in progress!"));
            }
            return false;
        }

        long cooldown = this.plugin.getConfig().getLong("general.cooldown-between-giveaways-seconds", 15L);
        long elapsed = (System.currentTimeMillis() - this.lastEndTime) / 1000L;
        if (elapsed < cooldown) {
            this.active.set(false);
            long wait = cooldown - elapsed;
            String msg = this.plugin.getMessage("chat.cooldown-active", "&#ff4757Please wait &#ffd200%seconds%s&f.")
                    .replace("%seconds%", String.valueOf(wait));
            if (initiator != null) {
                initiator.sendMessage(msg);
            }
            return false;
        }

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int minRequired = this.plugin.getConfig().getInt("general.min-participants-to-start", 1);
        if (online.size() < minRequired) {
            this.active.set(false);
            String msg = this.plugin.getMessage("chat.insufficient-players", "&#ff4757Need &#ffd200%required% &#ff4757players to start.")
                    .replace("%required%", String.valueOf(minRequired - online.size()));
            if (initiator != null) {
                initiator.sendMessage(msg);
            }
            return false;
        }

        Player winner = this.selectRandomWinner(online, null);
        if (winner == null) {
            this.active.set(false);
            if (initiator != null) {
                initiator.sendMessage(this.plugin.getMessage("chat.no-eligible-winners", "&#ff4757No eligible participants online."));
            }
            return false;
        }

        this.currentWinnerId = winner.getUniqueId();
        this.activeTasks.clear();
        this.trackedEntityIds.clear();

        String animKey = this.plugin.getAnimations().selectWeightedKey();
        this.activePool = (overridePrizes != null && !overridePrizes.isEmpty())
                ? overridePrizes
                : this.plugin.getRewardPools().getPoolPrizes(animKey);

        if (this.activePool == null || this.activePool.isEmpty()) {
            this.activePool = Collections.singletonList(new ItemStack(Material.DIAMOND));
        }

        ItemStack previewPrize = this.activePool.get(0);
        String prizeName = this.formatPrizeTitle(previewPrize);

        // Start broadcasts
        List<String> startAnnouncement = this.plugin.getConfig().getStringList("announcements.start");
        if (!startAnnouncement.isEmpty()) {
            for (String line : startAnnouncement) {
                String formatted = PlexText.colorize(line
                        .replace("%reward%", prizeName)
                        .replace("%prize%", prizeName)
                        .replace("%entry_count%", String.valueOf(online.size()))
                );
                Bukkit.broadcastMessage(formatted);
            }
        }

        this.plugin.getDisplayBar().showRollingBar();
        this.plugin.getDiscord().dispatch("on-start", null, prizeName, online.size());

        // Launch animation
        this.currentAnimation = this.plugin.getAnimations().create(animKey, winner, this.activePool);
        BukkitTask task = this.currentAnimation.runTaskTimer(this.plugin, 0L, 1L);
        this.activeTasks.add(task);

        return true;
    }

    private Player selectRandomWinner(List<Player> players, UUID excludeId) {
        List<Player> valid = new ArrayList<>();
        for (Player p : players) {
            if (excludeId != null && p.getUniqueId().equals(excludeId)) continue;
            if (p.getGameMode() == GameMode.SPECTATOR || p.hasPermission("plex.giveaways.exempt")) continue;
            valid.add(p);
        }
        if (valid.isEmpty()) {
            for (Player p : players) {
                if (excludeId != null && p.getUniqueId().equals(excludeId)) continue;
                valid.add(p);
            }
        }
        if (valid.isEmpty()) return null;
        return valid.get(new Random().nextInt(valid.size()));
    }

    public boolean rerollWinner() {
        if (this.active.get()) {
            return false;
        }
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        Player newWinner = this.selectRandomWinner(online, this.currentWinnerId);
        if (newWinner == null) {
            return false;
        }
        this.currentWinnerId = newWinner.getUniqueId();
        this.active.set(true);

        if (this.activePool == null || this.activePool.isEmpty()) {
            this.activePool = this.plugin.getRewardPools().getPoolPrizes("roulette");
        }
        ItemStack prize = this.activePool.get(new Random().nextInt(this.activePool.size()));

        String rerollMsg = this.plugin.getMessage("chat.reroll-broadcast", "&#2ecc71Winner rerolled! Lucky winner: &#00f2fe%winner%&f!")
                .replace("%winner%", newWinner.getName());
        Bukkit.broadcastMessage(PlexText.colorize(rerollMsg));

        this.onAnimationCompleted(newWinner, prize);
        return true;
    }

    public void handlePlayerDisconnect(Player player) {
        if (this.active.get() && this.currentWinnerId != null && player.getUniqueId().equals(this.currentWinnerId)) {
            List<Player> remaining = new ArrayList<>(Bukkit.getOnlinePlayers());
            remaining.remove(player);
            Player runnerUp = this.selectRandomWinner(remaining, player.getUniqueId());

            if (runnerUp != null) {
                this.currentWinnerId = runnerUp.getUniqueId();
                Bukkit.broadcastMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#ffd200Winner disconnected! Rerolled to &#00f2fe" + runnerUp.getName() + "&#ffd200."));
            } else {
                Bukkit.broadcastMessage(this.plugin.getMessage("chat.winner-disconnected", "&#ff4757Winner disconnected; aborting giveaway."));
                this.abortGiveaway();
            }
        }
    }

    public void onAnimationCompleted(Player winner, ItemStack prizeItem) {
        if (!this.active.get()) {
            return;
        }

        ItemStack reward = prizeItem != null ? prizeItem.clone() : new ItemStack(Material.DIAMOND);
        String prizeName = this.formatPrizeTitle(reward);

        this.lastWinnerName = winner.getName();
        this.lastPrizeName = prizeName;

        this.plugin.getDisplayBar().showWinnerBar(winner.getName(), prizeName);

        // Winner announcements
        List<String> winAnnouncement = this.plugin.getConfig().getStringList("announcements.winner");
        if (!winAnnouncement.isEmpty()) {
            for (String line : winAnnouncement) {
                String formatted = PlexText.colorize(line
                        .replace("%winner%", winner.getName())
                        .replace("%reward%", prizeName)
                        .replace("%prize%", prizeName)
                );
                Bukkit.broadcastMessage(formatted);
            }
        }

        this.plugin.getDiscord().dispatch("on-finish", winner.getName(), prizeName, Bukkit.getOnlinePlayers().size());

        // Full inventory protection: item drops safely if inventory full
        String overflowMsg = this.plugin.getMessage("chat.inventory-overflow", "&#ffd200Your inventory was full! Reward dropped at your feet.");
        InventoryUtil.giveOrDrop(winner, reward, overflowMsg);

        this.concludeGiveaway();
    }

    public void abortGiveaway() {
        if (!this.active.get()) return;

        if (this.currentAnimation != null) {
            this.currentAnimation.cancelAnimation();
            this.currentAnimation = null;
        }

        for (BukkitTask task : this.activeTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        this.activeTasks.clear();

        this.cleanupAllEntities();
        this.plugin.getDisplayBar().hideAll();

        this.active.set(false);
        this.currentWinnerId = null;
        this.lastEndTime = System.currentTimeMillis();

        Bukkit.broadcastMessage(this.plugin.getMessage("chat.admin-aborted", "&#ff4757The giveaway was aborted by an administrator."));
    }

    public void concludeGiveaway() {
        this.active.set(false);
        this.currentWinnerId = null;
        this.lastEndTime = System.currentTimeMillis();

        for (BukkitTask task : this.activeTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        this.activeTasks.clear();
        this.cleanupAllEntities();
    }

    public void registerTrackedEntity(Entity entity) {
        if (entity != null) {
            entity.setPersistent(false);
            entity.getPersistentDataContainer().set(this.entityTag, PersistentDataType.BYTE, (byte) 1);
            this.trackedEntityIds.add(entity.getUniqueId());
        }
    }

    public void unregisterTrackedEntity(UUID id) {
        if (id != null) {
            this.trackedEntityIds.remove(id);
        }
    }

    public void cleanupAllEntities() {
        for (UUID id : this.trackedEntityIds) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.isValid()) {
                e.remove();
            }
        }
        this.trackedEntityIds.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Entity e : p.getNearbyEntities(8.0, 8.0, 8.0)) {
                if (e.getPersistentDataContainer().has(this.entityTag, PersistentDataType.BYTE)) {
                    e.remove();
                }
            }
        }
    }

    private String formatPrizeTitle(ItemStack item) {
        if (item == null) return "Mystery Reward";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return PlexText.colorize(item.getItemMeta().getDisplayName());
        }
        String raw = item.getType().name().replace("_", " ").toLowerCase();
        StringBuilder title = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty()) {
                title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return PlexText.strip(title.toString().trim());
    }

    public String getLastWinnerName() {
        return lastWinnerName;
    }

    public String getLastPrizeName() {
        return lastPrizeName;
    }
}
