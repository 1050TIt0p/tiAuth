package ru.matveylegenda.tiauth.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.config.MainConfig;

import java.util.Locale;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer player)) {
            return;
        }

        if (AuthCache.isAuthenticated(player.getName())) {
            return;
        }

        if (event.isCommand() || event.isProxyCommand()) {
            String command = cutCommand(event.getMessage()).toLowerCase(Locale.ROOT);

            if (!MainConfig.IMP.auth.allowedCommands.contains(command)) {
                event.setCancelled(true);
            }

            return;
        }

        event.setCancelled(true);
    }

    private String cutCommand(String str) {
        int index = str.indexOf(' ');
        return index == -1 ? str : str.substring(0, index);
    }
}
