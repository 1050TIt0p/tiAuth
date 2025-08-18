package ru.matveylegenda.tiauth.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;

public class LoginCommand extends Command {
    private final TiAuth plugin;
    private final Database database;
    private final AuthCache authCache;
    private final SessionCache sessionCache;

    public LoginCommand(TiAuth plugin, String name, String... aliases) {
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

        if (args.length != 1) {
            player.sendMessage("Использование: /login <пароль>");

            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), user -> {
            if (user == null) {
                player.sendMessage("Вы еще не зарегистрированы");

                return;
            }

            if (authCache.isAuthenticated(player.getName())) {
                player.sendMessage("Вы уже авторизованы");

                return;
            }

            Hash hash = HashFactory.create("bcrypt");
            String password = args[0];
            String hashedPassword = user.getPassword();

            if (hash.verifyPassword(password, hashedPassword)) {
                player.sendMessage("Вы вошли успешно");

                authCache.setAuthenticated(player.getName());
                sessionCache.addPlayer(player.getName(), player.getAddress().getAddress().getHostAddress());

                ServerInfo backendServer = plugin.getProxy().getServerInfo("hub");
                player.connect(backendServer);
            } else {
                player.sendMessage("Неверный пароль");
            }
        });
    }
}
