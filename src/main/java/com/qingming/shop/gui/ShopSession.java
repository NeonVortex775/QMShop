package com.qingming.shop.gui;

import com.qingming.shop.config.Product;
import com.qingming.shop.config.Shop;

import java.util.UUID;

/** Per-player state: page, selected product, quantity. */
public final class ShopSession {

    private final UUID player;
    private final Shop shop;

    private final int perPage;
    private int page;
    private int selected = -1;
    private int qty;

    public ShopSession(UUID player, Shop shop, int defaultQty, int perPage) {
        this.player = player;
        this.shop = shop;
        this.perPage = Math.max(1, perPage);
        this.qty = Math.max(1, defaultQty);
    }

    public UUID player() {
        return player;
    }

    public Shop shop() {
        return shop;
    }

    public int perPage() {
        return perPage;
    }

    public int pageCount() {
        int size = shop.getProducts().size();
        return Math.max(1, (size + perPage - 1) / perPage);
    }

    public int page() {
        return page;
    }

    public void page(int page) {
        this.page = Math.max(0, Math.min(pageCount() - 1, page));
    }

    public int selectedIndex() {
        return selected;
    }

    public void select(int index) {
        this.selected = index;
        if (index >= 0 && index < shop.getProducts().size()) {
            this.page = index / perPage;
        }
    }

    public void clearSelection() {
        this.selected = -1;
    }

    public Product selected() {
        return shop.at(selected);
    }

    public int qty() {
        return qty;
    }

    public void qty(int qty, int min, int max) {
        this.qty = Math.max(min, Math.min(max, qty));
    }

    public int firstIndexOfPage() {
        return page * perPage;
    }
}
