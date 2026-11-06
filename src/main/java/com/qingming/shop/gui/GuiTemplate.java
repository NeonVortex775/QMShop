package com.qingming.shop.gui;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;

/** A template out of gui/*.yml. */
public final class GuiTemplate {

    private final File file;
    private final String id;
    private final String match;
    private final int width;
    private final int height;
    private final int rows;

    public GuiTemplate(File file, YamlConfiguration cfg) {
        this.file = file;
        this.id = file.getName().replaceAll("(?i)\\.yml$", "");
        String m = cfg.getString("match");
        this.match = (m == null || m.trim().isEmpty()) ? "*" : m.trim();

        List<Integer> size = cfg.getIntegerList("size");
        if (size.size() >= 2) {
            this.width = Math.max(0, size.get(0));
            this.height = Math.max(0, size.get(1));
        } else {
            this.width = 0;
            this.height = 0;
        }
        this.rows = Math.max(0, cfg.getInt("rows", 0));
    }

    public String guiName() {

        return "qmshop_" + id.toLowerCase(java.util.Locale.ROOT);
    }

    public File file() {
        return file;
    }

    public String id() {
        return id;
    }

    public String match() {
        return match;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int rows() {
        return rows;
    }

    public boolean universal() {
        return "*".equals(match);
    }

    public List<Integer> emptySize() {
        return Collections.emptyList();
    }

    public String describe() {
        return file.getName() + " (match=" + match + (rows > 0 ? ", rows=" + rows : "") + ")";
    }
}
