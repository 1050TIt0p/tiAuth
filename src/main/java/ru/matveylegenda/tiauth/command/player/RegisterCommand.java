package ru.matveylegenda.tiauth.command.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;
import ru.matveylegenda.tiauth.util.colorizer.ColorizedMessages;

public class RegisterCommand extends Command {
    private final Utils utils;
    private final ColorizedMessages colorizedMessages;
    private final AuthManager authManager;

    public RegisterCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
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

        if (args.length != 2) {
            utils.sendMessage(
                    player,
                    colorizedMessages.player.register.usage
            );

            return;
        }

        String password = args[0];
        String repeatPassword = args[1];

        authManager.registerPlayer(player, password, repeatPassword);
    }
}
