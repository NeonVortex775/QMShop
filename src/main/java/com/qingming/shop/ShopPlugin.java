package com.qingming.shop;

import com.qingming.shop.command.ShopCommand;
import com.qingming.shop.config.ItemSpec;
import com.qingming.shop.config.Price;
import com.qingming.shop.config.Product;
import com.qingming.shop.config.Shop;
import com.qingming.shop.config.ShopManager;
import com.qingming.shop.economy.Payment;
import com.qingming.shop.gui.CustomGui;
import com.qingming.shop.gui.GuiTemplate;
import com.qingming.shop.gui.GuiTemplateManager;
import com.qingming.shop.gui.ShopScreen;
import com.qingming.shop.gui.ShopSession;
import com.qingming.shop.listener.ShopListener;
import com.qingming.shop.listener.ShopPacketListener;
import com.qingming.shop.util.Items;
import com.qingming.shop.util.Log;
import com.qingming.shop.util.NbtUtil;
import com.qingming.shop.util.NiHook;
import com.qingming.shop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Plugin entry point: shops, economy, GUI templates, sessions. */
public final class ShopPlugin extends JavaPlugin {

    public static final int MAX_ITEMS_PER_BUY = 64;

    private ShopManager shops;
    private Payment payment;
    private ShopCommand command;
    private GuiTemplateManager guiTemplates;

    private final Map<java.util.UUID, ShopSession> sessions =
            new ConcurrentHashMap<java.util.UUID, ShopSession>();
    private final Map<java.util.UUID, ShopScreen> screens =
            new ConcurrentHashMap<java.util.UUID, ShopScreen>();

    private ItemSpec pointsIcon;
    private ItemSpec moneyIcon;

