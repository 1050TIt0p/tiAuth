package ru.matveylegenda.tiauth.command.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.config.CachedMessages;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;

public class LoginCommand extends Command {
    private final AuthManager authManager;

    public LoginCommand(TiAuth plugin, String name, String... aliases) {
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

        if (args.length != 1) {
            Utils.sendMessage(
                    player,
                    CachedMessages.IMP.player.login.usage
            );

            return;
        }

        String password = args[0];
        authManager.loginPlayer(player, password);
    }
}
