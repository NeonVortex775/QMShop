package com.qingming.shop.config;

import com.qingming.shop.util.Cfg;
import com.qingming.shop.util.Items;
import com.qingming.shop.util.NbtUtil;
import com.qingming.shop.util.NiHook;
import com.qingming.shop.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An item block from YAML - a product icon or a requirement. */
public final class ItemSpec {

    private Material material;

    private short data;
    private String name;
    private List<String> lore = new ArrayList<String>();
    private Map<String, String> nbt = new LinkedHashMap<String, String>();
    private String ni;
    private boolean glow;
    private int amount = 1;

    private static boolean strictPlain = true;

    public static void strictPlainMatch(boolean on) {
        strictPlain = on;
    }

    public static ItemSpec parse(Object sec) {
        if (!Cfg.isNode(sec)) {
            return null;
        }
        ItemSpec s = new ItemSpec();
        s.ni = Cfg.str(sec, "ni", null);
        s.name = Cfg.str(sec, "name", null);
        s.lore = Cfg.strList(sec, "lore");
        s.glow = Cfg.bool(sec, "glow", false);
        s.amount = Math.max(1, Math.min(64, Cfg.num(sec, "amount", 1)));

        String raw = Cfg.str(sec, "material", null);
        if (raw == null || raw.trim().isEmpty()) {
            raw = Cfg.str(sec, "type", "");
        }

        String dataRaw = Cfg.str(sec, "data", null);
        if (dataRaw != null && "*".equals(dataRaw.trim())) {
            s.data = -1;
        }
        if (raw != null && !raw.trim().isEmpty()) {
            String m = raw.trim().toUpperCase();
            if (m.contains(":")) {
                String[] sp = m.split(":", 2);
                m = sp[0];
                if ("*".equals(sp[1].trim())) {
                    s.data = -1;
                } else {
                    try {
                        s.data = Short.parseShort(sp[1].trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if (s.data != -1) {
                s.data = (short) Cfg.num(sec, "data", 0);
            }
            s.material = Material.matchMaterial(m);
        }
        Map<String, Object> nbtMap = Cfg.asMap(Cfg.node(sec, "nbt"));
        if (nbtMap != null) {
            for (Map.Entry<String, Object> e : nbtMap.entrySet()) {
                if (!Cfg.isNode(e.getValue())) {
                    s.nbt.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }
        }
        if (s.material == null && (s.ni == null || s.ni.trim().isEmpty())) {
            return null;
        }
        return s;
    }

    public ItemStack build(Player player, int want) {
        ItemStack it = build(player);
        if (it != null) {
            it.setAmount(Math.max(1, Math.min(64, want)));
        }
        return it;
    }

    public ItemStack build(Player player) {
        ItemStack item = null;
        if (ni != null && !ni.trim().isEmpty()) {
            item = NiHook.get(ni, player);
        }
        if (item == null && material != null) {
            item = new ItemStack(material, Math.max(1, amount), data);
            if (name != null || !lore.isEmpty()) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (name != null) {
                        meta.setDisplayName(Text.color(name));
                    }
                    if (!lore.isEmpty()) {
                        meta.setLore(Text.color(lore));
                    }
                    item.setItemMeta(meta);
                }
            }
            if (glow) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
            }
            if (!nbt.isEmpty()) {
                item = NbtUtil.apply(item, nbt);
            }
        }
        if (item != null) {
            item = item.clone();
            item.setAmount(Math.max(1, Math.min(64, amount)));
        }
        return item;
    }

    public boolean matches(ItemStack item) {
        if (Items.empty(item)) {
            return false;
        }
        boolean niOnly = ni != null && !ni.trim().isEmpty();
        if (niOnly) {
            String id = NiHook.idOf(item);
            if (id == null || !Text.wildcard(ni.trim(), id)) {
                return false;
            }
        } else if (material != null) {
            if (item.getType() != material) {
                return false;
            }
            if (data >= 0 && item.getDurability() != data) {
                return false;
            }
        }
        boolean plainSpec = !niOnly && material != null
                && (name == null || name.trim().isEmpty())
                && lore.isEmpty() && nbt.isEmpty();
        if (strictPlain && plainSpec) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null && (meta.hasDisplayName() || meta.hasLore())) {
                return false;
            }
        }
        if (name != null && !name.trim().isEmpty()) {
            if (!Text.wildcard(Text.plain(color(name)), Items.plainName(item))) {
                return false;
            }
        }
        if (!lore.isEmpty()) {
            List<String> have = Items.plainLore(item);
            for (String want : lore) {
                String w = Text.plain(color(want));
                boolean found = false;
                for (String h : have) {
                    if (h.contains(w)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
        }
        for (Map.Entry<String, String> e : nbt.entrySet()) {
            String v = NbtUtil.read(item, e.getKey());
            if (v == null || !Text.wildcard(e.getValue(), v)) {
                return false;
            }
        }
        return true;
    }

    public String label() {
        if (name != null && !name.trim().isEmpty()) {
            return color(name);
        }
        if (ni != null && !ni.trim().isEmpty()) {
            return "§f" + ni;
        }
        return material == null ? "§f未知" : "§f" + Items.pretty(material);
    }

    public int getAmount() {
        return amount;
    }

    private static String color(String s) {
        return Text.color(s);
    }
}
