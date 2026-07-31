package ru.matveylegenda.tiauth.velocity.listener;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import net.kyori.adventure.text.Component;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.premium.PremiumVerifier;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.manager.TaskManager;
import ru.matveylegenda.tiauth.velocity.manager.TotpManager;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class AuthListener {

    private final TiAuth plugin;
    private final Database database;
    private final AuthManager authManager;
    private final TaskManager taskManager;
    private final TotpManager totpManager;
    private final PremiumVerifier premiumVerifier;
    private final Pattern nickPattern;
    private final ProxyServer proxyServer;

    public AuthListener(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.authManager = plugin.getAuthManager();
        this.taskManager = plugin.getTaskManager();
        this.totpManager = plugin.getTotpManager();
        this.premiumVerifier = new PremiumVerifier(MainConfig.IMP.auth.premiumApiUrl);
        this.nickPattern = Pattern.compile(MainConfig.IMP.nickPattern);
        this.proxyServer = plugin.getServer();
    }

    @Subscribe
    public EventTask onPreLogin(PreLoginEvent event) {
        String username = event.getUsername();
        String ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

        if (!nickPattern.matcher(username).matches()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(CachedComponents.IMP.player.kick.invalidNickPattern));
            return null;
        }

        if (BanCache.isBanned(ip)) {
            Component kickMessage = CachedComponents.IMP.player.kick.ban.replaceText(builder -> builder
                    .match(VelocityUtils.TIME)
                    .replacement(String.valueOf(BanCache.getRemainingSeconds(ip))));
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(kickMessage));
            return null;
        }

        if (BanCache.isTotpBanned(ip)) {
            Component kickMessage = CachedComponents.IMP.player.kick.totpBan.replaceText(builder -> builder
                    .match(VelocityUtils.TIME)
                    .replacement(String.valueOf(BanCache.getTotpRemainingSeconds(ip))));
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(kickMessage));
            return null;
        }

        if (!isAutomaticPremiumEnabled(event) && PremiumCache.isPremium(username)) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            return null;
        }

        int count = getPlayersCountByIp(ip);
        if (!MainConfig.IMP.excludedIps.contains(ip)) {
            if (count >= MainConfig.IMP.maxOnlineAccountsPerIp) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(CachedComponents.IMP.player.kick.ipLimitOnlineReached));
                return null;
            }
        }

        CompletableFuture<Void> future = detectPremium(event)
                .thenCompose(automaticPremium -> automaticPremium == null
                        ? configureLegacyConnection(username, ip, event)
                        : configureAutomaticConnection(
                                username,
                                event.getUniqueId(),
                                automaticPremium,
                                ip,
                                event
                        ))
                .exceptionally(throwable -> {
                    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(CachedComponents.IMP.queryError));
                    return null;
                });

        return EventTask.resumeWhenComplete(future);
    }

    private CompletableFuture<Void> configureAutomaticConnection(
            String username,
            UUID loginUuid,
            boolean premium,
            String ip,
            PreLoginEvent event
    ) {
        return database.getAuthUserRepository().getUser(username)
                .thenCompose(user -> {
                    if (user != null && user.isAutomaticPremium()) {
                        if (loginUuid == null || !loginUuid.toString().equalsIgnoreCase(user.getPremiumUuid())) {
                            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                                    CachedComponents.IMP.player.kick.premiumTaken
                            ));
                            return CompletableFuture.completedFuture(null);
                        }
                    }

                    if (premium) {
                        if (user != null && !user.isPremium()) {
                            if (proxyServer.getPlayer(username).isPresent()) {
                                return CompletableFuture.completedFuture(null);
                            }
                            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                                    CachedComponents.IMP.player.kick.nicknameTaken
                            ));
                            return CompletableFuture.completedFuture(null);
                        }

                        event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
                        return CompletableFuture.completedFuture(null);
                    }

                    event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());

                    if (MainConfig.IMP.excludedIps.contains(ip)) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return database.getAuthUserRepository().getUserCountByIp(ip)
                            .thenAccept(ipCount -> {
                                if (ipCount >= MainConfig.IMP.maxRegisteredAccountsPerIp) {
                                    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                                            CachedComponents.IMP.player.kick.ipLimitRegisteredReached
                                    ));
                                }
                            });
                });
    }

    private CompletableFuture<Void> configureLegacyConnection(
            String username,
            String ip,
            PreLoginEvent event
    ) {
        return database.getAuthUserRepository().getUser(username)
                .thenCompose(user -> {
                    if (user == null) {
                        if (!MainConfig.IMP.excludedIps.contains(ip)) {
                            return database.getAuthUserRepository().getUserCountByIp(ip)
                                    .thenAccept(ipCount -> {
                                        if (ipCount >= MainConfig.IMP.maxRegisteredAccountsPerIp) {
                                            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                                                    CachedComponents.IMP.player.kick.ipLimitRegisteredReached
                                            ));
                                        } else {
                                            event.setResult(PreLoginEvent.PreLoginComponentResult.allowed());
                                        }
                                    });
                        }

                        event.setResult(PreLoginEvent.PreLoginComponentResult.allowed());
                        return CompletableFuture.completedFuture(null);
                    }

                    if (user.isPremium()) {
                        event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
                        PremiumCache.addPremium(username);
                    } else {
                        event.setResult(PreLoginEvent.PreLoginComponentResult.allowed());
                    }

                    return CompletableFuture.completedFuture(null);
                });
    }

    private CompletableFuture<Boolean> detectPremium(PreLoginEvent event) {
        if (!isAutomaticPremiumEnabled(event)) {
            return CompletableFuture.completedFuture(null);
        }

        UUID loginUuid = event.getUniqueId();
        return premiumVerifier.isPremium(event.getUsername(), loginUuid);
    }

    private boolean isAutomaticPremiumEnabled(PreLoginEvent event) {
        return MainConfig.IMP.auth.skipPremiumPlayers &&
                event.getConnection().getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0;
    }

    @Subscribe
    public void onGameProfile(GameProfileRequestEvent event) {
        GameProfile gameProfile = event.getGameProfile();
        event.setGameProfile(gameProfile.withId(UuidUtils.generateOfflinePlayerUuid(event.getUsername())));
    }

    @Subscribe
    public EventTask onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        CompletableFuture<Void> future = new CompletableFuture<>();

        AuthCache.registerConnection(player.getUsername(), player);

        bindPremiumIdentity(player).whenComplete((result, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().warn("Failed to bind premium identity for {}", player.getUsername(), throwable);
                player.disconnect(CachedComponents.IMP.queryError);
                future.complete(null);
                return;
            }

            authManager.forceAuth(player, event, future);
        });
        authManager.forceAuth(player, event, future);
        return EventTask.resumeWhenComplete(future);
    }

    private CompletableFuture<Void> bindPremiumIdentity(Player player) {
        if (!MainConfig.IMP.auth.skipPremiumPlayers ||
                !player.isOnlineMode() ||
                player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) < 0) {
            return CompletableFuture.completedFuture(null);
        }

        return premiumVerifier.findUuid(player.getUsername())
                .thenCompose(profileUuid -> profileUuid
                        .map(uuid -> database.getAuthUserRepository().registerPremiumUser(
                                player.getUsername(),
                                uuid,
                                VelocityUtils.getIp(player)
                        ))
                        .orElseGet(() -> CompletableFuture.failedFuture(
                                new IllegalStateException("Premium profile disappeared after online-mode login")
                        )));
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        String targetServer = event.getOriginalServer().getServerInfo().getName();

        if (!AuthCache.isAuthenticated(player.getUsername()) &&
                !targetServer.equals(MainConfig.IMP.servers.auth)) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

    @Subscribe
    public void onServerConnected(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        String connectedServer = event.getPlayer().getCurrentServer().get().getServerInfo().getName();

        if (connectedServer.equals(MainConfig.IMP.servers.auth) &&
                !AuthCache.isAuthenticated(player.getUsername())) {
            taskManager.startDisplayTimerTask(player);
            authManager.showLoginDialog(player);
        } else {
            taskManager.cancelTasks(player);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        String username = player.getUsername();

        if (AuthCache.unregisterConnection(username, player)) {
            totpManager.clearTotpState(username);
        }

        taskManager.cancelTasks(player);
    }

    public int getPlayersCountByIp(String ip) {
        int count = 0;

        for (Player player : proxyServer.getAllPlayers()) {
            String playerIp = VelocityUtils.getIp(player);
            if (playerIp.equals(ip)) {
                count++;
            }
        }

        return count;
    }
}
