package com.qingming.shop.command;

import com.qingming.shop.ShopPlugin;
import com.qingming.shop.config.Shop;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** /shop [id] | open <player> <id> | list | reload */
public final class ShopCommand implements CommandExecutor, TabCompleter {

    private final ShopPlugin plugin;

    public ShopCommand(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.msg("only-player"));
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("qingmingshop.use")) {
                p.sendMessage(plugin.msg("no-permission"));
                return true;
            }
            Shop shop = plugin.shops().defaultShop();
            if (shop == null) {
                p.sendMessage(plugin.msg("no-shop").replace("%shop%", "default"));
                return true;
            }
            plugin.open(p, shop);
            return true;
        }

        String sub = args[0];

        if ("reload".equalsIgnoreCase(sub)) {
            if (!sender.hasPermission("qingmingshop.admin")) {
                sender.sendMessage(plugin.msg("no-permission"));
                return true;
            }
            plugin.reload();
            sender.sendMessage(plugin.msg("reloaded")
                    .replace("%count%", String.valueOf(plugin.shops().all().size())));
            return true;
        }

        if ("list".equalsIgnoreCase(sub)) {
            StringBuilder sb = new StringBuilder("§2商店列表 §7(" + plugin.shops().all().size() + ")§8: ");
            for (Shop s : plugin.shops().all()) {
                sb.append("§f").append(s.getId()).append("§7(").append(s.getProducts().size()).append("件) ");
            }
            sender.sendMessage(sb.toString());
            return true;
        }

        if ("open".equalsIgnoreCase(sub)) {
            if (!sender.hasPermission("qingmingshop.open")) {
                sender.sendMessage(plugin.msg("no-permission"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§7用法: /" + label + " open <玩家> <商店ID>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§c玩家不在线: " + args[1]);
                return true;
            }
            Shop shop = plugin.shops().get(args[2]);
            if (shop == null) {
                sender.sendMessage(plugin.msg("no-shop").replace("%shop%", args[2]));
                return true;
            }
            plugin.open(target, shop);
            sender.sendMessage("§a已为 §f" + target.getName() + " §a打开商店 §f" + shop.getId());
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.msg("only-player") + " 控制台请用 /" + label + " open <玩家> <商店>");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("qingmingshop.use")) {
            p.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        Shop shop = plugin.shops().get(sub);
        if (shop == null) {
            p.sendMessage(plugin.msg("no-shop").replace("%shop%", sub));
            return true;
        }
        plugin.open(p, shop);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            List<String> base = new ArrayList<String>(Arrays.asList("open", "list", "reload"));
            base.addAll(plugin.shops().ids());
            for (String s : base) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && "open".equalsIgnoreCase(args[0])) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 3 && "open".equalsIgnoreCase(args[0])) {
            for (String s : plugin.shops().ids()) {
                if (s.toLowerCase().startsWith(args[2].toLowerCase())) {
                    out.add(s);
                }
            }
        }
        return out;
    }
}
