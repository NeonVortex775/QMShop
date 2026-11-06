package com.qingming.shop.gui;

import com.qingming.shop.ShopPlugin;
import com.qingming.shop.config.Layout;
import com.qingming.shop.config.Price;
import com.qingming.shop.config.Product;
import com.qingming.shop.config.Shop;
import eos.moe.dragoncore.network.PacketSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns gui/*.yml into a DragonCore screen and keeps it in sync. */
public final class CustomGui {

    public static final String PACKET = "QingMingShop";

    private static final Pattern ROW = Pattern.compile("^行(\\d+)(.*)$");

    private CustomGui() {
    }

    public static boolean open(ShopPlugin plugin, Player p, ShopSession s, GuiTemplate tpl) {
        Shop shop = s.shop();
        Layout L = shop.getLayout();

        YamlConfiguration cfg;
        try {
            cfg = YamlConfiguration.loadConfiguration(tpl.file());
        } catch (Throwable t) {
            plugin.getLogger().warning("读取界面模板失败: " + tpl.file().getName() + " -> " + t);
            return false;
        }
        if (cfg.getKeys(false).isEmpty()) {
            plugin.getLogger().warning("界面模板是空的: " + tpl.file().getName());
            return false;
        }

        cfg.set("match", tpl.guiName());

        int W = tpl.width() > 0 ? tpl.width() : L.width;
        int H = tpl.height() > 0 ? tpl.height() : L.height();
        int rows = tpl.rows() > 0 ? tpl.rows() : L.rows;

        int perPage = s.perPage();
        if (rows > perPage) {
            plugin.getLogger().warning("gui/" + tpl.file().getName() + " 画了 " + rows
                    + " 行，但商店 " + shop.getId() + " 每页只有 " + perPage + " 个商品，多出的行会隐藏");
        }

        Map<String, String> ph = new HashMap<String, String>();
        String title = com.qingming.shop.util.Text.color(shop.getTitle());
        ph.put("{w}", String.valueOf(W));
        ph.put("{h}", String.valueOf(H));
        ph.put("{shop}", shop.getId());
        ph.put("{title}", title);
        ph.put("{page}", String.valueOf(s.page() + 1));
        ph.put("{pages}", String.valueOf(s.pageCount()));
        ph.put("{qty}", String.valueOf(s.qty()));
        ph.put("{rows}", String.valueOf(rows));
        ph.put("{step}", String.valueOf(plugin.step()));
        ph.put("{big}", String.valueOf(plugin.bigStep()));

        Product sel = s.selected();
        int first = s.firstIndexOfPage();
        Map<String, ItemStack> items = new LinkedHashMap<String, ItemStack>();

        List<String> keys = new ArrayList<String>(cfg.getKeys(false));
        for (String key : keys) {
            Object raw = cfg.get(key);
            if (!(raw instanceof ConfigurationSection)) {

                if (raw instanceof String) {
                    cfg.set(key, apply((String) raw, ph));
                }
                continue;
            }
            ConfigurationSection sec = (ConfigurationSection) raw;

            if (key.startsWith("面板")) {
                if (sel == null) {
                    hide(sec);
                    continue;
                }
            }

            if (key.startsWith("提示")) {
                sec.set("visible", sel == null);
                replaceIn(sec, ph);
                continue;
            }

            Matcher m = ROW.matcher(key);
            if (m.matches()) {
                int n = Integer.parseInt(m.group(1));
                String suffix = m.group(2);
                Product prod = n <= perPage ? shop.at(first + n - 1) : null;
                if (prod == null) {
                    hide(sec);
                    continue;
                }
                sec.set("visible", true);
                Map<String, String> rp = new HashMap<String, String>(ph);
                int index = first + n - 1;
                rp.put("{index}", String.valueOf(index));
                rp.put("{row}", String.valueOf(n));

                if ("名".equals(suffix)) {
                    sec.set("texts", Arrays.asList(prod.name(p)));
                } else if ("价".equals(suffix)) {
                    sec.set("texts", Arrays.asList("§7需要 " + prod.priceLine(s.qty())));
                } else if ("选择字".equals(suffix)) {
                    boolean picked = s.selectedIndex() == index;
                    sec.set("texts", Arrays.asList(picked ? "§6已选" : "§e选择"));
                } else if ("图标".equals(suffix)) {
                    String ident = "qmshop_icon_" + n;
                    sec.set("identifier", ident);
                    ItemStack icon = prod.icon(p);
                    if (icon != null) {
                        items.put(ident, icon);
                    }
                } else if (suffix.startsWith("需求框")) {

                    int k = number(suffix.substring(3), 0) - 1;
                    List<Price> prices = prod.getPrice();
                    if (!(k >= 0 && k < prices.size() && k < L.maxRequirements)) {
                        hide(sec);
                        continue;
                    }
                } else if (suffix.startsWith("需求") && suffix.length() > 2
                        && Character.isDigit(suffix.charAt(2))) {
                    int k = number(suffix.substring(2), 0) - 1;
                    String ident = "qmshop_req_" + n + "_" + (k + 1);
                    List<Price> prices = prod.getPrice();
                    if (k >= 0 && k < prices.size() && k < L.maxRequirements) {
                        sec.set("identifier", ident);
                        ItemStack icon = requirementIcon(plugin, p, prices.get(k));
                        if (icon != null) {
                            items.put(ident, icon);
                        }
                    } else {
                        hide(sec);
                        continue;
                    }
                }
                replaceIn(sec, rp);
                continue;
            }

            if ("标题".equals(key)) {
                sec.set("texts", Arrays.asList(title));
            } else if ("页码".equals(key)) {
                sec.set("texts", Arrays.asList("§7" + (s.page() + 1) + " / " + s.pageCount()));
            } else if ("上一页".equals(key)) {
                sec.set("visible", s.page() > 0);
            } else if ("下一页".equals(key)) {
                sec.set("visible", s.page() < s.pageCount() - 1);
            } else if ("面板数量".equals(key)) {

                sec.set("texts", Arrays.asList("§e" + s.qty()));
            } else if ("面板合计".equals(key)) {
                Product picked = s.selected();
                if (picked != null) {

                    sec.set("texts", Arrays.asList("§7" + s.qty() + " × §f" + picked.name(p)
                            + "   §7合计: " + picked.priceLine(s.qty())));
                }
            }
            replaceIn(sec, ph);
        }

        for (Map.Entry<String, ItemStack> e : items.entrySet()) {
            try {
                PacketSender.putClientSlotItem(p, e.getKey(), e.getValue());
            } catch (Throwable t) {
                plugin.getLogger().warning("下发槽位物品失败 " + e.getKey() + " -> " + t);
            }
        }
        try {
            PacketSender.sendYaml(p, "Gui/" + tpl.guiName() + ".yml", cfg);
            PacketSender.sendOpenGui(p, tpl.guiName());
        } catch (Throwable t) {
            plugin.getLogger().warning("打开自定义界面失败: " + t);
            return false;
        }
        if (plugin.getConfig().getBoolean("gui.debug", false)) {
            plugin.getLogger().info("下发界面 gui/" + tpl.file().getName() + " -> Gui/"
                    + tpl.guiName() + ".yml（第 " + (s.page() + 1) + "/" + s.pageCount()
                    + " 页，" + rows + " 行，" + items.size() + " 个槽位物品）");
        }
        return true;
    }

