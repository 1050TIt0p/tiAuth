package ru.matveylegenda.tiauth.velocity.command.admin;

import com.velocitypowered.api.command.CommandSource;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

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
    public void execute(CommandSource sender, String[] args) {
        if (args.length < 1) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.forcePremium.usage);
            return;
        }

        String playerName = args[0];
        database.getAuthUserRepository().getUser(playerName)
                .thenCompose(user -> {
                    if (user == null) {
                        VelocityUtils.sendMessage(sender, CachedComponents.IMP.playerNotFound);
                        return CompletableFuture.completedFuture(null);
                    }

                    return database.getAuthUserRepository().setPremium(playerName, !user.isPremium())
                            .thenAccept(result -> {
                                if (user.isPremium()) {
                                    PremiumCache.removePremium(playerName);
                                    VelocityUtils.sendMessage(
                                            sender,
                                            CachedComponents.IMP.admin.forcePremium.disabled
                                                    .replaceText(builder -> builder
                                                            .match(VelocityUtils.PLAYER)
                                                            .replacement(playerName))
                                    );
                                } else {
                                    PremiumCache.addPremium(playerName);
                                    VelocityUtils.sendMessage(
                                            sender,
                                            CachedComponents.IMP.admin.forcePremium.enabled
                                                    .replaceText(builder -> builder
                                                            .match(VelocityUtils.PLAYER)
                                                            .replacement(playerName))
                                    );
                                }
                            });
                })
                .exceptionally(throwable -> {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.queryError);
                    return null;
                });
    }
}
