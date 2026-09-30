package com.plex.giveaways.listener;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.reward.VoucherService;
import com.plex.giveaways.util.PlexText;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoucherRedeemListener implements Listener {
    private final PlexGiveaways plugin;
    private final Map<UUID, Long> debounce = new ConcurrentHashMap<>();

    public VoucherRedeemListener(PlexGiveaways plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Strict main-hand only: prevents dual-hand double redemption
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Player player = event.getPlayer();

        if (!pdc.has(this.plugin.getVouchers().getVoucherIdKey(), PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        Long last = this.debounce.get(player.getUniqueId());
        if (last != null && now - last < 500L) {
            return;
        }
        this.debounce.put(player.getUniqueId(), now);

        // Expiry verification
        if (pdc.has(this.plugin.getVouchers().getExpiryKey(), PersistentDataType.LONG)) {
            Long exp = pdc.get(this.plugin.getVouchers().getExpiryKey(), PersistentDataType.LONG);
            if (exp != null && exp > 0 && now > exp) {
                player.sendMessage(this.plugin.getMessage("chat.voucher-expired", "&#ff4757Your reward voucher has expired."));
                try {
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                } catch (Exception ignored) {}
                item.subtract(1);
                return;
            }
        }

        String idStr = pdc.get(this.plugin.getVouchers().getVoucherIdKey(), PersistentDataType.STRING);
        UUID vId;
        try {
            vId = UUID.fromString(idStr);
        } catch (Exception e) {
            player.sendMessage(this.plugin.getMessage("chat.invalid-voucher", "&#ff4757This voucher is invalid or corrupted!"));
            return;
        }

        VoucherService.ServerVoucher voucher = this.plugin.getVouchers().claimVoucher(vId);
        if (voucher == null) {
            player.sendMessage(this.plugin.getMessage("chat.invalid-voucher", "&#ff4757This voucher is invalid, already redeemed, or counterfeit!"));
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Exception ignored) {}
            item.subtract(1);
            return;
        }

        String rawCommand = voucher.command();
        if (rawCommand != null && !rawCommand.trim().isEmpty()) {
            String toExecute = rawCommand.trim();
            while (toExecute.startsWith("/")) {
                toExecute = toExecute.substring(1).trim();
            }

            String safeName = player.getName().contains(" ") ? "\"" + player.getName() + "\"" : player.getName();
            String finalCmd = toExecute
                    .replace("%player%", safeName)
                    .replace("%player_name%", safeName)
                    .replace("%player_uuid%", player.getUniqueId().toString())
                    .replace("%winner%", safeName);

            // Lowercase the command root for Mojang Brigadier compatibility
            String[] parts = finalCmd.split(" ", 2);
            if (parts.length > 0) {
                finalCmd = parts[0].toLowerCase() + (parts.length > 1 ? " " + parts[1] : "");
            }

            Bukkit.getLogger().info("[PlexGiveaways] Executing voucher command for " + player.getName() + ": " + finalCmd);
            boolean executed = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            if (!executed) {
                Bukkit.getLogger().warning("[PlexGiveaways] Console failed to run voucher command: " + finalCmd);
            }

            // Visual feedback
            this.playRedeemEffects(player, rawCommand);
            item.subtract(1);
        }
    }

    private void playRedeemEffects(Player player, String command) {
        try {
            if (this.plugin.getConfig().getBoolean("vouchers.redeem-effects.particles", true)) {
                String pType = this.plugin.getConfig().getString("vouchers.redeem-effects.particle-name", "HAPPY_VILLAGER");
                Particle particle = Particle.HAPPY_VILLAGER;
                try {
                    particle = Particle.valueOf(pType.toUpperCase());
                } catch (Exception ignored) {}
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.2, 0), 30, 0.4, 0.4, 0.4, 0.1);
            }

            String sName = this.plugin.getConfig().getString("vouchers.redeem-effects.sound-name", "ENTITY_PLAYER_LEVELUP");
            Sound sound = Sound.valueOf(sName.toUpperCase());
            float vol = (float) this.plugin.getConfig().getDouble("vouchers.redeem-effects.sound-volume", 1.0);
            float pit = (float) this.plugin.getConfig().getDouble("vouchers.redeem-effects.sound-pitch", 1.2);
            player.playSound(player.getLocation(), sound, vol, pit);
        } catch (Exception ignored) {}

        String title = this.plugin.getMessage("titles.claimed.header", "&#00d2ff&l✦ REWARD REDEEMED ✦");
        String sub = this.plugin.getMessage("titles.claimed.footer", "&#ffffffClaimed reward token!");

        player.showTitle(Title.title(
                PlexText.component(title),
                PlexText.component(sub),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(400))
        ));

        player.sendMessage(this.plugin.getMessage("chat.voucher-claimed", "&#2ecc71Successfully redeemed your reward token!"));
    }
}
