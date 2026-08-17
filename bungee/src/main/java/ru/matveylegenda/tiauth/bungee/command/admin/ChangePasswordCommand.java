package ru.matveylegenda.tiauth.bungee.command.admin;

import net.md_5.bungee.api.CommandSender;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;

public class ChangePasswordCommand implements AdminSubcommand {
    private final AuthManager authManager;

    public ChangePasswordCommand(TiAuth plugin) {
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public String permission() {
        return "tiauth.admin.commands.changepassword";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.changePassword.usage);
            return;
        }

        String playerName = args[0];
        String password = args[1];
        authManager.changePasswordUser(playerName, password)
                .thenAccept(success -> {
                    if (!success) {
                        BungeeUtils.sendMessage(sender, CachedMessages.IMP.queryError);
                        return;
                    }

                    BungeeUtils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.changePassword.success.replace("{player}", playerName)
                    );
                });
    }
}
