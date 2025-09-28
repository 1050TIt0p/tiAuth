package ru.matveylegenda.tiauth.command.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;
import ru.matveylegenda.tiauth.util.colorizer.ColorizedMessages;

public class LogoutCommand extends Command {
    private final AuthCache authCache;
    private final PremiumCache premiumCache;
    private final Utils utils;
    private final ColorizedMessages colorizedMessages;
    private final AuthManager authManager;

    public LogoutCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.authCache = plugin.getAuthCache();
        this.premiumCache = plugin.getPremiumCache();
        this.utils = plugin.getUtils();
        this.colorizedMessages = plugin.getColorizedMessages();
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            utils.sendMessage(
                    sender,
                    colorizedMessages.onlyPlayer
            );

            return;
        }

        if (!authCache.isAuthenticated(player.getName())) {
            return;
        }

        if (premiumCache.isPremium(player.getName())) {
            utils.sendMessage(
                    sender,
                    colorizedMessages.player.logout.logoutByPremium
            );

            return;
        }

        authManager.logoutPlayer(player);
        authManager.forceAuth(player);
    }
}
