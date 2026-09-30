package com.plex.giveaways.command;

import com.plex.giveaways.PlexGiveaways;
import com.plex.giveaways.ui.DashboardMenu;
import com.plex.giveaways.util.InventoryUtil;
import com.plex.giveaways.util.PlexText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlexGiveawayCommand implements TabExecutor {
    private final PlexGiveaways plugin;

    public PlexGiveawayCommand(PlexGiveaways plugin) {
        this.plugin = plugin;
    }

    private boolean checkPerm(CommandSender sender, String node) {
        return sender.hasPermission(node) || sender.hasPermission("plex.giveaways.admin");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "menu", "gui" -> {
                if (sender instanceof Player p) {
                    if (this.checkPerm(p, "plex.giveaways.menu")) {
                        DashboardMenu.open(p, this.plugin);
                    } else {
                        p.sendMessage(PlexText.colorize("&#ff4757You lack permission to access the giveaway dashboard."));
                    }
                } else {
                    sender.sendMessage("This command can only be run in-game.");
                }
            }
            case "start", "forcestart" -> {
                if (!this.checkPerm(sender, "plex.giveaways.start")) {
                    sender.sendMessage(PlexText.colorize("&#ff4757You lack permission to start giveaways."));
                    return true;
                }
                boolean started = this.plugin.getEngine().launchGiveaway(sender instanceof Player p ? p : null, null);
                if (started) {
                    sender.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#2ecc71Successfully launched giveaway event!"));
                }
            }
            case "stop", "cancel", "abort" -> {
                if (!this.checkPerm(sender, "plex.giveaways.cancel")) {
                    sender.sendMessage(PlexText.colorize("&#ff4757You lack permission to abort giveaways."));
                    return true;
                }
                if (!this.plugin.getEngine().isActive()) {
                    sender.sendMessage(PlexText.colorize("&#ff4757There is no active giveaway to abort."));
                    return true;
                }
                this.plugin.getEngine().abortGiveaway();
                sender.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#ff4757Giveaway event was aborted."));
            }
            case "reroll" -> {
                if (!this.checkPerm(sender, "plex.giveaways.reroll")) {
                    sender.sendMessage(PlexText.colorize("&#ff4757You lack permission to reroll winners."));
                    return true;
                }
                if (this.plugin.getEngine().isActive()) {
                    sender.sendMessage(PlexText.colorize("&#ff4757Cannot reroll while a giveaway is currently spinning!"));
                    return true;
                }
                boolean rerolled = this.plugin.getEngine().rerollWinner();
                if (rerolled) {
                    sender.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#2ecc71Winner rerolled successfully!"));
                } else {
                    sender.sendMessage(PlexText.colorize("&#ff4757No eligible participants to reroll."));
                }
            }
            case "cv", "createvoucher" -> {
                if (!this.checkPerm(sender, "plex.giveaways.voucher")) {
                    sender.sendMessage(PlexText.colorize("&#ff4757You lack permission to configure vouchers."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#ff4757Usage: /gw cv <pool> <command...>"));
                    sender.sendMessage(PlexText.colorize("&#718096Pools: roulette, vortex, overhead, instant"));
                    return true;
                }
                String pool = args[1].toLowerCase();
                String commandStr = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                this.plugin.getRewardPools().addCommandReward(pool, commandStr);
                sender.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#2ecc71Added command voucher to pool: " + pool));
            }
            case "givevoucher" -> {
                if (!this.checkPerm(sender, "plex.giveaways.voucher")) {
                    sender.sendMessage(PlexText.colorize("&#ff4757You lack permission to issue vouchers."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#ff4757Usage: /gw givevoucher <player> [durationMin] <command...>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    sender.sendMessage(PlexText.colorize("&#ff4757Target player is not online."));
                    return true;
                }

                int duration = this.plugin.getConfig().getInt("vouchers.default-expiration-minutes", 10);
                int cmdIndex = 2;
                try {
                    int parsed = Integer.parseInt(args[2]);
                    if (parsed > 0) {
                        duration = parsed;
                        cmdIndex = 3;
                    }
                } catch (NumberFormatException ignored) {}

                if (cmdIndex >= args.length) {
                    sender.sendMessage(PlexText.colorize("&#ff4757Please provide a command for the voucher!"));
                    return true;
                }

                String cmdStr = String.join(" ", Arrays.copyOfRange(args, cmdIndex, args.length));
                ItemStack voucher = this.plugin.getVouchers().createVoucherItem(cmdStr, duration);

                InventoryUtil.giveOrDrop(target, voucher, "&#ffd200Your inventory was full! Voucher dropped at your feet.");
                sender.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#2ecc71Issued reward token to " + target.getName()));
                target.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#2ecc71You received an exclusive Giveaway Reward Token!"));
            }
            case "reload", "rl" -> {
                if (!this.checkPerm(sender, "plex.giveaways.reload")) {
                    sender.sendMessage(PlexText.colorize("&#ff4757You lack permission to reload configurations."));
                    return true;
                }
                this.plugin.reloadAll();
                sender.sendMessage(PlexText.colorize("&#00d2ff&lPLEX&#00f2fe&lGIVEAWAYS &8» &#2ecc71All configurations, pools, and schedules reloaded!"));
            }
            case "support", "help" -> this.sendSupport(sender);
            default -> this.sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(PlexText.colorize("&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(PlexText.colorize("       &#00d2ff&l❖ &#00f2fe&lPLEX GIVEAWAYS COMMANDS &#00d2ff&l❖"));
        sender.sendMessage(PlexText.colorize(""));
        if (this.checkPerm(sender, "plex.giveaways.menu")) {
            sender.sendMessage(PlexText.colorize("  &#00f2fe/gw menu &8- &#ffffffOpen interactive administration dashboard"));
        }
        if (this.checkPerm(sender, "plex.giveaways.start")) {
            sender.sendMessage(PlexText.colorize("  &#00f2fe/gw start &8- &#ffffffForce launch a server giveaway"));
        }
        if (this.checkPerm(sender, "plex.giveaways.cancel")) {
            sender.sendMessage(PlexText.colorize("  &#00f2fe/gw stop &8- &#ffffffAbort currently active giveaway"));
        }
        if (this.checkPerm(sender, "plex.giveaways.reroll")) {
            sender.sendMessage(PlexText.colorize("  &#00f2fe/gw reroll &8- &#ffffffPick an alternate winner for the last giveaway"));
        }
        if (this.checkPerm(sender, "plex.giveaways.voucher")) {
            sender.sendMessage(PlexText.colorize("  &#00f2fe/gw cv <pool> <cmd> &8- &#ffffffAdd command voucher to reward pool"));
            sender.sendMessage(PlexText.colorize("  &#00f2fe/gw givevoucher <p> [min] <cmd> &8- &#ffffffIssue reward token to player"));
        }
        if (this.checkPerm(sender, "plex.giveaways.reload")) {
            sender.sendMessage(PlexText.colorize("  &#00f2fe/gw reload &8- &#ffffffReload all YAML configurations & schedules"));
        }
        sender.sendMessage(PlexText.colorize("  &#00f2fe/gw support &8- &#ffffffCommunity support and assistance"));
        sender.sendMessage(PlexText.colorize(""));
        sender.sendMessage(PlexText.colorize("&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void sendSupport(CommandSender sender) {
        sender.sendMessage(PlexText.colorize("&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(PlexText.colorize("       &#00d2ff&l❖ &#00f2fe&lPLEX COMMUNITY SUPPORT &#00d2ff&l❖"));
        sender.sendMessage(PlexText.colorize(""));
        String discordUrl = this.plugin.getConfig().getString("general.community-discord", "https://discord.gg/plex");
        if (discordUrl != null && !discordUrl.isEmpty()) {
            Component link = PlexText.component("  &#00f2fe▶ &9&nClick Here to Join our Official Discord")
                    .clickEvent(ClickEvent.openUrl(discordUrl))
                    .hoverEvent(HoverEvent.showText(PlexText.component("&aOpen: " + discordUrl)));
            sender.sendMessage(link);
        }
        sender.sendMessage(PlexText.colorize("  &#718096PlexGiveaways v" + this.plugin.getDescription().getVersion() + " authored by &#00f2fePlex"));
        sender.sendMessage(PlexText.colorize(""));
        sender.sendMessage(PlexText.colorize("&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if (this.checkPerm(sender, "plex.giveaways.menu")) list.add("menu");
            if (this.checkPerm(sender, "plex.giveaways.start")) list.add("start");
            if (this.checkPerm(sender, "plex.giveaways.cancel")) list.add("stop");
            if (this.checkPerm(sender, "plex.giveaways.reroll")) list.add("reroll");
            if (this.checkPerm(sender, "plex.giveaways.voucher")) {
                list.add("createvoucher");
                list.add("givevoucher");
            }
            if (this.checkPerm(sender, "plex.giveaways.reload")) list.add("reload");
            list.add("support");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("createvoucher") || args[0].equalsIgnoreCase("cv")) {
                list.addAll(Arrays.asList("roulette", "vortex", "overhead", "instant"));
            } else if (args[0].equalsIgnoreCase("givevoucher")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }
            }
        }
        return list;
    }
}
