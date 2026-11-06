package com.qingming.shop.gui;

import com.qingming.shop.ShopPlugin;
import com.qingming.shop.config.Layout;
import com.qingming.shop.config.Price;
import com.qingming.shop.config.Product;
import com.qingming.shop.config.Shop;
import com.qingming.shop.util.Items;
import eos.moe.dragoncore.api.easygui.EasyScreen;
import eos.moe.dragoncore.api.easygui.component.EasyButton;
import eos.moe.dragoncore.api.easygui.component.EasyImage;
import eos.moe.dragoncore.api.easygui.component.EasyLabel;
import eos.moe.dragoncore.api.easygui.component.EasySlot;
import eos.moe.dragoncore.api.easygui.component.EasyTextField;
import eos.moe.dragoncore.api.easygui.component.listener.ClickListener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The built-in screen. All components are generated at runtime and sent via EasyScreen. */
public final class ShopScreen extends EasyScreen {

    private interface Act {
        void run(Player player);
    }

    private final ShopPlugin plugin;
    private final Player player;
    private final Shop shop;
    private final ShopSession session;
    private final Layout L;

    private boolean pending;

    private boolean keepSession;

    public ShopScreen(ShopPlugin plugin, Player player, ShopSession session) {
        super(plugin.texture("边框_中.png"), session.shop().getLayout().width,
                session.shop().getLayout().height());
        this.plugin = plugin;
        this.player = player;
        this.session = session;
        this.shop = session.shop();
        this.L = shop.getLayout();
        rebuild();
    }

