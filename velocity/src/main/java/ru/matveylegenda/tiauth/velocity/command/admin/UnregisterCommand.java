package ru.matveylegenda.tiauth.velocity.command.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

public class UnregisterCommand implements AdminSubcommand {
    private final ProxyServer proxy;
    private final AuthManager authManager;

    public UnregisterCommand(TiAuth plugin) {
        this.proxy = plugin.getServer();
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public String permission() {
        return "tiauth.admin.commands.unregister";
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (args.length < 1) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.unregister.usage);
            return;
        }

        String playerName = args[0];
        authManager.unregisterUser(playerName)
                .thenAccept(success -> {
                    if (!success) {
                        VelocityUtils.sendMessage(sender, CachedComponents.IMP.queryError);
                        return;
                    }

                    proxy.getPlayer(playerName).ifPresent(player -> {
                        SessionCache.removePlayer(playerName);
                        player.disconnect(CachedComponents.IMP.player.unregister.success);
                    });

                    VelocityUtils.sendMessage(
                            sender,
                            CachedComponents.IMP.admin.unregister.success
                                    .replaceText(builder -> builder
                                            .match(VelocityUtils.PLAYER)
                                            .replacement(playerName))
                    );
                });
    }
}
