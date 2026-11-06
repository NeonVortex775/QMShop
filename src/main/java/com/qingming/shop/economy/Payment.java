package com.qingming.shop.economy;

import com.qingming.shop.ShopPlugin;
import com.qingming.shop.config.ItemSpec;
import com.qingming.shop.config.Price;
import com.qingming.shop.config.Product;
import com.qingming.shop.util.Items;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Level;

/** Takes and returns money, points and items. */
public final class Payment {

    private final ShopPlugin plugin;
    private Economy vault;
    private PlayerPointsAPI points;
    private DecimalFormat moneyFmt = new DecimalFormat("0.##");

    public Payment(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        vault = null;
        points = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                RegisteredServiceProvider<Economy> rsp =
                        Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    vault = rsp.getProvider();
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Vault 经济服务获取失败", t);
            }
        }
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            try {
                points = PlayerPoints.getInstance().getAPI();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "PlayerPoints 接口获取失败", t);
            }
        }
        plugin.getLogger().info("经济: 金币=" + (vault == null ? "不可用" : vault.getName())
                + "，点券=" + (points == null ? "不可用" : "就绪"));
    }

    public boolean moneyReady() {
        return vault != null;
    }

    public boolean pointsReady() {
        return points != null;
    }

    public String moneyName() {
        return plugin.getConfig().getString("economy.money-name", "金币");
    }

    public String pointsName() {
        return plugin.getConfig().getString("economy.points-name", "点券");
    }

    public String moneyText(double v) {
        return moneyFmt.format(v);
    }

    public double balance(Player p) {
        return vault == null ? 0 : vault.getBalance(p);
    }

    public int points(Player p) {
        return points == null ? 0 : points.look(p.getUniqueId());
    }

    public String check(Player p, Product product, int qty) {
        List<Price> prices = product.getPrice();
        for (Price price : prices) {
            switch (price.getKind()) {
                case ITEM: {
                    int need = price.total(qty);
                    int have = count(p.getInventory(), price.getItem());
                    if (have < need) {
                        String msg = plugin.msg("not-enough-item")
                                .replace("%need%", String.valueOf(need))
                                .replace("%item%", price.getItem().label())
                                .replace("%have%", String.valueOf(have));
                        return msg;
                    }
                    break;
                }
                case POINTS: {
                    if (points == null) {
                        return plugin.msg("no-economy");
                    }
                    int need = price.total(qty);
                    int have = points(p);
                    if (have < need) {
                        return plugin.msg("not-enough-points")
                                .replace("%s", pointsName())
                                .replace("%need%", String.valueOf(need))
                                .replace("%have%", String.valueOf(have));
                    }
                    break;
                }
                default: {
                    if (vault == null) {
                        return plugin.msg("no-economy");
                    }
                    double need = price.total(qty);
                    double have = balance(p);
                    if (have + 1.0E-6 < need) {
                        return plugin.msg("not-enough-money")
                                .replace("%s", moneyName())
                                .replace("%need%", moneyText(need))
                                .replace("%have%", moneyText(have));
                    }
                    break;
                }
            }
        }
        return null;
    }

    public boolean charge(Player p, Product product, int qty) {
        for (Price price : product.getPrice()) {
            if (price.getKind() != Price.Kind.ITEM) {
                continue;
            }
            int need = price.total(qty);
            if (removeItems(p.getInventory(), price.getItem(), need) < need) {
                return false;
            }
        }
        for (Price price : product.getPrice()) {
            int need = price.total(qty);
            if (price.getKind() == Price.Kind.POINTS) {
                if (points == null || !points.take(p.getUniqueId(), need)) {
                    return false;
                }
            } else if (price.getKind() == Price.Kind.MONEY) {
                if (vault == null) {
                    return false;
                }
                if (!vault.withdrawPlayer(p, need).transactionSuccess()) {
                    return false;
                }
            }
        }
        return true;
    }

    public int count(PlayerInventory inv, ItemSpec spec) {
        int total = 0;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < Math.min(36, contents.length); i++) {
            ItemStack it = contents[i];
            if (!Items.empty(it) && spec.matches(it)) {
                total += it.getAmount();
            }
        }
        return total;
    }

    public int removeItems(PlayerInventory inv, ItemSpec spec, int amount) {
        int left = amount;
        for (int i = 0; i < Math.min(36, inv.getContents().length) && left > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (Items.empty(it) || !spec.matches(it)) {
                continue;
            }
            int have = it.getAmount();
            if (have <= left) {
                left -= have;
                inv.setItem(i, null);
            } else {
                it.setAmount(have - left);
                left = 0;
                inv.setItem(i, it);
            }
        }
        return amount - left;
    }
}
