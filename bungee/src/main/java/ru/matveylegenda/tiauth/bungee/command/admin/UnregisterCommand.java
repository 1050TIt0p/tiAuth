package ru.matveylegenda.tiauth.bungee.command.admin;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.cache.SessionCache;

public class UnregisterCommand implements AdminSubcommand {
    private final TiAuth plugin;
    private final AuthManager authManager;

    public UnregisterCommand(TiAuth plugin) {
        this.plugin = plugin;
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public String permission() {
        return "tiauth.admin.commands.unregister";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.unregister.usage);
            return;
        }

        String playerName = args[0];
        authManager.unregisterUser(playerName)
                .thenAccept(success -> {
                    if (!success) {
                        BungeeUtils.sendMessage(sender, CachedMessages.IMP.queryError);
                        return;
                    }

                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);
                    if (player != null) {
                        SessionCache.removePlayer(playerName);
                        player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.player.unregister.success));
                    }

                    BungeeUtils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.unregister.success.replace("{player}", playerName)
                    );
                });
    }
}
