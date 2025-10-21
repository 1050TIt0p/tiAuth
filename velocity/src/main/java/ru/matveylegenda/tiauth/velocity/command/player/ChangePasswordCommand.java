package ru.matveylegenda.tiauth.velocity.command.player;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

public class ChangePasswordCommand implements SimpleCommand {
    private final AuthManager authManager;

    public ChangePasswordCommand(TiAuth plugin) {
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (!(sender instanceof Player player)) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.onlyPlayer);
            return;
        }

        if (args.length != 2) {
            VelocityUtils.sendMessage(player, CachedComponents.IMP.player.changePassword.usage);
            return;
        }

        String oldPassword = args[0];
        String newPassword = args[1];
        authManager.changePasswordPlayer(player, oldPassword, newPassword);
    }
}