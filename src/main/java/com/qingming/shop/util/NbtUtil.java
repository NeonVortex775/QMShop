package com.qingming.shop.util;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** NBT through NBTAPI reflection - optional dependency. */
public final class NbtUtil {

    private static Logger log;
    private static boolean ready = false;
    private static boolean broken = false;
    private static int failures = 0;

    private static Constructor<?> mItemCtor;
    private static Method mHasTag;
    private static Method mGetType;
    private static Method mGetItem;
    private static Method mSetString;
    private static Method mSetInt;
    private static Method mSetLong;

    private NbtUtil() {
    }

    public static void init(Logger logger) {
        log = logger;
    }

    private static boolean prepare() {
        if (broken) {
            return false;
        }
        if (ready) {
            return true;
        }
        try {
            Class<?> c = Class.forName("de.tr7zw.nbtapi.NBTItem");
            mItemCtor = c.getConstructor(ItemStack.class);
            mHasTag = c.getMethod("hasTag", String.class);
            mGetType = c.getMethod("getType", String.class);
            mGetItem = c.getMethod("getItem");
            mSetString = c.getMethod("setString", String.class, String.class);
            mSetInt = method(c, "setInteger", String.class, Integer.class, int.class);
            mSetLong = method(c, "setLong", String.class, Long.class, long.class);
            ready = true;
            return true;
        } catch (Throwable t) {
            broken = true;
            if (log != null) {
                log.log(Level.WARNING, "[NBT] 未找到 NBTAPI，nbt 匹配与 nbt 写入已停用", t);
            }
            return false;
        }
    }

    private static Method method(Class<?> c, String name, Class<?> a, Class<?> boxed, Class<?> primitive) {
        try {
            return c.getMethod(name, a, boxed);
        } catch (NoSuchMethodException ignored) {
            try {
                return c.getMethod(name, a, primitive);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    }

    private static Object newItem(ItemStack item) throws Exception {
        return mItemCtor.newInstance(item);
    }

    public static String read(ItemStack item, String path) {
        if (item == null || path == null || path.trim().isEmpty() || !prepare()) {
            return null;
        }
        try {
            Object nbt = newItem(item);
            Object has = mHasTag.invoke(nbt, path);
            if (!(has instanceof Boolean) || !((Boolean) has)) {
                return null;
            }
            Object type = mGetType.invoke(nbt, path);
            int id = tagId(type);
            return readByType(nbt, path, id);
        } catch (Throwable t) {
            warn("读取失败: " + path, t);
            return null;
        }
    }

    private static int tagId(Object type) {
        if (type == null) {
            return -1;
        }
        try {
            Object id = type.getClass().getMethod("getId").invoke(type);
            if (id instanceof Number) {
                return ((Number) id).intValue();
            }
        } catch (Throwable ignored) {

        }
        String name = String.valueOf(type).toUpperCase();
        if (name.contains("STRING")) {
            return 8;
        }
        if (name.contains("INT")) {
            return 3;
        }
        if (name.contains("LONG")) {
            return 4;
        }
        if (name.contains("DOUBLE")) {
            return 6;
        }
        if (name.contains("FLOAT")) {
            return 5;
        }
        if (name.contains("SHORT")) {
            return 2;
        }
        if (name.contains("BYTE")) {
            return 1;
        }
        return -1;
    }

    private static String readByType(Object nbt, String path, int id) throws Exception {
        switch (id) {
            case 8:
                return get(nbt, "getString", path);
            case 3:
                return get(nbt, "getInteger", path);
            case 4:
                return get(nbt, "getLong", path);
            case 6:
                return get(nbt, "getDouble", path);
            case 5:
                return get(nbt, "getFloat", path);
            case 2:
                return get(nbt, "getShort", path);
            case 1:
                return get(nbt, "getByte", path);
            default:
                return null;
        }
    }

    private static String get(Object nbt, String method, String path) throws Exception {
        Object v = nbt.getClass().getMethod(method, String.class).invoke(nbt, path);
        return v == null ? null : String.valueOf(v);
    }

    public static ItemStack apply(ItemStack item, Map<String, String> values) {
        if (item == null || values == null || values.isEmpty() || !prepare()) {
            return item;
        }
        try {
            Object nbt = newItem(item);
            for (Map.Entry<String, String> e : values.entrySet()) {
                String key = e.getKey();
                if (key == null || key.trim().isEmpty()) {
                    continue;
                }
                String val = e.getValue() == null ? "" : e.getValue();
                String k = key.trim();
                if (val.matches("-?\\d{1,9}") && mSetInt != null) {
                    mSetInt.invoke(nbt, k, Integer.valueOf(val));
                } else if (val.matches("-?\\d{10,18}") && mSetLong != null) {
                    mSetLong.invoke(nbt, k, Long.valueOf(val));
                } else if (mSetString != null) {
                    mSetString.invoke(nbt, k, val);
                }
            }
            Object result = mGetItem.invoke(nbt);
            return result instanceof ItemStack ? (ItemStack) result : item;
        } catch (Throwable t) {
            warn("写入失败", t);
            return item;
        }
    }

    private static void warn(String what, Throwable t) {
        failures++;
        if (log != null && failures <= 3) {
            log.log(Level.WARNING, "[NBT] " + what + "（第 " + failures + " 次）", t);
        }
    }

    public static boolean available() {
        return prepare();
    }
}
