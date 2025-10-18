package ru.matveylegenda.tiauth.bungee.command.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;

public class PremiumCommand extends Command {
    private final AuthManager authManager;

    public PremiumCommand(TiAuth plugin, String name) {
        super(name);
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            BungeeUtils.sendMessage(
                    sender,
                    CachedMessages.IMP.onlyPlayer
            );

            return;
        }

        authManager.togglePremium(player);
    }
}
