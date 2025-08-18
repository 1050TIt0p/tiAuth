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
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;
import ru.matveylegenda.tiauth.util.ChatUtils;

import java.util.Locale;

public class RegisterCommand extends Command {
    private final TiAuth plugin;
    private final Database database;
    private final AuthCache authCache;
    private final SessionCache sessionCache;
    private final MainConfig mainConfig;
    private final MessagesConfig messagesConfig;
    private final ChatUtils chatUtils;

    public RegisterCommand(TiAuth plugin, String name, String... aliases) {
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

        if (args.length != 2) {
            chatUtils.sendMessage(
                    player,
                    messagesConfig.register.usage
            );

            return;
        }

        String password = args[0];
        String repeatPassword = args[1];

        if (!password.equals(repeatPassword)) {
            chatUtils.sendMessage(
                    player,
                    messagesConfig.register.mismatch
            );

            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), user -> {
            if (user != null) {
                chatUtils.sendMessage(
                        player,
                        messagesConfig.register.alreadyRegistered
                );
                return;
            }

            Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
            database.getAuthUserRepository().registerUser(
                    new AuthUser(
                            player.getName().toLowerCase(Locale.ROOT),
                            player.getName(),
                            hash.hashPassword(password),
                            false,
                            player.getAddress().getAddress().getHostAddress()
                    ), () -> {
                        chatUtils.sendMessage(
                                player,
                                messagesConfig.register.success
                        );
                        authCache.setAuthenticated(player.getName());

                        sessionCache.addPlayer(player.getName(), player.getAddress().getAddress().getHostAddress());
                        ServerInfo backendServer = plugin.getProxy().getServerInfo(mainConfig.servers.backend);

                        player.connect(backendServer);
                    }
            );
        });
    }
}
