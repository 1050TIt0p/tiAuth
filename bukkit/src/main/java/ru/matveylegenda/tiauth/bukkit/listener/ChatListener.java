package ru.matveylegenda.tiauth.bukkit.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.config.MainConfig;

public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!AuthCache.isAuthenticated(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (AuthCache.isAuthenticated(player.getName())) {
            return;
        }

        String command = event.getMessage().split(" ")[0].toLowerCase();
        if (!MainConfig.IMP.auth.allowedCommands.contains(command)) {
            event.setCancelled(true);
        }
    }
}
