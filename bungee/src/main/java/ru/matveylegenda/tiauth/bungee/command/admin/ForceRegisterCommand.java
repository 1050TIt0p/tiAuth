package ru.matveylegenda.tiauth.bungee.command.admin;

import net.md_5.bungee.api.CommandSender;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.database.Database;

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
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.forceRegister.usage);
            return;
        }

        String playerName = args[0];
        String password = args[1];

        database.getAuthUserRepository().getUser(playerName.toLowerCase(Locale.ROOT))
                .thenCompose(user -> {
                    if (user != null) {
                        BungeeUtils.sendMessage(
                                sender,
                                CachedMessages.IMP.admin.forceRegister.alreadyRegistered
                                        .replace("{player}", playerName)
                        );
                        return CompletableFuture.completedFuture(null);
                    }

                    return authManager.registerUser(playerName, password, null)
                            .thenAccept(success -> {
                                if (!success) {
                                    BungeeUtils.sendMessage(sender, CachedMessages.IMP.queryError);
                                    return;
                                }

                                BungeeUtils.sendMessage(
                                        sender,
                                        CachedMessages.IMP.admin.forceRegister.success
                                                .replace("{player}", playerName)
                                );
                            });
                });
    }
}
