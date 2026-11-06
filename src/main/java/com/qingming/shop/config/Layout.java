package com.qingming.shop.config;

import com.qingming.shop.util.Cfg;

/** Screen geometry. Every coordinate on the built-in screen comes from these numbers. */
public final class Layout {

    public static final int CORNER = 88;
    public static final int TITLE_W = 320;
    public static final int TITLE_H = 52;
    public static final int ICON = 34;
    public static final int ICON_ITEM = 32;
    public static final int REQ = 28;
    public static final int REQ_GAP = 8;
    public static final int SEL_W = 116;
    public static final int SEL_H = 34;
    public static final int PANEL_W = 644;
    public static final int BTN_BUY_W = 120;
    public static final int BTN_CANCEL_W = 96;
    public static final int BTN_H = 36;
    public static final int ARROW = 26;

    public int width = 760;
    public int rows = 4;
    public int rowHeight = 48;
    public int rowGap = 6;
    public int margin = 22;
    public int titleHeight = 52;
    public int panelHeight = 92;
    public int maxRequirements = 4;

    public static Layout load(Object cfg) {
        Layout l = new Layout();
        if (Cfg.isNode(cfg)) {
            l.width = Cfg.num(cfg, "width", l.width);
            l.rows = Cfg.num(cfg, "rows", l.rows);
            l.rowHeight = Cfg.num(cfg, "row-height", l.rowHeight);
            l.rowGap = Cfg.num(cfg, "row-gap", l.rowGap);
            l.margin = Cfg.num(cfg, "margin", l.margin);
            l.titleHeight = Cfg.num(cfg, "title-height", l.titleHeight);
            l.panelHeight = Cfg.num(cfg, "panel-height", l.panelHeight);
            l.maxRequirements = Cfg.num(cfg, "max-requirements", l.maxRequirements);
        }
        l.sanitize();
        return l;
    }

    public void override(Object sec) {
        if (!Cfg.isNode(sec)) {
            return;
        }
        width = Cfg.num(sec, "width", width);
        rows = Cfg.num(sec, "rows", rows);
        rowHeight = Cfg.num(sec, "row-height", rowHeight);
        rowGap = Cfg.num(sec, "row-gap", rowGap);
        margin = Cfg.num(sec, "margin", margin);
        titleHeight = Cfg.num(sec, "title-height", titleHeight);
        panelHeight = Cfg.num(sec, "panel-height", panelHeight);
        maxRequirements = Cfg.num(sec, "max-requirements", maxRequirements);
        sanitize();
    }

    public Layout copy() {
        Layout l = new Layout();
        l.width = width;
        l.rows = rows;
        l.rowHeight = rowHeight;
        l.rowGap = rowGap;
        l.margin = margin;
        l.titleHeight = titleHeight;
        l.panelHeight = panelHeight;
        l.maxRequirements = maxRequirements;
        return l;
    }

    private void sanitize() {
        width = Math.max(480, Math.min(1200, width));
        rows = Math.max(1, Math.min(10, rows));
        rowHeight = Math.max(36, Math.min(80, rowHeight));
        rowGap = Math.max(0, Math.min(20, rowGap));
        margin = Math.max(14, Math.min(60, margin));
        titleHeight = Math.max(40, Math.min(90, titleHeight));
        panelHeight = Math.max(64, Math.min(140, panelHeight));
        maxRequirements = Math.max(1, Math.min(6, maxRequirements));
    }

    public int rowWidth() {
        return width - 2 * margin;
    }

    public int contentTop() {
        return margin + titleHeight;
    }

    public int rowY(int index) {
        return contentTop() + index * (rowHeight + rowGap);
    }

    public int rowsBottom() {
        return contentTop() + rows * rowHeight + (rows - 1) * rowGap;
    }

    public int panelY() {
        return rowsBottom() + 16;
    }

    public int height() {
        return panelY() + panelHeight + margin;
    }

    public int panelX() {
        return (width - PANEL_W) / 2;
    }

    public int iconX() {
        return margin + 12;
    }

    public int iconY(int rowIndex) {
        return rowY(rowIndex) + (rowHeight - ICON) / 2;
    }

    public int nameX() {
        return margin + 56;
    }

    public int nameY(int rowIndex) {
        return rowY(rowIndex) + rowHeight / 2 - 5;
    }

    public int selectX() {
        return width - margin - 12 - SEL_W;
    }

    public int selectY(int rowIndex) {
        return rowY(rowIndex) + (rowHeight - SEL_H) / 2;
    }

    public int reqRight() {
        return selectX() - 14;
    }

    public int reqStartX(int count) {
        int n = Math.max(1, Math.min(count, maxRequirements));
        return reqRight() - (n * REQ + (n - 1) * REQ_GAP);
    }

    public int reqY(int rowIndex) {
        return rowY(rowIndex) + (rowHeight - REQ) / 2;
    }

    public int panelCenterY() {
        return panelY() + 34;
    }

    public int qtyLabelX() {
        return panelX() + 26;
    }

    public int qtyLabelY() {
        return panelCenterY() - 6;
    }

    public int arrowY() {
        return panelCenterY() - ARROW / 2;
    }

    public int arrowLeftX(int index) {
        return panelX() + 82 + index * 32;
    }

    public int arrowRightX(int index) {
        return panelX() + 258 + index * 32;
    }

    public int inputX() {
        return panelX() + 146;
    }

    public int inputY() {
        return panelCenterY() - 15;
    }

    public int inputW() {
        return 104;
    }

    public int inputH() {
        return 30;
    }

    public static final int INPUT_PAD = 14;

    public static final int INPUT_FONT = 16;

    public int inputTextX() {
        return inputX() + INPUT_PAD;
    }

    public int inputTextY() {
        return inputY() + (inputH() - INPUT_FONT) / 2;
    }

    public int totalX() {
        return panelX() + 84;
    }

    public int totalY() {
        return panelY() + 62;
    }

    public int buyX() {
        return panelX() + PANEL_W - 22 - BTN_BUY_W;
    }

    public int cancelX() {
        return buyX() - 12 - BTN_CANCEL_W;
    }

    public int buttonY() {
        return panelCenterY() - BTN_H / 2;
    }

    public int pageLeftX() {
        return (width - TITLE_W) / 2 - 40;
    }

    public int pageRightX() {
        return (width + TITLE_W) / 2 + 14;
    }

    public int pageButtonY() {
        return 25;
    }
}
