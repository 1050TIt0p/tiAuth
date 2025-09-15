package ru.matveylegenda.tiauth.command.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;

public class PremiumCommand extends Command {
    private final MessagesConfig messagesConfig;
    private final Utils utils;
    private final AuthManager authManager;

    public PremiumCommand(TiAuth plugin, String name) {
        super(name);
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

        authManager.togglePremium(player);
    }
}
