package com.qingming.shop.util;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/** NeigeItems bridge through reflection - optional dependency. */
public final class NiHook {

    private static Logger log;
    private static boolean tried;
    private static Object manager;
    private static Method mWithPlayer;
    private static Method mPlain;

    private NiHook() {
    }

    public static void init(Logger logger) {
        log = logger;
    }

    public static boolean available() {
        prepare();
        return manager != null;
    }

    private static synchronized void prepare() {
        if (tried) {
            return;
        }
        tried = true;
        try {
            Class<?> c = Class.forName("pers.neige.neigeitems.manager.ItemManager");
            manager = c.getField("INSTANCE").get(null);
            try {
                mWithPlayer = c.getMethod("getItemStack", String.class, OfflinePlayer.class);
            } catch (NoSuchMethodException ignored) {
            }
            mPlain = c.getMethod("getItemStack", String.class);
        } catch (Throwable t) {
            manager = null;
            if (log != null) {
                log.log(Level.INFO, "[NI] 未找到 NeigeItems，ni: 配置项不可用");
            }
        }
    }

    public static ItemStack get(String id, OfflinePlayer player) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        prepare();
        if (manager == null) {
            return null;
        }
        try {
            Object result = null;
            if (mWithPlayer != null && player != null) {
                result = mWithPlayer.invoke(manager, id.trim(), player);
            }
            if (result == null && mPlain != null) {
                result = mPlain.invoke(manager, id.trim());
            }
            return result instanceof ItemStack ? ((ItemStack) result).clone() : null;
        } catch (Throwable t) {
            if (log != null) {
                log.log(Level.WARNING, "[NI] 取物品失败: " + id, t);
            }
            return null;
        }
    }

    public static boolean has(String id) {
        prepare();
        if (manager == null) {
            return false;
        }
        try {
            Method m = manager.getClass().getMethod("hasItem", String.class);
            Object r = m.invoke(manager, id);
            return r instanceof Boolean && (Boolean) r;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String where() {
        return manager == null ? "未安装" : "已连接";
    }

    public static String idOf(ItemStack item) {
        if (item == null) {
            return null;
        }
        prepare();
        if (manager == null) {
            return null;
        }
        try {
            Method m = manager.getClass().getMethod("getItemId", ItemStack.class);
            Object r = m.invoke(manager, item);
            return r instanceof String ? (String) r : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
