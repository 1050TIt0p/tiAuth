package ru.matveylegenda.tiauth.listener;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.manager.AuthManager;

import static ru.matveylegenda.tiauth.util.ChatUtils.colorize;

public class AuthListener implements Listener {
    private final Database database;
    private final AuthCache authCache;
    private final PremiumCache premiumCache;
    private final MessagesConfig messagesConfig;
    private final AuthManager authManager;

    public AuthListener(TiAuth plugin) {
        this.database = plugin.database;
        this.authCache = plugin.authCache;
        this.premiumCache = plugin.premiumCache;
        this.messagesConfig = plugin.messagesConfig;
        this.authManager = plugin.authManager;
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        PendingConnection connection = event.getConnection();

        if (premiumCache.isPremium(connection.getName())) {
            connection.setOnlineMode(true);
        }
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            event.setCancelled(true);

            database.getAuthUserRepository().getUser(player.getName(), user -> {
                authManager.forceAuth(player, user);
            });

            return;
        }

        if (!authCache.isAuthenticated(player.getName()) && !event.getTarget().getName().equals("auth")) {
            player.disconnect(
                    colorize(
                            messagesConfig.kick.notAuth
                                    .replace("{prefix}", messagesConfig.prefix)
                    )
            );
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (authCache.isAuthenticated(player.getName())) {
            authCache.logout(player.getName());
        }
    }
}
