package ru.matveylegenda.tiauth.bungee.command.admin;

import net.md_5.bungee.api.CommandSender;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.database.Database;

import java.util.concurrent.CompletableFuture;

public class ForcePremiumCommand implements AdminSubcommand {
    private final Database database;

    public ForcePremiumCommand(TiAuth plugin) {
        this.database = plugin.getDatabase();
    }

    @Override
    public String permission() {
        return "tiauth.admin.commands.forcepremium";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.forcePremium.usage);
            return;
        }

        String playerName = args[0];
        database.getAuthUserRepository().getUser(playerName)
                .thenCompose(user -> {
                    if (user == null) {
                        BungeeUtils.sendMessage(sender, CachedMessages.IMP.playerNotFound);
                        return CompletableFuture.completedFuture(null);
                    }

                    return database.getAuthUserRepository().setPremium(playerName, !user.isPremium())
                            .thenAccept(result -> {
                                if (user.isPremium()) {
                                    PremiumCache.removePremium(playerName);
                                    BungeeUtils.sendMessage(
                                            sender,
                                            CachedMessages.IMP.admin.forcePremium.disabled
                                                    .replace("{player}", playerName)
                                    );
                                } else {
                                    PremiumCache.addPremium(playerName);
                                    BungeeUtils.sendMessage(
                                            sender,
                                            CachedMessages.IMP.admin.forcePremium.enabled
                                                    .replace("{player}", playerName)
                                    );
                                }
                            });
                })
                .exceptionally(throwable -> {
                    BungeeUtils.sendMessage(sender, CachedMessages.IMP.queryError);
                    return null;
                });
    }
}