    @Override
    public void onEnable() {
        Log.init(getLogger());
        NbtUtil.init(getLogger());
        NiHook.init(getLogger());
        saveDefaultConfig();
        ItemSpec.strictPlainMatch(getConfig().getBoolean("match.require-plain-item", true));
        Price.moneyDecimals(getConfig().getInt("economy.money-decimals", 2));

        shops = new ShopManager(this);
        shops.loadAll();

        guiTemplates = new GuiTemplateManager(this);
        guiTemplates.loadAll();

        payment = new Payment(this);
        payment.setup();

        loadIcons();

        command = new ShopCommand(this);
        if (getCommand("shop") != null) {
            getCommand("shop").setExecutor(command);
            getCommand("shop").setTabCompleter(command);
        }
        Bukkit.getPluginManager().registerEvents(new ShopListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ShopPacketListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("DragonCore") == null) {
            getLogger().warning("未找到 DragonCore —— 商城界面无法打开（本插件依赖龙核渲染）");
        }
        getLogger().info("青冥商城已启用（" + shops.all().size() + " 个商店，"
                + "点券 " + (payment.pointsReady() ? "就绪" : "不可用")
                + "，金币 " + (payment.moneyReady() ? "就绪" : "不可用") + "）");
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ShopScreen screen = screens.get(p.getUniqueId());
            if (screen != null) {
                try {
                    screen.closeGui(p);
                } catch (Throwable ignored) {
                }
            }
        }
        screens.clear();
        sessions.clear();
    }

    private void loadIcons() {
        pointsIcon = ItemSpec.parse(getConfig().getConfigurationSection("icons.points"));
        moneyIcon = ItemSpec.parse(getConfig().getConfigurationSection("icons.money"));
    }

    public String texturePrefix() {
        String p = getConfig().getString("gui.texture-prefix", "商城/");
        if (p == null) {
            return "";
        }
        if (!p.isEmpty() && !p.endsWith("/")) {
            p = p + "/";
        }
        return p;
    }

    public String texture(String name) {
        return texturePrefix() + name;
    }

    public String msg(String key) {
        String s = getConfig().getString("messages." + key);
        return s == null ? "" : Text.color(s);
    }

    public int step() {
        return Math.max(1, getConfig().getInt("quantity.step", 1));
    }

    public int bigStep() {
        return Math.max(1, getConfig().getInt("quantity.big-step", 10));
    }

    public int minQty() {

        return Math.max(1, Math.min(MAX_ITEMS_PER_BUY, getConfig().getInt("quantity.min", 1)));
    }

    public int maxQty() {
        int cfg = getConfig().getInt("quantity.max", MAX_ITEMS_PER_BUY);
        return Math.max(minQty(), Math.min(MAX_ITEMS_PER_BUY, cfg));
    }

    public int maxQty(Product product) {
        int amt = product == null ? 1 : Math.max(1, product.getAmount());
        int byStack = Math.max(1, MAX_ITEMS_PER_BUY / amt);
        return Math.max(minQty(), Math.min(maxQty(), byStack));
    }

    public void clampQty(ShopSession session) {
        if (session == null) {
            return;
        }
        session.qty(session.qty(), minQty(), maxQty(session.selected()));
    }

    public void sound(Player p, String key) {
        String name = getConfig().getString("sounds." + key);
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        try {
            p.playSound(p.getLocation(), Sound.valueOf(name.trim().toUpperCase()), 0.6f, 1.0f);
        } catch (Throwable ignored) {
        }
    }

    public ShopManager shops() {
        return shops;
    }

    public Payment payment() {
        return payment;
    }

    public Map<java.util.UUID, ShopSession> sessions() {
        return sessions;
    }

    public void reload() {
        reloadConfig();
        loadIcons();

        if (payment != null) {
            payment.setup();
        }
        ItemSpec.strictPlainMatch(getConfig().getBoolean("match.require-plain-item", true));
        Price.moneyDecimals(getConfig().getInt("economy.money-decimals", 2));
        shops.loadAll();
        if (guiTemplates != null) {
            guiTemplates.loadAll();
        }
        screens.clear();
        sessions.clear();
    }

    public ItemStack pointsIcon(Player p, int amount) {
        if (pointsIcon == null) {
            return null;
        }
        return pointsIcon.build(p, amount);
    }

    public ItemStack moneyIcon(Player p, int amount) {
        if (moneyIcon == null) {
            return null;
        }
        return moneyIcon.build(p, amount);
    }

    public ShopScreen open(Player p, Shop shop) {

        GuiTemplate tpl = guiPackets() && guiTemplates != null ? guiTemplates.pick(shop.getId()) : null;

        int perPage = shop.getLayout().rows;
        if (tpl != null && tpl.rows() > 0) {
            perPage = Math.min(tpl.rows(), layoutRows(shop));
        }

        ShopSession session = new ShopSession(p.getUniqueId(), shop,
                getConfig().getInt("quantity.default", 1), perPage);
        clampQty(session);
        sessions.put(p.getUniqueId(), session);

        if (tpl != null) {
            ShopScreen old = screens.remove(p.getUniqueId());
            if (old != null) {
                try {
                    old.closeGui(p);
                } catch (Throwable ignored) {
                }
            }
            if (CustomGui.open(this, p, session, tpl)) {
                return null;
            }
            getLogger().warning("自定义界面 " + tpl.file().getName() + " 打开失败，改用内置界面");
        }

        ShopScreen screen = new ShopScreen(this, p, session);
        screens.put(p.getUniqueId(), screen);
        screen.openGui(p);
        return screen;
    }

    private static int layoutRows(Shop shop) {
        return Math.max(1, shop.getLayout().rows);
    }

    public void refresh(Player p, ShopSession session) {
        if (p == null || session == null) {
            return;
        }
        ShopScreen screen = screens.get(p.getUniqueId());
        if (screen != null) {
            screen.refresh();
            return;
        }
        CustomGui.refresh(this, p, session);
    }

    public GuiTemplateManager guiTemplates() {
        return guiTemplates;
    }

    public boolean guiPackets() {
        return getConfig().getBoolean("gui.custom-enabled", true);
    }

    public void buy(Player p, ShopScreen screen, ShopSession session) {
        if (session == null) {
            return;
        }

        if (!p.hasPermission("qingmingshop.use") && !p.isOp()) {
            p.sendMessage(msg("no-permission"));
            sound(p, "fail");
            return;
        }
        Product product = session.selected();
        if (product == null) {
            p.sendMessage(msg("need-select"));
            sound(p, "fail");
            return;
        }
        String perm = product.getPermission();
        if (perm != null && !p.hasPermission(perm) && !p.isOp()) {
            p.sendMessage(msg("no-shop-permission"));
            sound(p, "fail");
            return;
        }
        if (product.getPrice().isEmpty()) {

            p.sendMessage(msg("no-price"));
            getLogger().warning("商品「" + product.getDisplay().label() + "」没有可用的 price，已拒绝购买");
            sound(p, "fail");
            return;
        }

        int qty = Math.max(minQty(), Math.min(maxQty(product), session.qty()));
        if (qty != session.qty()) {
            session.qty(qty, minQty(), maxQty(product));
        }

        ItemStack base = product.getDisplay().build(p, 1);
        if (base == null) {
            p.sendMessage(msg("item-failed"));
            getLogger().warning("商品「" + product.getDisplay().label()
                    + "」的物品生成失败（ni 写错或 NeigeItems 未安装），已拒绝购买");
            sound(p, "fail");
            return;
        }

        String err = payment.check(p, product, qty);
        if (err != null) {
            p.sendMessage(err);
            sound(p, "fail");
            return;
        }
        if (!payment.charge(p, product, qty)) {
            p.sendMessage(msg("not-enough-item"));
            sound(p, "fail");
            return;
        }

        long total = (long) product.getAmount() * (long) qty;
        total = Math.max(1, total);
        String productName = product.name(p);

        ItemStack out = base.clone();
        out.setAmount(1);
        int left = (int) total;
        boolean dropped = false;
        while (left > 0) {
            int n = Math.min(64, left);
            ItemStack give = out.clone();
            give.setAmount(n);
            HashMap<Integer, ItemStack> rest = p.getInventory().addItem(give);
            for (ItemStack r : rest.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), r);
                dropped = true;
            }
            left -= n;
        }
        if (dropped) {
            p.sendMessage(msg("inventory-full"));
        }

        for (String cmd : product.getCommands()) {
            String run = cmd.replace("%player%", p.getName())
                    .replace("%amount%", String.valueOf(total))
                    .replace("%qty%", String.valueOf(qty));
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run);
            } catch (Throwable t) {
                getLogger().warning("商品指令执行失败: " + run + " -> " + t);
            }
        }

        p.sendMessage(msg("bought")
                .replace("%item%", productName)
                .replace("%amount%", String.valueOf(total)));
        sound(p, "buy");

        refresh(p, session);
    }

    public ShopScreen screen(Player p) {
        return screens.get(p.getUniqueId());
    }

    public void forget(Player p) {
        if (p == null) {
            return;
        }
        sessions.remove(p.getUniqueId());
        screens.remove(p.getUniqueId());
    }

    public ItemSpec pointsIconSpec() {
        return pointsIcon;
    }

    public ItemSpec moneyIconSpec() {
        return moneyIcon;
    }

    public ShopCommand command() {
        return command;
    }

    @SuppressWarnings("unused")
    private static boolean empty(ItemStack it) {
        return Items.empty(it);
    }
}
