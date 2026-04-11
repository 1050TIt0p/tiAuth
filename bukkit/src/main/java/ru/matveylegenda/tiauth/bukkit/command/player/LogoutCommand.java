package ru.matveylegenda.tiauth.bukkit.command.player;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.matveylegenda.tiauth.bukkit.TiAuth;
import ru.matveylegenda.tiauth.bukkit.manager.AuthManager;
import ru.matveylegenda.tiauth.bukkit.storage.CachedMessages;
import ru.matveylegenda.tiauth.bukkit.util.BukkitUtils;
import ru.matveylegenda.tiauth.cache.AuthCache;

public class LogoutCommand implements CommandExecutor {
    private final AuthManager authManager;

    public LogoutCommand(TiAuth plugin) {
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            BukkitUtils.sendMessage(sender, CachedMessages.IMP.onlyPlayer);
            return true;
        }

        if (!AuthCache.isAuthenticated(player.getName())) {
            return true;
        }

        authManager.logoutPlayer(player);
        authManager.forceAuth(player);
        return true;
    }
}
