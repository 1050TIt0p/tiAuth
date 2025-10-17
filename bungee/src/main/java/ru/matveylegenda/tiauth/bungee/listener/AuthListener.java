package ru.matveylegenda.tiauth.bungee.listener;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.manager.TaskManager;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class AuthListener implements Listener {
    private static Field UNIQUE_ID_FIELD;

    static {
        try {
            Class<?> INITIAL_HANDLER_CLASS = Class.forName("net.md_5.bungee.connection.InitialHandler");

            UNIQUE_ID_FIELD = INITIAL_HANDLER_CLASS.getDeclaredField("uniqueId");
            UNIQUE_ID_FIELD.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private final Object2IntOpenHashMap<String> ipCounts = new Object2IntOpenHashMap<>();
    private final TiAuth plugin;
    private final Database database;
    private final AuthManager authManager;
    private final TaskManager taskManager;
    private final Pattern nickPattern;

    public AuthListener(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.authManager = plugin.getAuthManager();
        this.taskManager = plugin.getTaskManager();
        this.nickPattern = Pattern.compile(MainConfig.IMP.nickPattern);
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        PendingConnection connection = event.getConnection();

        if (!nickPattern.matcher(connection.getName()).matches()) {
            event.setCancelReason(CachedMessages.IMP.player.kick.invalidNickPattern);
            event.setCancelled(true);
            return;
        }

        String ip = connection.getAddress().getAddress().getHostAddress();
        if (BanCache.isBanned(ip)) {
            event.setCancelReason(CachedMessages.IMP.player.kick.ban
                    .replace("{time}", String.valueOf(BanCache.getRemainingSeconds(ip))));
            event.setCancelled(true);
            return;
        }

        if (PremiumCache.isPremium(connection.getName())) {
            connection.setOnlineMode(true);
            return;
        }

        int count = ipCounts.getOrDefault(ip, 0);

        if (count >= MainConfig.IMP.maxOnlineAccountsPerIp) {
            event.setCancelReason(CachedMessages.IMP.player.kick.ipLimitOnlineReached);
            event.setCancelled(true);
            return;
        }
        ipCounts.addTo(ip, 1);

        event.registerIntent(plugin);
        database.getAuthUserRepository().getUser(connection.getName(), (user, success) -> {
            if (!success) {
                event.setCancelReason(CachedMessages.IMP.queryError);
                event.setCancelled(true);

                event.completeIntent(plugin);
                return;
            }

            if (user == null) {
                database.getAuthUserRepository().getUserCountByIp(ip, count1 -> {
                    if (count1 >= MainConfig.IMP.maxRegisteredAccountsPerIp) {
                        event.setCancelReason(CachedMessages.IMP.player.kick.ipLimitRegisteredReached);
                        event.setCancelled(true);
                    }
                    event.completeIntent(plugin);
                });
                return;
            } else if (user.isPremium()) {
                connection.setOnlineMode(true);
                PremiumCache.addPremium(connection.getName());
            }

            event.completeIntent(plugin);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(LoginEvent event) {
        PendingConnection connection = event.getConnection();

        try {
            UUID offlineId = UUID.nameUUIDFromBytes(
                    ("OfflinePlayer:" + connection.getName()).getBytes(StandardCharsets.UTF_8)
            );
            UNIQUE_ID_FIELD.set(connection, offlineId);
        } catch (IllegalAccessException e) {
            TiAuth.logger.log(Level.WARNING, "Failed to set offline UUID for player " + connection.getName(), e);
        }
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

        if (!AuthCache.isAuthenticated(player.getName()) &&
                !event.getTarget().getName().equals(MainConfig.IMP.servers.auth)) {
            player.disconnect(CachedMessages.IMP.player.kick.notAuth);
        }
    }

    @EventHandler
    public void onServerConnectedEvent(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (event.getServer().getInfo().getName().equals(MainConfig.IMP.servers.auth) &&
                !AuthCache.isAuthenticated(player.getName())) {
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
        if (ipCounts.addTo(ip, -1) == 0) {
            ipCounts.removeInt(player.getName());
        }

        if (AuthCache.isAuthenticated(player.getName())) {
            AuthCache.logout(player.getName());
        }

        taskManager.cancelTasks(player);
    }
}
