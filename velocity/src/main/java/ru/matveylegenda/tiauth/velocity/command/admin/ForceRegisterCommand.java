package ru.matveylegenda.tiauth.velocity.command.admin;

import com.velocitypowered.api.command.CommandSource;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ForceRegisterCommand implements AdminSubcommand {
    private final Database database;
    private final AuthManager authManager;

    public ForceRegisterCommand(TiAuth plugin) {
        this.database = plugin.getDatabase();
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public String permission() {
        return "tiauth.admin.commands.forceregister";
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (args.length < 2) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.forceRegister.usage);
            return;
        }

        String playerName = args[0];
        String password = args[1];

        database.getAuthUserRepository().getUser(playerName.toLowerCase(Locale.ROOT))
                .thenCompose(user -> {
                    if (user != null) {
                        VelocityUtils.sendMessage(
                                sender,
                                CachedComponents.IMP.admin.forceRegister.alreadyRegistered
                                        .replaceText(builder -> builder
                                                .match(VelocityUtils.PLAYER)
                                                .replacement(playerName))
                        );
                        return CompletableFuture.completedFuture(null);
                    }

                    return authManager.registerUser(playerName, password, null)
                            .thenAccept(success -> {
                                if (!success) {
                                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.queryError);
                                    return;
                                }

                                VelocityUtils.sendMessage(
                                        sender,
                                        CachedComponents.IMP.admin.forceRegister.success
                                                .replaceText(builder -> builder
                                                        .match(VelocityUtils.PLAYER)
                                                        .replacement(playerName))
                                );
                            });
                });
    }
}
