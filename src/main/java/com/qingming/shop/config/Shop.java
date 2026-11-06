package com.qingming.shop.config;

import java.util.ArrayList;
import java.util.List;

/** A shop file: title, layout and products. */
public final class Shop {

    private final String id;
    private String title;
    private Layout layout = new Layout();
    private final List<Product> products = new ArrayList<Product>();

    public Shop(String id) {
        this.id = id;
        this.title = id;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public List<Product> getProducts() {
        return products;
    }

    public int pageCount() {
        int per = Math.max(1, layout.rows);
        return Math.max(1, (products.size() + per - 1) / per);
    }

    public int indexOf(Product product) {
        return products.indexOf(product);
    }

    public Product at(int index) {
        return index < 0 || index >= products.size() ? null : products.get(index);
    }
}
