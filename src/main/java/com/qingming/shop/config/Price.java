package com.qingming.shop.config;

import com.qingming.shop.util.Cfg;
import com.qingming.shop.util.Log;

import java.text.DecimalFormat;

/** One cost line: items, points or money. */
public final class Price {

    public enum Kind {
        ITEM, POINTS, MONEY
    }

    private static DecimalFormat moneyFmt = new DecimalFormat("0.##");

    private Kind kind = Kind.MONEY;
    private ItemSpec item;
    private int amount = 1;

    private double money = 1;

    public static void moneyDecimals(int decimals) {
        int d = Math.max(0, Math.min(6, decimals));
        StringBuilder pat = new StringBuilder("0");
        if (d > 0) {
            pat.append('.');
            for (int i = 0; i < d; i++) {
                pat.append('#');
            }
        }
        moneyFmt = new DecimalFormat(pat.toString());
    }

    public static String moneyText(double v) {
        return moneyFmt.format(v);
    }

    public static Price parse(Object sec) {
        if (!Cfg.isNode(sec)) {
            return null;
        }
        String raw = Cfg.str(sec, "type", "money");
        String t = raw == null ? "money" : raw.trim().toLowerCase();
        Kind k;
        if ("item".equals(t) || "实物".equals(raw) || "material".equals(t)) {
            k = Kind.ITEM;
        } else if ("points".equals(t) || "point".equals(t) || "点券".equals(raw)) {
            k = Kind.POINTS;
        } else if ("money".equals(t) || "coin".equals(t) || "coins".equals(t)
                || "vault".equals(t) || "金币".equals(raw)) {
            k = Kind.MONEY;
        } else {

            Log.warn("[青冥商城] 价格 type 写错了: " + raw
                    + "（只认 item / points / money），这一条价格已被忽略");
            return null;
        }
        Price p = new Price();
        p.kind = k;
        if (k == Kind.MONEY) {
            p.money = Math.max(0.01, Cfg.dbl(sec, "amount", 1));
            p.amount = (int) Math.max(1, Math.round(p.money));
        } else {
            p.amount = Math.max(1, Cfg.num(sec, "amount", 1));
        }
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

    public double moneyTotal(int qty) {
        return money * Math.max(0, qty);
    }

    public String unitText() {
        if (kind == Kind.MONEY) {
            return moneyText(money);
        }
        return String.valueOf(amount);
    }
}