    public void refresh() {
        if (pending) {
            return;
        }
        pending = true;
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                pending = false;
                apply();
            }
        });
    }

    private void apply() {
        if (player == null || !player.isOnline()) {
            return;
        }
        rebuild();
        boolean reopenMode = "reopen".equalsIgnoreCase(
                plugin.getConfig().getString("refresh-mode", "update"));
        if (!reopenMode && updateGui(player)) {

            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline()
                            && EasyScreen.getOpenedScreen(player) == null
                            && plugin.sessions().get(player.getUniqueId()) == session) {
                        EasyScreen.setOpenedScreen(player, ShopScreen.this);
                    }
                }
            }, 3L);
            return;
        }
        reopen();
    }

    private void reopen() {
        keepSession = true;
        EasyScreen.closeScreen(player);
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                keepSession = false;
                if (player.isOnline() && plugin.sessions().get(player.getUniqueId()) == session) {
                    EasyScreen.setOpenedScreen(player, ShopScreen.this);
                }
            }
        }, 2L);
    }

    private void rebuild() {
        getComponents().clear();
        frame();
        title();
        rows();
        panel();
    }

    @Override
    public void onClose(Player p) {
        if (keepSession) {
            return;
        }

        if (plugin.sessions().get(p.getUniqueId()) == session) {
            plugin.sessions().remove(p.getUniqueId());
        }
    }

    @Override
    public Map<String, Map<String, Object>> build(Player p, Map<String, String> functions) {
        Map<String, Map<String, Object>> map = super.build(p, functions);
        for (Map<String, Object> c : map.values()) {
            if ("textbox".equals(c.get("type"))) {

                c.put("drawBackground", Boolean.FALSE);
                c.put("color", "236,228,200");
                c.put("length", Integer.valueOf(6));

                c.put("scale", Integer.valueOf(2));
                continue;
            }
            if (!"slot".equals(c.get("type"))) {
                continue;
            }
            Object id = c.get("identifier");
            String sid = id == null ? "" : String.valueOf(id);
            int size = sid.startsWith("qs_req") ? Layout.REQ : Layout.ICON_ITEM;
            c.put("width", size);
            c.put("height", size);
        }
        return map;
    }

    private String tex(String name) {
        return plugin.texture(name);
    }

    private EasyImage image(String name, int x, int y, int w, int h) {
        EasyImage img = new EasyImage(x, y, w, h, tex(name));
        addComponent(img);
        return img;
    }

    private EasyLabel label(String text, int x, int y, int scale, boolean center) {
        List<String> texts = new ArrayList<String>();
        texts.add(text);
        EasyLabel l = new EasyLabel(x, y, scale, texts, center);
        addComponent(l);
        return l;
    }

    private void slot(String identifier, int x, int y, ItemStack item) {
        if (Items.empty(item)) {
            return;
        }
        EasySlot s = new EasySlot(x, y, identifier);
        s.setItemStack(item);
        addComponent(s);
    }

    private void button(String normal, String hover, int x, int y, int w, int h, final Act act) {
        EasyButton b = new EasyButton(x, y, w, h, tex(normal), tex(hover)) {
            @Override
            public void onClick(Player p, ClickListener.Type type) {
                act.run(p);
            }
        };
        addComponent(b);
    }

    private void buttonText(int x, int y, int w, int h, String text) {
        label(text, x + w / 2, y + h / 2 - 4, 1, true);
    }

    private void frame() {
        int W = L.width;
        int H = L.height();
        int c = Layout.CORNER;
        image("边框_左上.png", 0, 0, c, c);
        image("边框_右上.png", W - c, 0, c, c);
        image("边框_左下.png", 0, H - c, c, c);
        image("边框_右下.png", W - c, H - c, c, c);
        image("边框_上.png", c, 0, W - 2 * c, c);
        image("边框_下.png", c, H - c, W - 2 * c, c);
        image("边框_左.png", 0, c, c, H - 2 * c);
        image("边框_右.png", W - c, c, c, H - 2 * c);
    }

    private void title() {
        int x = (L.width - Layout.TITLE_W) / 2;
        image("标题牌.png", x, 12, Layout.TITLE_W, Layout.TITLE_H);
        label(shop.getTitle(), L.width / 2, 12 + Layout.TITLE_H / 2 - 8, 2, true);

        if (shop.pageCount() > 1) {
            int y = L.pageButtonY();
            if (session.page() > 0) {
                button("箭头_左.png", "箭头_左_悬停.png", L.pageLeftX(), y, 26, 26, new Act() {
                    public void run(Player p) {
                        session.page(session.page() - 1);
                        plugin.sound(p, "click");
                        refresh();
                    }
                });
            }
            if (session.page() < shop.pageCount() - 1) {
                button("箭头_右.png", "箭头_右_悬停.png", L.pageRightX(), y, 26, 26, new Act() {
                    public void run(Player p) {
                        session.page(session.page() + 1);
                        plugin.sound(p, "click");
                        refresh();
                    }
                });
            }
            label("§7" + (session.page() + 1) + " / " + shop.pageCount(),
                    L.pageRightX() + 13, y + 26 + 2, 1, true);
        }
    }

    private void rows() {
        int per = L.rows;
        int first = session.firstIndexOfPage();
        for (int i = 0; i < per; i++) {
            final int index = first + i;
            final Product product = shop.at(index);
            if (product == null) {
                break;
            }
            int y = L.rowY(i);
            image("行条.png", L.margin, y, L.rowWidth(), L.rowHeight);

            image("格_34.png", L.iconX(), L.iconY(i), Layout.ICON, Layout.ICON);
            slot("qs_icon_" + i, L.iconX() + 1, L.iconY(i) + 1, product.icon(player));

            label(product.name(player), L.nameX(), L.nameY(i), 1, false);

            List<Price> prices = product.getPrice();
            int n = Math.min(prices.size(), L.maxRequirements);
            int startX = L.reqStartX(n);
            for (int j = 0; j < n; j++) {
                int x = startX + j * (Layout.REQ + Layout.REQ_GAP);
                image("格_28.png", x, L.reqY(i), Layout.REQ, Layout.REQ);
                slot("qs_req_" + i + "_" + j, x + 1, L.reqY(i) + 1, requirementIcon(prices.get(j)));
            }

            boolean selected = session.selectedIndex() == index;
            button("按钮_选择.png", "按钮_选择_悬停.png",
                    L.selectX(), L.selectY(i), Layout.SEL_W, Layout.SEL_H, new Act() {
                        public void run(Player p) {
                            session.select(index);
                            plugin.sound(p, "click");
                            refresh();
                        }
                    });
            buttonText(L.selectX(), L.selectY(i), Layout.SEL_W, Layout.SEL_H,
                    selected ? "§6已选" : "§e选择");
        }
    }

    private ItemStack requirementIcon(Price price) {
        if (price.getKind() == Price.Kind.ITEM) {
            return price.getItem().build(player, price.getAmount());
        }
        if (price.getKind() == Price.Kind.POINTS) {
            return plugin.pointsIcon(player, price.getAmount());
        }
        return plugin.moneyIcon(player, price.getAmount());
    }

    private void panel() {
        int px = L.panelX();
        int py = L.panelY();
        image("购买框.png", px, py, Layout.PANEL_W, L.panelHeight);

        final Product product = session.selected();
        if (product == null) {
            label("§7点击右侧「选择」，在这里设置购买数量", L.width / 2, py + L.panelHeight / 2 - 4, 1, true);
            return;
        }

        label("§e数量", L.qtyLabelX(), L.qtyLabelY(), 1, false);

        arrowButton("箭头_左2.png", L.arrowLeftX(0), -plugin.bigStep());
        arrowButton("箭头_左.png", L.arrowLeftX(1), -plugin.step());
        arrowButton("箭头_右.png", L.arrowRightX(0), plugin.step());
        arrowButton("箭头_右2.png", L.arrowRightX(1), plugin.bigStep());

        image("输入框.png", L.inputX(), L.inputY(), L.inputW(), L.inputH());

        EasyTextField field = new EasyTextField(L.inputTextX(), L.inputTextY(),
                L.inputW() - Layout.INPUT_PAD, L.inputH(), String.valueOf(session.qty())) {
            @Override
            public void onTextChange(String text) {
                String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
                if (digits.isEmpty()) {
                    return;
                }
                try {
                    int v = Integer.parseInt(digits);
                    session.qty(v, plugin.minQty(), plugin.maxQty());
                } catch (NumberFormatException ignored) {
                }
            }
        };
        addComponent(field);

        label("§7" + session.qty() + " × " + product.name(player) + "   §8合计: "
                + product.priceLine(session.qty()), L.totalX(), L.totalY(), 1, false);

        button("按钮_购买.png", "按钮_购买_悬停.png", L.buyX(), L.buttonY(),
                Layout.BTN_BUY_W, Layout.BTN_H, new Act() {
                    public void run(Player p) {
                        plugin.buy(p, ShopScreen.this, session);
                    }
                });
        buttonText(L.buyX(), L.buttonY(), Layout.BTN_BUY_W, Layout.BTN_H, "§e购买");

        button("按钮_取消.png", "按钮_取消_悬停.png", L.cancelX(), L.buttonY(),
                Layout.BTN_CANCEL_W, Layout.BTN_H, new Act() {
                    public void run(Player p) {
                        session.clearSelection();
                        plugin.sound(p, "click");
                        refresh();
                    }
                });
        buttonText(L.cancelX(), L.buttonY(), Layout.BTN_CANCEL_W, Layout.BTN_H, "§7取消");
    }

    private void arrowButton(String tex, int x, final int delta) {
        button(tex, tex.replace(".png", "_悬停.png"), x, L.arrowY(), Layout.ARROW, Layout.ARROW,
                new Act() {
                    public void run(Player p) {
                        session.qty(session.qty() + delta, plugin.minQty(), plugin.maxQty());
                        plugin.sound(p, "click");
                        refresh();
                    }
                });
    }

    public String debug() {
        return shop.getId() + " " + L.width + "x" + L.height()
                + " rows=" + L.rows + " 第" + (session.page() + 1) + "/" + shop.pageCount() + "页";
    }

    @SuppressWarnings("unused")
    private static List<String> lines(String... s) {
        return Arrays.asList(s);
    }
}
