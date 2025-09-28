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
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.manager.TaskManager;
import ru.matveylegenda.tiauth.util.Utils;
import ru.matveylegenda.tiauth.util.colorizer.ColorizedMessages;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AuthListener implements Listener {
    private final Map<String, Integer> ipCounts = new HashMap<>();
    private final TiAuth plugin;
    private final Database database;
    private final AuthCache authCache;
    private final PremiumCache premiumCache;
    private final BanCache banCache;
    private final MainConfig mainConfig;
    private final AuthManager authManager;
    private final TaskManager taskManager;
    private final Utils utils;
    private final ColorizedMessages colorizedMessages;
    private final Pattern nickPattern;

    public AuthListener(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.authCache = plugin.getAuthCache();
        this.premiumCache = plugin.getPremiumCache();
        this.banCache = plugin.getBanCache();
        this.mainConfig = plugin.getMainConfig();
        this.authManager = plugin.getAuthManager();
        this.taskManager = plugin.getTaskManager();
        this.utils = plugin.getUtils();
        this.colorizedMessages = plugin.getColorizedMessages();
        this.nickPattern = Pattern.compile(mainConfig.nickPattern);
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        PendingConnection connection = event.getConnection();

        if (!nickPattern.matcher(connection.getName()).matches()) {
            utils.kickPlayer(
                    event,
                    colorizedMessages.player.kick.invalidNickPattern
            );
            return;
        }

        String ip = connection.getAddress().getAddress().getHostAddress();
        if (banCache.isBanned(ip)) {
            utils.kickPlayer(
                    event,
                    colorizedMessages.player.kick.ban
                            .replace("{time}", String.valueOf(banCache.getRemainingSeconds(ip)))
            );
            return;
        }

        if (premiumCache.isPremium(connection.getName())) {
            connection.setOnlineMode(true);
            premiumCache.addPremium(connection.getName());
            return;
        }

        int count = ipCounts.getOrDefault(ip, 0);

        if (count >= mainConfig.maxOnlineAccountsPerIp) {
            utils.kickPlayer(
                    event,
                    colorizedMessages.player.kick.ipLimitOnlineReached
            );
            return;
        }
        ipCounts.put(ip, count + 1);

        event.registerIntent(plugin);
        database.getAuthUserRepository().getUser(connection.getName(), (user, success) -> {
            if (!success) {
                utils.kickPlayer(
                        event,
                        colorizedMessages.queryError
                );

                event.completeIntent(plugin);
                return;
            }

            if (user == null) {
                database.getAuthUserRepository().getUserCountByIp(ip, count1 -> {
                    if (count1 >= mainConfig.maxRegisteredAccountsPerIp) {
                        utils.kickPlayer(
                                event,
                                colorizedMessages.player.kick.ipLimitRegisteredReached
                        );
                    }
                    event.completeIntent(plugin);
                });
                return;
            } else if (user.isPremium()) {
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

        authManager.forceAuth(player, event);
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
                    colorizedMessages.player.kick.notAuth
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
        } else {
            taskManager.cancelTasks(player);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        String ip = player.getAddress().getAddress().getHostAddress();
        ipCounts.put(ip, ipCounts.getOrDefault(ip, 1) - 1);

        if (authCache.isAuthenticated(player.getName())) {
            authCache.logout(player.getName());
        }

        taskManager.cancelTasks(player);
    }
}
