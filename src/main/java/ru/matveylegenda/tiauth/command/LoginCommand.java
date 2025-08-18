package ru.matveylegenda.tiauth.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;
import ru.matveylegenda.tiauth.util.ChatUtils;

public class LoginCommand extends Command {
    private final TiAuth plugin;
    private final Database database;
    private final AuthCache authCache;
    private final SessionCache sessionCache;
    private final MainConfig mainConfig;
    private final MessagesConfig messagesConfig;
    private final ChatUtils chatUtils;

    public LoginCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.plugin = plugin;
        this.database = plugin.database;
        this.authCache = plugin.authCache;
        this.sessionCache = plugin.sessionCache;
        this.mainConfig = plugin.mainConfig;
        this.messagesConfig = plugin.messagesConfig;
        this.chatUtils = plugin.chatUtils;
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

        if (args.length != 1) {
            chatUtils.sendMessage(
                    player,
                    messagesConfig.login.usage
            );

            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), user -> {
            if (user == null) {
                chatUtils.sendMessage(
                        player,
                        messagesConfig.login.notRegistered
                );

                return;
            }

            if (authCache.isAuthenticated(player.getName())) {
                chatUtils.sendMessage(
                        player,
                        messagesConfig.login.alreadyLogged
                );

                return;
            }

            Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
            String password = args[0];
            String hashedPassword = user.getPassword();

            if (hash.verifyPassword(password, hashedPassword)) {
                chatUtils.sendMessage(
                        player,
                        messagesConfig.login.success
                );

                authCache.setAuthenticated(player.getName());
                sessionCache.addPlayer(player.getName(), player.getAddress().getAddress().getHostAddress());

                ServerInfo backendServer = plugin.getProxy().getServerInfo(mainConfig.servers.backend);
                player.connect(backendServer);
            } else {
                chatUtils.sendMessage(
                        player,
                        messagesConfig.login.wrongPassword
                );
            }
        });
    }
}
