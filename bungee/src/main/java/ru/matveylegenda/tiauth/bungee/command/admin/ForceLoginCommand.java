package ru.matveylegenda.tiauth.bungee.command.admin;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.cache.AuthCache;

public class ForceLoginCommand implements AdminSubcommand {
    private final TiAuth plugin;
    private final AuthManager authManager;

    public ForceLoginCommand(TiAuth plugin) {
        this.plugin = plugin;
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public String permission() {
        return "tiauth.admin.commands.forcelogin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.forceLogin.usage);
            return;
        }

        ProxiedPlayer player = plugin.getProxy().getPlayer(args[0]);
        if (player == null) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.playerNotFound);
            return;
        }

        if (AuthCache.isAuthenticated(player.getName())) {
            BungeeUtils.sendMessage(
                    sender,
                    CachedMessages.IMP.admin.forceLogin.isAuthenticated.replace("{player}", player.getName())
            );
            return;
        }

        authManager.loginPlayer(player, true)
                .thenRun(() -> BungeeUtils.sendMessage(
                        sender,
                        CachedMessages.IMP.admin.forceLogin.success.replace("{player}", player.getName())
                ));
    }
}
