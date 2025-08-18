package ru.matveylegenda.tiauth.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;

import java.util.Locale;

public class RegisterCommand extends Command {
    private final TiAuth plugin;
    private final Database database;
    private final AuthCache authCache;
    private final SessionCache sessionCache;

    public RegisterCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.plugin = plugin;
        this.database = plugin.database;
        this.authCache = plugin.authCache;
        this.sessionCache = plugin.sessionCache;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
           sender.sendMessage("Команду может использовать только игрок");

           return;
        }

        if (args.length != 2) {
            player.sendMessage("Использование: /register <пароль> <пароль>");

            return;
        }

        String password = args[0];
        String repeatPassword = args[1];

        if (!password.equals(repeatPassword)) {
            player.sendMessage("Пароли не совпадают");

            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), user -> {
            if (user != null) {
                player.sendMessage("Вы уже зарегистрированы");
                return;
            }

            Hash hash = HashFactory.create("bcrypt");
            database.getAuthUserRepository().registerUser(
                    new AuthUser(
                            player.getName().toLowerCase(Locale.ROOT),
                            player.getName(),
                            hash.hashPassword(password),
                            false,
                            player.getAddress().getAddress().getHostAddress()
                    ), () -> {
                        player.sendMessage("Вы успешно зарегистрировались");
                        authCache.setAuthenticated(player.getName());

                        sessionCache.addPlayer(player.getName(), player.getAddress().getAddress().getHostAddress());
                        ServerInfo backendServer = plugin.getProxy().getServerInfo("hub");

                        player.connect(backendServer);
                    }
            );
        });
    }
}
