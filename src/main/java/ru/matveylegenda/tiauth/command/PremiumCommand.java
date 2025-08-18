package ru.matveylegenda.tiauth.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.util.ChatUtils;

public class PremiumCommand extends Command {
    private final Database database;
    private final PremiumCache premiumCache;
    private final MessagesConfig messagesConfig;
    private final ChatUtils chatUtils;

    public PremiumCommand(TiAuth plugin, String name) {
        super(name);
        this.database = plugin.database;
        this.premiumCache = plugin.premiumCache;
        this.messagesConfig = plugin.messagesConfig;
        this.chatUtils = plugin.chatUtils;
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

        if (premiumCache.isPremium(player.getName())) {
            database.getAuthUserRepository().setPremium(player.getName(), false);
            premiumCache.removePremium(player.getName());

            chatUtils.sendMessage(
                    player,
                    messagesConfig.premium.disabled
            );
        } else {
            database.getAuthUserRepository().setPremium(player.getName(), true);
            premiumCache.addPremium(player.getName());

            chatUtils.sendMessage(
                    player,
                    messagesConfig.premium.enabled
            );
        }
    }
}
