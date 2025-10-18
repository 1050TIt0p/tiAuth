package ru.matveylegenda.tiauth.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.kyori.adventure.text.Component;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.manager.TaskManager;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;

import java.util.regex.Pattern;

public class AuthListener {

    private final Database database;
    private final AuthManager authManager;
    private final TaskManager taskManager;
    private final Pattern nickPattern;
    private final Object2IntOpenHashMap<String> ipCounts = new Object2IntOpenHashMap<>();

    public AuthListener(TiAuth plugin) {
        this.database = plugin.getDatabase();
        this.authManager = plugin.getAuthManager();
        this.taskManager = plugin.getTaskManager();
        this.nickPattern = Pattern.compile(MainConfig.IMP.nickPattern);
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        String username = event.getUsername();
        String ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

        if (!nickPattern.matcher(username).matches()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(CachedComponents.IMP.player.kick.invalidNickPattern));
            return;
        }

        if (BanCache.isBanned(ip)) {
            Component kickMessage = CachedComponents.IMP.player.kick.ban.replaceText(builder -> builder
                    .matchLiteral("{time}")
                    .replacement(String.valueOf(BanCache.getRemainingSeconds(ip))));
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(kickMessage));
            return;
        }

        if (PremiumCache.isPremium(username)) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            return;
        }

        int count = ipCounts.getOrDefault(ip, 0);
        if (count >= MainConfig.IMP.maxOnlineAccountsPerIp) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(CachedComponents.IMP.player.kick.ipLimitOnlineReached));
            return;
        }

        database.getAuthUserRepository().getUser(username, (user, success) -> {
            if (!success) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(CachedComponents.IMP.queryError));
                return;
            }

            if (user == null) {
                database.getAuthUserRepository().getUserCountByIp(ip, count1 -> {
                    if (count1 >= MainConfig.IMP.maxRegisteredAccountsPerIp) {
                        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(CachedComponents.IMP.player.kick.ipLimitRegisteredReached));
                    } else {
                        ipCounts.addTo(ip, 1);
                        event.setResult(PreLoginEvent.PreLoginComponentResult.allowed());
                    }
                });
            } else {
                if (user.isPremium()) {
                    PremiumCache.addPremium(username);
                }
                ipCounts.addTo(ip, 1);
                event.setResult(PreLoginEvent.PreLoginComponentResult.allowed());
            }
        });
    }

    @Subscribe
    public void onGameProfile(GameProfileRequestEvent event) {
        GameProfile gameProfile = event.getGameProfile();
        event.setGameProfile(gameProfile.withId(UuidUtils.generateOfflinePlayerUuid(event.getUsername())));
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        authManager.forceAuth(player);
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
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        if (ipCounts.addTo(ip, -1) == 0) {
            ipCounts.removeInt(ip);
        }

        if (AuthCache.isAuthenticated(username)) {
            AuthCache.logout(username);
        }

        taskManager.cancelTasks(player);
    }
}