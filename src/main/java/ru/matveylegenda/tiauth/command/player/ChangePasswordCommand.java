package ru.matveylegenda.tiauth.command.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.config.CachedMessages;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;

public class ChangePasswordCommand extends Command {
    private final AuthManager authManager;

    public ChangePasswordCommand(TiAuth plugin, String name, String... aliases) {
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
                    CachedMessages.IMP.player.changePassword.usage
            );

            return;
        }

        String oldPassword = args[0];
        String newPassword = args[1];
        authManager.changePasswordPlayer(player, oldPassword, newPassword);
    }
}