    public static void refresh(ShopPlugin plugin, Player p, ShopSession s) {
        GuiTemplate tpl = plugin.guiTemplates().pick(s.shop().getId());
        if (tpl == null) {
            return;
        }
        open(plugin, p, s, tpl);
    }

    private static void hide(ConfigurationSection sec) {
        sec.set("visible", false);
    }

    private static ItemStack requirementIcon(ShopPlugin plugin, Player p, Price price) {
        if (price.getKind() == Price.Kind.ITEM) {
            return price.getItem().build(p, price.getAmount());
        }
        if (price.getKind() == Price.Kind.POINTS) {
            return plugin.pointsIcon(p, price.getAmount());
        }
        return plugin.moneyIcon(p, price.getAmount());
    }

    private static int number(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Throwable t) {
            return def;
        }
    }

    private static void replaceIn(ConfigurationSection sec, Map<String, String> ph) {
        List<String> keys = new ArrayList<String>(sec.getKeys(true));
        for (String k : keys) {
            Object v = sec.get(k);
            if (v instanceof String) {
                String nv = apply((String) v, ph);
                if (!nv.equals(v)) {
                    sec.set(k, nv);
                }
            } else if (v instanceof List) {
                List<?> list = (List<?>) v;
                boolean changed = false;
                List<Object> out = new ArrayList<Object>();
                for (Object o : list) {
                    if (o instanceof String) {
                        String nv = apply((String) o, ph);
                        if (!nv.equals(o)) {
                            changed = true;
                        }
                        out.add(nv);
                    } else {
                        out.add(o);
                    }
                }
                if (changed) {
                    sec.set(k, out);
                }
            }
        }
    }

    private static String apply(String s, Map<String, String> ph) {
        if (s == null || s.indexOf('{') < 0) {
            return s;
        }
        String out = s;
        for (Map.Entry<String, String> e : ph.entrySet()) {
            if (out.contains(e.getKey())) {
                out = out.replace(e.getKey(), e.getValue());
            }
        }
        return out;
    }
}
