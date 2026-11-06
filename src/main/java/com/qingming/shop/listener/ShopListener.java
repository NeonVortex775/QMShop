package com.qingming.shop.listener;

import com.qingming.shop.ShopPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Clears the session when a player quits. */
public final class ShopListener implements Listener {

    private final ShopPlugin plugin;

    public ShopListener(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        plugin.forget(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent e) {
        plugin.forget(e.getPlayer());
    }
}
