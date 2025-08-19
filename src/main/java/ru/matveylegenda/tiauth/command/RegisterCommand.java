package ru.matveylegenda.tiauth.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.ChatUtils;

public class RegisterCommand extends Command {
    private final MessagesConfig messagesConfig;
    private final ChatUtils chatUtils;
    private final AuthManager authManager;

    public RegisterCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
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

        if (args.length != 2) {
            chatUtils.sendMessage(
                    player,
                    messagesConfig.register.usage
            );

            return;
        }

        String password = args[0];
        String repeatPassword = args[1];

        authManager.registerPlayer(player, password, repeatPassword);
    }
}
