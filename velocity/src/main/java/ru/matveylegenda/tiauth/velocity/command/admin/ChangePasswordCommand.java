package ru.matveylegenda.tiauth.velocity.command.admin;

import com.velocitypowered.api.command.CommandSource;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

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
    public void execute(CommandSource sender, String[] args) {
        if (args.length < 2) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.changePassword.usage);
            return;
        }

        String playerName = args[0];
        String password = args[1];
        authManager.changePasswordUser(playerName, password)
                .thenAccept(success -> {
                    if (!success) {
                        VelocityUtils.sendMessage(sender, CachedComponents.IMP.queryError);
                        return;
                    }

                    VelocityUtils.sendMessage(
                            sender,
                            CachedComponents.IMP.admin.changePassword.success
                                    .replaceText(builder -> builder
                                            .match(VelocityUtils.PLAYER)
                                            .replacement(playerName))
                    );
                });
    }
}
