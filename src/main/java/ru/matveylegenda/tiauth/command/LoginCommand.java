package ru.matveylegenda.tiauth.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.ChatUtils;

public class LoginCommand extends Command {
    private final MessagesConfig messagesConfig;
    private final ChatUtils chatUtils;
    private final AuthManager authManager;

    public LoginCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.messagesConfig = plugin.getMessagesConfig();
        this.chatUtils = plugin.getChatUtils();
        this.authManager = plugin.getAuthManager();
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

        String password = args[0];
        authManager.loginPlayer(player, password);
    }
}
