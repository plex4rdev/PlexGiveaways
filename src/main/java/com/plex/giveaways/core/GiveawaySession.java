package com.plex.giveaways.core;

import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GiveawaySession {
    private final UUID sessionId;
    private volatile UUID winnerId;
    private volatile String animationKey;
    private volatile List<ItemStack> prizePool;
    private final long startTime;
    private final Set<UUID> trackedEntities = ConcurrentHashMap.newKeySet();
    private final List<BukkitTask> scheduledTasks = new CopyOnWriteArrayList<>();

    public GiveawaySession(UUID winnerId, String animationKey, List<ItemStack> prizePool) {
        this.sessionId = UUID.randomUUID();
        this.winnerId = winnerId;
        this.animationKey = animationKey;
        this.prizePool = prizePool != null ? new ArrayList<>(prizePool) : new ArrayList<>();
        this.startTime = System.currentTimeMillis();
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(UUID winnerId) {
        this.winnerId = winnerId;
    }

    public String getAnimationKey() {
        return animationKey;
    }

    public List<ItemStack> getPrizePool() {
        return prizePool;
    }

    public long getStartTime() {
        return startTime;
    }

    public Set<UUID> getTrackedEntities() {
        return trackedEntities;
    }

    public List<BukkitTask> getScheduledTasks() {
        return scheduledTasks;
    }

    public void cancelAllTasks() {
        for (BukkitTask task : scheduledTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        scheduledTasks.clear();
    }
}
