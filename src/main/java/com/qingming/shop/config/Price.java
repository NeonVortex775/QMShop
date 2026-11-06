package com.qingming.shop.config;

import com.qingming.shop.util.Cfg;

/** One cost line: items, points or money. */
public final class Price {

    public enum Kind {
        ITEM, POINTS, MONEY
    }

    private Kind kind = Kind.MONEY;
    private ItemSpec item;
    private int amount = 1;

    public static Price parse(Object sec) {
        if (!Cfg.isNode(sec)) {
            return null;
        }
        String raw = Cfg.str(sec, "type", "money");
        Kind k;
        if ("item".equalsIgnoreCase(raw) || "实物".equals(raw)) {
            k = Kind.ITEM;
        } else if ("points".equalsIgnoreCase(raw) || "点券".equals(raw)) {
            k = Kind.POINTS;
        } else {
            k = Kind.MONEY;
        }
        Price p = new Price();
        p.kind = k;
        p.amount = Math.max(1, Cfg.num(sec, "amount", 1));
        if (k == Kind.ITEM) {
            p.item = ItemSpec.parse(sec);
            if (p.item == null) {
                return null;
            }
        }
        return p;
    }

    public Kind getKind() {
        return kind;
    }

    public ItemSpec getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }

    public int total(int qty) {
        long v = (long) amount * (long) qty;
        return (int) Math.min(Integer.MAX_VALUE, v);
    }
}
