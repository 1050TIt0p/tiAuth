package ru.matveylegenda.tiauth.command.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;

public class ChangePasswordCommand extends Command {
    private final MessagesConfig messagesConfig;
    private final Utils utils;
    private final AuthManager authManager;

    public ChangePasswordCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.messagesConfig = plugin.getMessagesConfig();
        this.utils = plugin.getUtils();
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            utils.sendMessage(
                    sender,
                    messagesConfig.onlyPlayer
            );

            return;
        }

        if (args.length != 2) {
            utils.sendMessage(
                    player,
                    messagesConfig.player.changePassword.usage
            );

            return;
        }

        String oldPassword = args[0].trim();
        String newPassword = args[1].trim();
        authManager.changePasswordPlayer(player, oldPassword, newPassword);
    }
}
