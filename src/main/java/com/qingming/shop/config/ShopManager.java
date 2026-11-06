package com.qingming.shop.config;

import com.qingming.shop.ShopPlugin;
import com.qingming.shop.util.Cfg;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/** Loads shops/*.yml. The file name is the shop id. */
public final class ShopManager {

    private final ShopPlugin plugin;
    private final Map<String, Shop> shops = new LinkedHashMap<String, Shop>();
    private Layout defaults = new Layout();

    public ShopManager(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    public Layout defaults() {
        return defaults;
    }

    public void loadAll() {
        shops.clear();
        defaults = Layout.load(plugin.getConfig().getConfigurationSection("gui"));

        File dir = new File(plugin.getDataFolder(), "shops");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            plugin.getLogger().warning("无法创建 shops 目录: " + dir.getPath());
        }

        if (dir.isDirectory() && dir.listFiles() != null && dir.listFiles().length == 0) {
            plugin.saveResource("shops/灵材.yml", false);
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (!f.isFile() || !f.getName().toLowerCase().endsWith(".yml")) {
                continue;
            }
            String id = f.getName().substring(0, f.getName().length() - 4);
            try {
                Shop shop = load(id, YamlConfiguration.loadConfiguration(f));
                if (shop != null) {
                    shops.put(id, shop);
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "载入商店失败: " + f.getName(), t);
            }
        }
        plugin.getLogger().info("已载入 " + shops.size() + " 个商店: " + shops.keySet());
    }

    private Shop load(String id, YamlConfiguration cfg) {
        Shop shop = new Shop(id);
        shop.setTitle(Cfg.str(cfg, "title", id));

        Layout layout = defaults.copy();
        layout.override(Cfg.node(cfg, "layout"));
        shop.setLayout(layout);

        List<?> list = Cfg.list(cfg, "items");
        if (list == null) {
            plugin.getLogger().warning("商店 " + id + " 没有 items 段");
            return shop;
        }
        for (Object o : list) {
            Product p = Product.parse(o, layout);
            if (p == null) {
                plugin.getLogger().warning("商店 " + id + " 有一个商品解析失败（缺少 display 或材质写错）");
                continue;
            }
            if (p.getPrice().isEmpty()) {
                plugin.getLogger().warning("商店 " + id + " 的商品 " + p.name(null) + " 没有 price，将无法购买");
            }
            if (p.getPrice().size() > layout.maxRequirements) {
                plugin.getLogger().warning("商店 " + id + " 的商品 " + p.name(null) + " 需求超过 "
                        + layout.maxRequirements + " 项，超出的不会显示");
            }
            shop.getProducts().add(p);
        }
        plugin.getLogger().info("商店 " + id + " 载入 " + shop.getProducts().size() + " 个商品"
                + (shop.getProducts().size() > layout.rows ? "（分 " + shop.pageCount() + " 页）" : ""));
        return shop;
    }

    public Shop get(String id) {
        if (id == null) {
            return null;
        }
        Shop s = shops.get(id);
        if (s != null) {
            return s;
        }
        for (Map.Entry<String, Shop> e : shops.entrySet()) {
            if (e.getKey().equalsIgnoreCase(id)) {
                return e.getValue();
            }
        }
        return null;
    }

    public Collection<Shop> all() {
        return shops.values();
    }

    public List<String> ids() {
        return new ArrayList<String>(shops.keySet());
    }

    public Shop defaultShop() {
        String want = plugin.getConfig().getString("default-shop", "");
        Shop s = get(want);
        if (s != null) {
            return s;
        }
        return shops.isEmpty() ? null : shops.values().iterator().next();
    }

}
