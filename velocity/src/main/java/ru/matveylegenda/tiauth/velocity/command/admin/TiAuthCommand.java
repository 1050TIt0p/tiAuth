package ru.matveylegenda.tiauth.velocity.command.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.DatabaseMigrator;
import ru.matveylegenda.tiauth.database.DatabaseType;
import ru.matveylegenda.tiauth.hash.HashFactory;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

public class TiAuthCommand implements SimpleCommand {
    private final TiAuth plugin;
    private final ProxyServer proxy;
    private final Database database;
    private final AuthManager authManager;

    public TiAuthCommand(TiAuth plugin) {
        this.plugin = plugin;
        this.proxy = plugin.getServer();
        this.database = plugin.getDatabase();
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.usage);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("tiauth.admin.commands.reload")) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.noPermission);
                    return;
                }

                MainConfig.IMP.reload();
                MessagesConfig.IMP.reload();
                plugin.getAuthManager().setPasswordPattern(Pattern.compile(MainConfig.IMP.auth.passwordPattern));
                plugin.getAuthManager().setHash(HashFactory.create(MainConfig.IMP.auth.hashAlgorithm));
                CachedComponents.IMP = new CachedComponents(MessagesConfig.IMP);
                VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.config.reload);
            }

            case "unregister", "unreg" -> {
                if (!sender.hasPermission("tiauth.admin.commands.unregister")) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.noPermission);
                    return;
                }

                if (args.length < 2) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.unregister.usage);
                    return;
                }

                String playerName = args[1];
                authManager.unregisterPlayer(playerName, success -> {
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

            case "changepassword", "changepass" -> {
                if (!sender.hasPermission("tiauth.admin.commands.changepassword")) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.noPermission);
                    return;
                }

                if (args.length < 3) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.changePassword.usage);
                    return;
                }

                String playerName = args[1];
                String password = args[2];
                authManager.changePasswordPlayer(playerName, password, success -> {
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

            case "forcelogin" -> {
                if (!sender.hasPermission("tiauth.admin.commands.forcelogin")) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.noPermission);
                    return;
                }

                if (args.length < 2) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.forceLogin.usage);
                    return;
                }

                Player player = proxy.getPlayer(args[1]).orElse(null);
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

                authManager.loginPlayer(player, () -> VelocityUtils.sendMessage(
                        sender,
                        CachedComponents.IMP.admin.forceLogin.success
                                .replaceText(builder -> builder
                                        .match(VelocityUtils.PLAYER)
                                        .replacement(player.getUsername()))
                ));
            }

            case "forceregister" -> {
                if (!sender.hasPermission("tiauth.admin.commands.forceregister")) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.noPermission);
                    return;
                }

                if (args.length < 3) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.forceRegister.usage);
                    return;
                }

                String playerName = args[1];
                String password = args[2];

                database.getAuthUserRepository().getUser(playerName.toLowerCase(Locale.ROOT), (user, success) -> {
                    if (!success) {
                        VelocityUtils.sendMessage(sender, CachedComponents.IMP.queryError);
                        return;
                    }

                    if (user != null) {
                        VelocityUtils.sendMessage(
                                sender,
                                CachedComponents.IMP.admin.forceRegister.alreadyRegistered
                                        .replaceText(builder -> builder
                                                .match(VelocityUtils.PLAYER)
                                                .replacement(playerName))
                        );
                        return;
                    }

                    authManager.registerPlayer(playerName, password, null, success1 -> {
                        if (!success1) {
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

            case "migrate" -> {
                if (!sender.hasPermission("tiauth.admin.commands.migrate")) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.noPermission);
                    return;
                }

                if (args.length < 3) {
                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.migrate.usage);
                    return;
                }

                DatabaseMigrator.SourcePlugin sourcePlugin = DatabaseMigrator.SourcePlugin.valueOf(args[1].toUpperCase());
                DatabaseType sourceDatabase = DatabaseType.valueOf(args[2].toUpperCase());

                DatabaseMigrator databaseMigrator = new DatabaseMigrator(plugin.getDatabase());
                databaseMigrator.setSourcePlugin(sourcePlugin);
                databaseMigrator.setSourceDatabase(sourceDatabase);

                switch (sourceDatabase) {
                    case SQLITE -> {
                        if (args.length < 4) {
                            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.migrate.usage);
                            return;
                        }

                        databaseMigrator.setSourceDatabaseFile(new File(plugin.getDataFolder().toString(), args[3]).getAbsolutePath());
                    }

                    case H2 -> {
                        if (args.length < 6) {
                            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.migrate.usage);
                            return;
                        }

                        databaseMigrator.setSourceDatabaseFile(new File(plugin.getDataFolder().toString(), args[3]).getAbsolutePath());
                        if (!args[4].equals("empty")) {
                            databaseMigrator.setSourceDatabaseUser(args[4]);
                        }
                        if (!args[5].equals("empty")) {
                            databaseMigrator.setSourceDatabasePassword(args[5]);
                        }
                    }

                    case MYSQL, POSTGRESQL -> {
                        if (args.length < 8) {
                            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.migrate.usage);
                            return;
                        }

                        if (!args[3].equals("empty")) {
                            databaseMigrator.setSourceDatabaseUser(args[3]);
                        }
                        if (!args[4].equals("empty")) {
                            databaseMigrator.setSourceDatabasePassword(args[4]);
                        }
                        databaseMigrator.setSourceDatabaseHost(args[5]);
                        databaseMigrator.setSourceDatabasePort(args[6]);
                        databaseMigrator.setSourceDatabaseName(args[7]);
                    }
                }

                databaseMigrator.migrate(success -> {
                    if (!success) {
                        VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.migrate.error);
                        return;
                    }

                    VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.migrate.success);
                });
            }

            default -> VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.usage);
        }
    }
}