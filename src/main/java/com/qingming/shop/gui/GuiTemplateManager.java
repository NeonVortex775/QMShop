package com.qingming.shop.gui;

import com.qingming.shop.ShopPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Picks the template for a shop: exact match first, then "*". */
public final class GuiTemplateManager {

    private final ShopPlugin plugin;
    private final Map<String, GuiTemplate> byMatch = new LinkedHashMap<String, GuiTemplate>();

    public GuiTemplateManager(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    public File folder() {
        return new File(plugin.getDataFolder(), "gui");
    }

    public void loadAll() {
        byMatch.clear();
        File dir = folder();
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("无法创建 gui 目录: " + dir.getAbsolutePath());
            return;
        }

        File def = new File(dir, "默认.yml");
        if (!def.exists()) {
            try {
                plugin.saveResource("gui/默认.yml", false);
            } catch (Throwable ignored) {

            }
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (!f.isFile() || !f.getName().toLowerCase().endsWith(".yml")) {
                continue;
            }
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                GuiTemplate t = new GuiTemplate(f, cfg);
                if (byMatch.containsKey(t.match()) && !t.universal()) {
                    plugin.getLogger().warning("gui/" + f.getName() + " 的 match=" + t.match()
                            + " 与 " + byMatch.get(t.match()).file().getName() + " 重复，后者被忽略");
                    continue;
                }
                byMatch.put(t.match(), t);
            } catch (Throwable t) {
                plugin.getLogger().warning("gui/" + f.getName() + " 解析失败: " + t);
            }
        }
        if (byMatch.isEmpty()) {
            plugin.getLogger().info("gui 目录里没有可用模板，使用内置动态界面");
        } else {
            List<String> names = new ArrayList<String>();
            for (GuiTemplate t : byMatch.values()) {
                names.add(t.describe());
            }
            plugin.getLogger().info("已载入 " + names.size() + " 个自定义界面: " + names);
        }
    }

    public GuiTemplate pick(String shopId) {
        if (shopId != null) {
            for (GuiTemplate t : byMatch.values()) {
                if (!t.universal() && t.match().equalsIgnoreCase(shopId)) {
                    return t;
                }
            }
        }
        return byMatch.get("*");
    }

    public GuiTemplate byMatch(String match) {
        return byMatch.get(match);
    }

    public List<GuiTemplate> all() {
        return new ArrayList<GuiTemplate>(byMatch.values());
    }
}
