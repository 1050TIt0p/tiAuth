package ru.matveylegenda.tiauth.command.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.config.CachedMessages;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;

public class LogoutCommand extends Command {
    private final AuthManager authManager;

    public LogoutCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            Utils.sendMessage(
                    sender,
                    CachedMessages.IMP.onlyPlayer
            );

            return;
        }

        if (!AuthCache.isAuthenticated(player.getName())) {
            return;
        }

        if (PremiumCache.isPremium(player.getName())) {
            Utils.sendMessage(
                    sender,
                    CachedMessages.IMP.player.logout.logoutByPremium
            );

            return;
        }

        authManager.logoutPlayer(player);
        authManager.forceAuth(player);
    }
}
