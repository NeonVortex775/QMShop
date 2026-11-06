package com.qingming.shop.listener;

import com.qingming.shop.ShopPlugin;
import com.qingming.shop.config.Shop;
import com.qingming.shop.gui.ShopSession;
import eos.moe.dragoncore.api.gui.event.CustomPacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

/** Clicks from the custom GUIs arrive here as DragonCore packets. */
public final class ShopPacketListener implements Listener {

    private final ShopPlugin plugin;

    public ShopPacketListener(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCustomPacket(CustomPacketEvent e) {
        if (!plugin.guiPackets() || e == null) {
            return;
        }
        if (!e.getIdentifier().equalsIgnoreCase("QingMingShop")) {
            return;
        }
        Player p = e.getPlayer();
        if (p == null) {
            return;
        }
        List<String> data = e.getData();
        if (data == null || data.isEmpty()) {
            return;
        }
        ShopSession s = plugin.sessions().get(p.getUniqueId());
        if (s == null) {
            return;
        }

        String action = data.get(0).toLowerCase();
        if ("select".equals(action)) {
            int index = num(data, 1, -1);
            if (index >= 0 && index < s.shop().getProducts().size()) {
                s.select(index);

                plugin.clampQty(s);
                plugin.sound(p, "click");
            }
        } else if ("qty".equals(action)) {
            int max = plugin.maxQty(s.selected());
            if (data.size() >= 3 && "set".equalsIgnoreCase(data.get(1))) {
                s.qty(num(data, 2, s.qty()), plugin.minQty(), max);
            } else {
                s.qty(s.qty() + num(data, 1, 0), plugin.minQty(), max);
            }
            plugin.sound(p, "click");
        } else if ("page".equals(action)) {
            s.page(s.page() + num(data, 1, 0));
            plugin.sound(p, "click");
        } else if ("cancel".equals(action)) {
            s.clearSelection();
            plugin.sound(p, "click");
        } else if ("buy".equals(action)) {

            plugin.buy(p, plugin.screen(p), s);
            return;
        } else if ("open".equals(action)) {
            String id = data.size() > 1 ? data.get(1) : "";
            if (!p.hasPermission("qingmingshop.use") && !p.isOp()) {
                p.sendMessage(plugin.msg("no-permission"));
                return;
            }
            Shop shop = plugin.shops().get(id);
            if (shop != null) {
                plugin.open(p, shop);
            }
            return;
        } else if ("close".equals(action)) {
            plugin.sessions().remove(p.getUniqueId());
            p.closeInventory();
            return;
        } else if ("hint".equals(action)) {
            p.sendMessage(plugin.msg("qty-hint"));
            return;
        } else {
            return;
        }
        plugin.refresh(p, s);
    }

    private static int num(List<String> data, int i, int def) {
        if (data.size() <= i) {
            return def;
        }
        try {
            return Integer.parseInt(data.get(i).trim());
        } catch (Throwable t) {
            return def;
        }
    }
}
