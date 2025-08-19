package ru.matveylegenda.tiauth.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.ChatUtils;

public class LogoutCommand extends Command {
    private final Database database;
    private final AuthCache authCache;
    private final PremiumCache premiumCache;
    private final MessagesConfig messagesConfig;
    private final ChatUtils chatUtils;
    private final AuthManager authManager;

    public LogoutCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.database = plugin.database;
        this.authCache = plugin.authCache;
        this.premiumCache = plugin.premiumCache;
        this.messagesConfig = plugin.messagesConfig;
        this.chatUtils = plugin.chatUtils;
        this.authManager = plugin.authManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            chatUtils.sendMessage(
                    sender,
                    messagesConfig.onlyPlayer
            );

            return;
        }

        if (!authCache.isAuthenticated(player.getName())) {
            return;
        }

        if (premiumCache.isPremium(player.getName())) {
            chatUtils.sendMessage(
                    sender,
                    messagesConfig.logout.logoutByPremium
            );

            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), user -> {
            authManager.logoutPlayer(player);
            authManager.forceAuth(player, user);
        });
    }
}
