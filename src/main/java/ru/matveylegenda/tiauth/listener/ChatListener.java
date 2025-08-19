package ru.matveylegenda.tiauth.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.config.MainConfig;

import java.util.Locale;

public class ChatListener implements Listener {
    private final AuthCache authCache;
    private final MainConfig mainConfig;

    public ChatListener(TiAuth plugin) {
        this.authCache = plugin.getAuthCache();
        this.mainConfig = plugin.getMainConfig();
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer player)) {
            return;
        }

        if (authCache.isAuthenticated(player.getName())) {
            return;
        }

        if (event.isCommand() || event.isProxyCommand()) {
            String command = event.getMessage().split(" ")[0].toLowerCase(Locale.ROOT);

            if (!mainConfig.auth.allowedCommands.contains(command)) {
                event.setCancelled(true);
            }

            return;
        }

        event.setCancelled(true);
    }
}
