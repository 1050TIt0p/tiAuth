package ru.matveylegenda.tiauth.bungee.listener;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.protocol.ProtocolConstants;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.manager.TaskManager;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.premium.PremiumVerifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class AuthListener implements Listener {
    private static Field UNIQUE_ID_FIELD;
    private static Field REWRITE_ID_FIELD;
    // Bungee keeps the Login Start UUID inside its private InitialHandler state.
    private static Field LOGIN_REQUEST_FIELD;

    static {
        try {
            Class<?> INITIAL_HANDLER_CLASS = Class.forName("net.md_5.bungee.connection.InitialHandler");

            UNIQUE_ID_FIELD = INITIAL_HANDLER_CLASS.getDeclaredField("uniqueId");
            UNIQUE_ID_FIELD.setAccessible(true);

            REWRITE_ID_FIELD = INITIAL_HANDLER_CLASS.getDeclaredField("rewriteId");
            REWRITE_ID_FIELD.setAccessible(true);

            LOGIN_REQUEST_FIELD = INITIAL_HANDLER_CLASS.getDeclaredField("loginRequest");
            LOGIN_REQUEST_FIELD.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private final TiAuth plugin;
    private final Database database;
    private final AuthManager authManager;
    private final TaskManager taskManager;
    private final PremiumVerifier premiumVerifier;
    private final Pattern nickPattern;

    public AuthListener(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.authManager = plugin.getAuthManager();
        this.taskManager = plugin.getTaskManager();
        this.premiumVerifier = new PremiumVerifier(MainConfig.IMP.auth.premiumApiUrl);
        this.nickPattern = Pattern.compile(MainConfig.IMP.nickPattern);
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        PendingConnection connection = event.getConnection();

        if (!nickPattern.matcher(connection.getName()).matches()) {
            event.setReason(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.invalidNickPattern));
            event.setCancelled(true);
            return;
        }

        String ip = ((InetSocketAddress) connection.getSocketAddress()).getAddress().getHostAddress();
        if (BanCache.isBanned(ip)) {
            event.setReason(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.ban
                    .replace("{time}", String.valueOf(BanCache.getRemainingSeconds(ip)))));
            event.setCancelled(true);
            return;
        }

        if (BanCache.isTotpBanned(ip)) {
            event.setReason(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.totpBan
                    .replace("{time}", String.valueOf(BanCache.getTotpRemainingSeconds(ip)))));
            event.setCancelled(true);
            return;
        }

        if (!isAutomaticPremiumEnabled(connection) && PremiumCache.isPremium(connection.getName())) {
            connection.setOnlineMode(true);
            return;
        }

        int count = getPlayersCountByIp(ip);

        if (!MainConfig.IMP.excludedIps.contains(ip)) {
            if (count >= MainConfig.IMP.maxOnlineAccountsPerIp) {
                event.setReason(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.ipLimitOnlineReached));
                event.setCancelled(true);
                return;
            }
        }

        event.registerIntent(plugin);
        detectPremium(connection)
                .thenCompose(automaticPremium -> automaticPremium == null
                        ? configureLegacyConnection(connection, ip, event)
                        : configureAutomaticConnection(connection, automaticPremium, ip, event))
                .exceptionally(throwable -> {
                    event.setReason(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                    event.setCancelled(true);
                    return null;
                })
                .whenComplete((result, throwable) -> event.completeIntent(plugin));
    }

    private CompletableFuture<Void> configureAutomaticConnection(
            PendingConnection connection,
            boolean premium,
            String ip,
            PreLoginEvent event
    ) {
        UUID loginUuid = getLoginUuid(connection);
        return database.getPremiumIdentityRepository().getUuid(connection.getName())
                .thenCompose(boundUuid -> {
                    if (boundUuid != null && (!premium || !boundUuid.equals(loginUuid))) {
                        event.setReason(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.premiumTaken));
                        event.setCancelled(true);
                        return CompletableFuture.completedFuture(null);
                    }

                    connection.setOnlineMode(premium);
                    if (premium || MainConfig.IMP.excludedIps.contains(ip)) {
                        if (premium) {
                            return database.getAuthUserRepository().getUser(connection.getName())
                                    .thenAccept(user -> {
                                        if (user != null && !user.isPremium()) {
                                            if (ProxyServer.getInstance().getPlayer(connection.getName()) != null) {
                                                return;
                                            }
                                            event.setReason(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.nicknameTaken));
                                            event.setCancelled(true);
                                        }
                                    });
                        }
                        return CompletableFuture.completedFuture(null);
                    }

                    return database.getAuthUserRepository().getUserCountByIp(ip)
                            .thenAccept(ipCount -> {
                                if (ipCount >= MainConfig.IMP.maxRegisteredAccountsPerIp) {
                                    event.setReason(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.ipLimitRegisteredReached));
                                    event.setCancelled(true);
                                }
                            });
                });
    }

    private CompletableFuture<Void> configureLegacyConnection(
            PendingConnection connection,
            String ip,
            PreLoginEvent event
    ) {
        return database.getAuthUserRepository().getUser(connection.getName())
                .thenCompose(user -> {
                    if (user == null) {
                        connection.setOnlineMode(false);

                        if (!MainConfig.IMP.excludedIps.contains(ip)) {
                            return database.getAuthUserRepository().getUserCountByIp(ip)
                                    .thenAccept(ipCount -> {
                                        if (ipCount >= MainConfig.IMP.maxRegisteredAccountsPerIp) {
                                            event.setReason(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.ipLimitRegisteredReached));
                                            event.setCancelled(true);
                                        }
                                    });
                        }
                    } else if (user.isPremium()) {
                        connection.setOnlineMode(true);
                        PremiumCache.addPremium(connection.getName());
                    } else {
                        connection.setOnlineMode(false);
                    }

                    return CompletableFuture.completedFuture(null);
                });
    }

    private CompletableFuture<Boolean> detectPremium(PendingConnection connection) {
        if (!isAutomaticPremiumEnabled(connection)) {
            return CompletableFuture.completedFuture(null);
        }

        UUID loginUuid = getLoginUuid(connection);
        return premiumVerifier.isPremium(connection.getName(), loginUuid);
    }

    private boolean isAutomaticPremiumEnabled(PendingConnection connection) {
        return MainConfig.IMP.auth.skipPremiumPlayers &&
                connection.getVersion() >= ProtocolConstants.MINECRAFT_1_20_2;
    }

    private UUID getLoginUuid(PendingConnection connection) {
        if (LOGIN_REQUEST_FIELD == null) {
            return null;
        }

        try {
            Object loginRequest = LOGIN_REQUEST_FIELD.get(connection);
            if (loginRequest == null) {
                return null;
            }

            Method method = loginRequest.getClass().getMethod("getUuid");
            Object uuid = method.invoke(loginRequest);
            return uuid instanceof UUID ? (UUID) uuid : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            TiAuth.logger.log(Level.FINE, "Failed to read the 1.20.2+ login UUID", exception);
            return null;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(LoginEvent event) {
        PendingConnection connection = event.getConnection();

        if (!connection.isOnlineMode()) {
            return;
        }

        try {
            UUID offlineId = UUID.nameUUIDFromBytes(
                    ("OfflinePlayer:" + connection.getName()).getBytes(StandardCharsets.UTF_8)
            );
            UNIQUE_ID_FIELD.set(connection, offlineId);
            REWRITE_ID_FIELD.set(connection, offlineId);
        } catch (IllegalAccessException e) {
            TiAuth.logger.log(Level.WARNING, "Failed to set offline UUID for player " + connection.getName(), e);
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        event.registerIntent(plugin);

        AuthCache.registerConnection(player.getName(), player);

        bindPremiumIdentity(player).whenComplete((result, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().log(Level.WARNING, "Failed to bind premium identity for " + player.getName(), throwable);
                player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                event.completeIntent(plugin);
                return;
            }

            authManager.forceAuth(player, event);
        });
    }

    private CompletableFuture<Void> bindPremiumIdentity(ProxiedPlayer player) {
        PendingConnection connection = player.getPendingConnection();
        if (!MainConfig.IMP.auth.skipPremiumPlayers ||
                !connection.isOnlineMode() ||
                connection.getVersion() < ProtocolConstants.MINECRAFT_1_20_2) {
            return CompletableFuture.completedFuture(null);
        }

        return premiumVerifier.findUuid(player.getName())
                .thenCompose(profileUuid -> profileUuid
                        .map(uuid -> database.getPremiumIdentityRepository().bind(player.getName(), uuid))
                        .orElseGet(() -> CompletableFuture.failedFuture(
                                new IllegalStateException("Premium profile disappeared after online-mode login")
                        )));
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            return;
        }

        if (!AuthCache.isAuthenticated(player.getName()) &&
                !event.getTarget().getName().equals(MainConfig.IMP.servers.auth)) {
            event.setCancelled(true);
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

        if (AuthCache.unregisterConnection(player.getName(), player)) {
            plugin.getTotpManager().clearTotpState(player.getName());
        }

        taskManager.cancelTasks(player);
    }

    public int getPlayersCountByIp(String ip) {
        int count = 0;

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            String playerIp = BungeeUtils.getIp(player);
            if (playerIp.equals(ip)) {
                count++;
            }
        }

        return count;
    }
}
