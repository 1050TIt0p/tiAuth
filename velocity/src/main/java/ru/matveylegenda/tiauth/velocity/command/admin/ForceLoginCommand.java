package ru.matveylegenda.tiauth.velocity.command.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

public class ForceLoginCommand implements AdminSubcommand {
    private final ProxyServer proxy;
    private final AuthManager authManager;

    public ForceLoginCommand(TiAuth plugin) {
        this.proxy = plugin.getServer();
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public String permission() {
        return "tiauth.admin.commands.forcelogin";
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (args.length < 1) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.forceLogin.usage);
            return;
        }

        Player player = proxy.getPlayer(args[0]).orElse(null);
        if (player == null) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.playerNotFound);
            return;
        }

        if (AuthCache.isAuthenticated(player.getUsername())) {
            VelocityUtils.sendMessage(
                    sender,
                    CachedComponents.IMP.admin.forceLogin.isAuthenticated
                            .replaceText(builder -> builder
                                    .match(VelocityUtils.PLAYER)
                                    .replacement(player.getUsername()))
            );
            return;
        }

        authManager.loginPlayer(player, true)
                .thenRun(() -> VelocityUtils.sendMessage(
                        sender,
                        CachedComponents.IMP.admin.forceLogin.success
                                .replaceText(builder -> builder
                                        .match(VelocityUtils.PLAYER)
                                        .replacement(player.getUsername()))
                ));
    }
}
