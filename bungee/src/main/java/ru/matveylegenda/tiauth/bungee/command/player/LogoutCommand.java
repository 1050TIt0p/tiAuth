package ru.matveylegenda.tiauth.bungee.command.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;

public class LogoutCommand extends Command {
    private final AuthManager authManager;

    public LogoutCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            BungeeUtils.sendMessage(
                    sender,
                    CachedMessages.IMP.onlyPlayer
            );

            return;
        }

        if (!AuthCache.isAuthenticated(player.getName())) {
            return;
        }

        if (PremiumCache.isPremium(player.getName())) {
            BungeeUtils.sendMessage(
                    sender,
                    CachedMessages.IMP.player.logout.logoutByPremium
            );

            return;
        }

        authManager.logoutPlayer(player);
        authManager.forceAuth(player);
    }
}
