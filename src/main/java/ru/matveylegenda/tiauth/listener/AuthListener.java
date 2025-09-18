package ru.matveylegenda.tiauth.listener;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.manager.TaskManager;
import ru.matveylegenda.tiauth.util.Utils;

import java.util.regex.Pattern;

public class AuthListener implements Listener {
    private final TiAuth plugin;
    private final Database database;
    private final AuthCache authCache;
    private final PremiumCache premiumCache;
    private final BanCache banCache;
    private final MainConfig mainConfig;
    private final MessagesConfig messagesConfig;
    private final AuthManager authManager;
    private final TaskManager taskManager;
    private final Utils utils;
    private final Pattern nickPattern;

    public AuthListener(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.authCache = plugin.getAuthCache();
        this.premiumCache = plugin.getPremiumCache();
        this.banCache = plugin.getBanCache();
        this.mainConfig = plugin.getMainConfig();
        this.messagesConfig = plugin.getMessagesConfig();
        this.authManager = plugin.getAuthManager();
        this.taskManager = plugin.getTaskManager();
        this.utils = plugin.getUtils();
        this.nickPattern = Pattern.compile(mainConfig.nickPattern);
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        PendingConnection connection = event.getConnection();

        if (!nickPattern.matcher(connection.getName()).matches()) {
            utils.kickPlayer(
                    event,
                    messagesConfig.player.kick.invalidNickPattern
            );
            return;
        }

        if (banCache.isBanned(connection.getName())) {
            utils.kickPlayer(
                    event,
                    messagesConfig.player.kick.ban
                            .replace("{time}", String.valueOf(banCache.getRemainingSeconds(connection.getName())))
            );
            return;
        }

        event.registerIntent(plugin);

        database.getAuthUserRepository().getUser(connection.getName(), (user, success) -> {
            if (!success) {
                utils.kickPlayer(
                        event,
                        messagesConfig.queryError
                );

                event.completeIntent(plugin);
                return;
            }

            if (user != null && user.isPremium()) {
                connection.setOnlineMode(true);
                premiumCache.addPremium(connection.getName());
            }

            event.completeIntent(plugin);
        });
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        event.registerIntent(plugin);

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.kickPlayer(
                        player,
                        messagesConfig.queryError
                );
                event.completeIntent(plugin);

                return;
            }
            authManager.forceAuth(player, event);
        });
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            return;
        }

        if (!authCache.isAuthenticated(player.getName()) &&
                !event.getTarget().getName().equals(mainConfig.servers.auth)) {
            utils.kickPlayer(
                    player,
                    messagesConfig.player.kick.notAuth
            );
        }
    }

    @EventHandler
    public void onServerConnectedEvent(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (event.getServer().getInfo().getName().equals(mainConfig.servers.auth) &&
                !authCache.isAuthenticated(player.getName())) {
            taskManager.startDisplayTimerTask(player);
            authManager.showLoginDialog(player);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (authCache.isAuthenticated(player.getName())) {
            authCache.logout(player.getName());
        }

        taskManager.cancelTasks(player);
    }
}
