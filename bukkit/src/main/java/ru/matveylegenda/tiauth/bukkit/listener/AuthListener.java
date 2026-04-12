package ru.matveylegenda.tiauth.bukkit.listener;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.matveylegenda.tiauth.bukkit.TiAuth;
import ru.matveylegenda.tiauth.bukkit.manager.AuthManager;
import ru.matveylegenda.tiauth.bukkit.manager.TaskManager;
import ru.matveylegenda.tiauth.bukkit.storage.CachedMessages;
import ru.matveylegenda.tiauth.bukkit.util.BukkitUtils;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import java.util.regex.Pattern;

public class AuthListener implements Listener {
    private final AuthManager authManager;
    private final TaskManager taskManager;
    private final Pattern nickPattern;
    private final Server server;

    public AuthListener(TiAuth plugin) {
        this.authManager = plugin.getAuthManager();
        this.taskManager = plugin.getTaskManager();
        this.nickPattern = Pattern.compile(MainConfig.IMP.nickPattern);
        this.server = plugin.getServer();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String name = event.getName();
        String ip = event.getAddress().getHostAddress();

        if (!nickPattern.matcher(name).matches()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, BukkitUtils.colorize(CachedMessages.IMP.player.kick.invalidNickPattern));
            return;
        }

        if (BanCache.isBanned(ip)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, BukkitUtils.colorize(CachedMessages.IMP.player.kick.ban
                    .replace("{time}", String.valueOf(BanCache.getRemainingSeconds(ip)))));
            return;
        }

        int count = getPlayersCountByIp(ip);
        if (!MainConfig.IMP.excludedIps.contains(ip)) {
            if (count >= MainConfig.IMP.maxOnlineAccountsPerIp) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, BukkitUtils.colorize(CachedMessages.IMP.player.kick.ipLimitOnlineReached));
            }
        }
    }

    public int getPlayersCountByIp(String ip) {
        int count = 0;

        for (Player player : server.getOnlinePlayers()) {
            String playerIp = player.getAddress().getAddress().getHostAddress();
            if (playerIp.equals(ip)) {
                count++;
            }
        }

        return count;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        authManager.forceAuth(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (AuthCache.isAuthenticated(player.getName())) {
            AuthCache.logout(player.getName());
        }

        taskManager.cancelTasks(player);
    }
}
