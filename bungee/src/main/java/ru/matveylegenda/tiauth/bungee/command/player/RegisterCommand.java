package ru.matveylegenda.tiauth.bungee.command.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.util.Utils;

public class RegisterCommand extends Command {
    private final AuthManager authManager;

    public RegisterCommand(TiAuth plugin, String name, String... aliases) {
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

        if (args.length != 2) {
            Utils.sendMessage(
                    player,
                    CachedMessages.IMP.player.register.usage
            );

            return;
        }

        String password = args[0];
        String repeatPassword = args[1];

        authManager.registerPlayer(player, password, repeatPassword);
    }
}
