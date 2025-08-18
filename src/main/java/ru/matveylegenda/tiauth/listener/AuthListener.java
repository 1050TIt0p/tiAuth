package ru.matveylegenda.tiauth.listener;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.util.ChatUtils;

import java.util.concurrent.TimeUnit;

import static ru.matveylegenda.tiauth.util.ChatUtils.colorize;

public class AuthListener implements Listener {
    private final TiAuth plugin;
    private final Database database;
    private final AuthCache authCache;
    private final PremiumCache premiumCache;
    private final SessionCache sessionCache;
    private final MainConfig mainConfig;
    private final MessagesConfig messagesConfig;
    private final ChatUtils chatUtils;

    public AuthListener(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.database;
        this.authCache = plugin.authCache;
        this.premiumCache = plugin.premiumCache;
        this.sessionCache = plugin.sessionCache;
        this.mainConfig = plugin.mainConfig;
        this.messagesConfig = plugin.messagesConfig;
        this.chatUtils = plugin.chatUtils;
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
                    player.disconnect(
                            colorize(
                                    messagesConfig.kick.realname
                                            .replace("{prefix}", messagesConfig.prefix)
                                            .replace("{realname}", user.getRealName())
                                            .replace("{name}", player.getName())
                            )
                    );

                    return;
                }

                String sessionIP = sessionCache.getIP(player.getName());

                if (premiumCache.isPremium(player.getName()) ||
                        (sessionIP != null && sessionIP.equals(player.getAddress().getAddress().getHostAddress()))) {
                    authCache.setAuthenticated(player.getName());
                    ServerInfo backendServer = plugin.getProxy().getServerInfo(mainConfig.servers.backend);
                    player.connect(backendServer);

                    return;
                }

                ServerInfo authServer = plugin.getProxy().getServerInfo(mainConfig.servers.auth);
                player.connect(authServer);

                ScheduledTask[] taskHolder = new ScheduledTask[2];
                taskHolder[0] = plugin.getProxy().getScheduler().schedule(plugin, () -> {
                    if (!player.isConnected() || authCache.isAuthenticated(player.getName())) {
                        taskHolder[0].cancel();
                        return;
                    }

                    player.disconnect(
                            colorize(
                                    messagesConfig.kick.timeout
                                            .replace("{prefix}", messagesConfig.prefix)
                            )
                    );
                }, mainConfig.auth.timeoutSeconds, TimeUnit.SECONDS);

                String reminderMessage = (user != null)
                        ? messagesConfig.reminder.login
                        : messagesConfig.reminder.register;
                taskHolder[1] = plugin.getProxy().getScheduler().schedule(plugin, () -> {
                    if (!player.isConnected() || authCache.isAuthenticated(player.getName())) {
                        taskHolder[1].cancel();
                        return;
                    }

                    chatUtils.sendMessage(
                            player,
                            reminderMessage
                    );
                }, 0, mainConfig.auth.reminderInterval, TimeUnit.SECONDS);
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
