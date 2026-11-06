package com.qingming.shop.config;

import com.qingming.shop.util.Cfg;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** One row of a shop: display item, price list, and what the buyer gets. */
public final class Product {

    private ItemSpec display;
    private int amount = 1;
    private final List<Price> price = new ArrayList<Price>();
    private final List<String> commands = new ArrayList<String>();
    private String permission;

    public static Product parse(Object sec, Layout layout) {
        if (!Cfg.isNode(sec)) {
            return null;
        }
        Product p = new Product();
        p.display = ItemSpec.parse(Cfg.node(sec, "display"));
        if (p.display == null) {
            return null;
        }
        p.amount = Math.max(1, Cfg.num(sec, "amount", 1));
        p.permission = Cfg.str(sec, "permission", null);

        List<?> list = Cfg.list(sec, "price");
        if (list != null) {
            for (Object o : list) {
                Price pr = Price.parse(o);
                if (pr != null) {
                    p.price.add(pr);
                }
            }
        }
        p.commands.addAll(Cfg.strList(sec, "commands"));
        return p;
    }

    public ItemSpec getDisplay() {
        return display;
    }

    public int getAmount() {
        return amount;
    }

    public List<Price> getPrice() {
        return price;
    }

    public List<String> getCommands() {
        return commands;
    }

    public String getPermission() {
        return permission == null || permission.trim().isEmpty() ? null : permission.trim();
    }

    public ItemStack icon(Player player) {
        ItemStack item = display.build(player);
        if (item == null) {
            return null;
        }
        item.setAmount(1);
        return item;
    }

    public String name(Player player) {
        ItemStack icon = icon(player);
        return icon == null ? display.label() : com.qingming.shop.util.Items.name(icon);
    }

    public String priceLine(int qty) {
        StringBuilder sb = new StringBuilder();
        for (Price p : price) {
            if (sb.length() > 0) {
                sb.append(" §7· ");
            }
            switch (p.getKind()) {
                case ITEM:
                    sb.append("§f").append(p.getItem().label()).append(" §f").append(p.total(qty));
                    break;
                case POINTS:
                    sb.append("§d").append(p.total(qty)).append(" §f点券");
                    break;
                default:
                    sb.append("§6").append(p.total(qty)).append(" §f金币");
                    break;
            }
        }
        return sb.toString();
    }
}
