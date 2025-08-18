package ru.matveylegenda.tiauth.listener;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.database.Database;

import java.util.concurrent.TimeUnit;

public class AuthListener implements Listener {
    private final TiAuth plugin;
    private final Database database;
    private final AuthCache authCache;
    private final PremiumCache premiumCache;

    public AuthListener(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.database;
        this.authCache = plugin.authCache;
        this.premiumCache = plugin.premiumCache;
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
                if (user != null && !player.getName().equals(user.getRealName())) {
                    player.disconnect("Правильный ник: " + user.getRealName());

                    return;
                }

                if (premiumCache.isPremium(player.getName())) {
                    ServerInfo backendServer = plugin.getProxy().getServerInfo("hub");
                    authCache.setAuthenticated(player.getName());
                    player.connect(backendServer);

                    return;
                }

                ServerInfo authServer = plugin.getProxy().getServerInfo("auth");
                player.connect(authServer);

                String message = (user != null)
                        ? "Авторизируйтесь командой /login <пароль>"
                        : "Зарегистрируйтесь командой /register <пароль> <пароль>";

                ScheduledTask[] taskHolder = new ScheduledTask[1];
                taskHolder[0] = plugin.getProxy().getScheduler().schedule(plugin, () -> {
                    if (!player.isConnected() || authCache.isAuthenticated(player.getName())) {
                        taskHolder[0].cancel();
                        return;
                    }

                    player.sendMessage(message);
                }, 0, 1, TimeUnit.SECONDS);
            });

            return;
        }

        if (!authCache.isAuthenticated(player.getName()) && !event.getTarget().getName().equals("auth")) {
            player.disconnect("Вы не авторизовались");
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
