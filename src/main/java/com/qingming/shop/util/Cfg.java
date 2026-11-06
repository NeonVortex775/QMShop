package com.qingming.shop.util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** YAML helpers. Bukkit hands back plain Maps inside lists, so every read goes through here. */
public final class Cfg {

    private Cfg() {
    }

    public static boolean isNode(Object node) {
        return node instanceof ConfigurationSection || node instanceof Map;
    }

    public static Object get(Object node, String key) {
        if (node == null) {
            return null;
        }
        if (node instanceof ConfigurationSection) {
            return ((ConfigurationSection) node).get(key);
        }
        if (node instanceof Map) {
            return ((Map<?, ?>) node).get(key);
        }
        return null;
    }

    public static Object node(Object node, String key) {
        Object v = get(node, key);
        return isNode(v) ? v : null;
    }

    public static String str(Object node, String key, String def) {
        Object v = get(node, key);
        return v == null ? def : String.valueOf(v);
    }

    public static int num(Object node, String key, int def) {
        Object v = get(node, key);
        double d = Double.NaN;
        if (v instanceof Number) {
            d = ((Number) v).doubleValue();
        } else if (v != null) {
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                try {
                    d = Double.parseDouble(s);
                } catch (NumberFormatException ignored) {

                }
            }
        }
        if (Double.isNaN(d)) {
            return def;
        }

        long r = Math.round(d);
        if (r > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (r < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) r;
    }

    public static double dbl(Object node, String key, double def) {
        Object v = get(node, key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v != null) {
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException ignored) {

                }
            }
        }
        return def;
    }

    public static boolean bool(Object node, String key, boolean def) {
        Object v = get(node, key);
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        if (v != null) {
            return Boolean.parseBoolean(String.valueOf(v).trim());
        }
        return def;
    }

    public static List<?> list(Object node, String key) {
        Object v = get(node, key);
        return v instanceof List ? (List<?>) v : null;
    }

    public static List<String> strList(Object node, String key) {
        List<?> raw = list(node, key);
        List<String> out = new ArrayList<String>();
        if (raw != null) {
            for (Object o : raw) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
        }
        return out;
    }

    public static Map<String, Object> asMap(Object node) {
        if (node instanceof ConfigurationSection) {
            return new LinkedHashMap<String, Object>(((ConfigurationSection) node).getValues(false));
        }
        if (node instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) node).entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        return null;
    }
}
