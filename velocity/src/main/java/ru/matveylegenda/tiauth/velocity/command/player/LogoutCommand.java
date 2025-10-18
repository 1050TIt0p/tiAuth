package ru.matveylegenda.tiauth.velocity.command.player;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;

public class LogoutCommand implements SimpleCommand {
    private final AuthManager authManager;

    public LogoutCommand(TiAuth plugin) {
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();

        if (!(sender instanceof Player player)) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.onlyPlayer);
            return;
        }

        if (!AuthCache.isAuthenticated(player.getUsername())) {
            return;
        }

        if (PremiumCache.isPremium(player.getUsername())) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.player.logout.logoutByPremium);
            return;
        }

        authManager.logoutPlayer(player);
        authManager.forceAuth(player);
    }
}