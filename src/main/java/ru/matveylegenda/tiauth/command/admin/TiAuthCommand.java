package ru.matveylegenda.tiauth.command.admin;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.CachedMessages;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.DatabaseMigrator;
import ru.matveylegenda.tiauth.database.DatabaseType;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;

import java.io.File;
import java.util.Locale;

public class TiAuthCommand extends Command {
    private final TiAuth plugin;
    private final Database database;
    private final AuthManager authManager;

    public TiAuthCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Utils.sendMessage(
                    sender,
                    CachedMessages.IMP.admin.usage
            );
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("tiauth.admin.commands.reload")) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.noPermission
                    );
                    return;
                }

                MainConfig.IMP.reload();
                MessagesConfig.IMP.reload();
                CachedMessages.IMP = new CachedMessages(MessagesConfig.IMP);
                Utils.sendMessage(
                        sender,
                        CachedMessages.IMP.admin.config.reload
                );
            }

            case "unregister", "unreg" -> {
                if (!sender.hasPermission("tiauth.admin.commands.unregister")) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.noPermission
                    );
                    return;
                }

                if (args.length < 2) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.unregister.usage
                    );
                    return;
                }

                String playerName = args[1];
                authManager.unregisterPlayer(playerName, success -> {
                    if (!success) {
                        Utils.sendMessage(
                                sender,
                                CachedMessages.IMP.queryError
                        );
                        return;
                    }

                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);
                    if (player != null) {
                        SessionCache.removePlayer(playerName);
                        player.disconnect(CachedMessages.IMP.player.unregister.success);
                    }

                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.unregister.success
                                    .replace("{player}", playerName)
                    );
                });
            }

            case "changepassword", "changepass" -> {
                if (!sender.hasPermission("tiauth.admin.commands.changepassword")) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.noPermission
                    );
                    return;
                }

                if (args.length < 3) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.changePassword.usage
                    );
                    return;
                }

                String playerName = args[1];
                String password = args[2];
                authManager.changePasswordPlayer(playerName, password, success -> {
                    if (!success) {
                        Utils.sendMessage(
                                sender,
                                CachedMessages.IMP.queryError
                        );
                        return;
                    }

                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.changePassword.success
                                    .replace("{player}", playerName)
                    );
                });
            }

            case "forcelogin" -> {
                if (!sender.hasPermission("tiauth.admin.commands.forcelogin")) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.noPermission
                    );
                    return;
                }

                if (args.length < 2) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.forceLogin.usage
                    );
                    return;
                }

                ProxiedPlayer player = plugin.getProxy().getPlayer(args[1]);
                if (player == null) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.playerNotFound
                    );
                    return;
                }

                if (AuthCache.isAuthenticated(player.getName())) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.forceLogin.isAuthenticated
                                    .replace("{player}", player.getName())
                    );
                    return;
                }

                authManager.loginPlayer(player, () -> {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.forceLogin.success
                                    .replace("{player}", player.getName())
                    );
                });
            }

            case "forceregister" -> {
                // /auth forceregister <игрок> <пароль>
                if (!sender.hasPermission("tiauth.admin.commands.forceregister")) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.noPermission
                    );
                    return;
                }

                if (args.length < 3) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.forceRegister.usage
                    );
                    return;
                }

                String playerName = args[1];
                String password = args[2];

                database.getAuthUserRepository().getUser(playerName.toLowerCase(Locale.ROOT), (user, success) -> {
                    if (!success) {
                        Utils.sendMessage(
                                sender,
                                CachedMessages.IMP.queryError
                        );
                        return;
                    }

                    if (user != null) {
                        Utils.sendMessage(
                                sender,
                                CachedMessages.IMP.admin.forceRegister.alreadyRegistered
                                        .replace("{player}", playerName)
                        );
                        return;
                    }

                    authManager.registerPlayer(playerName, password, null, success1 -> {
                        if (!success1) {
                            Utils.sendMessage(
                                    sender,
                                    CachedMessages.IMP.queryError
                            );
                            return;
                        }

                        Utils.sendMessage(
                                sender,
                                CachedMessages.IMP.admin.forceRegister.success
                                        .replace("{player}", playerName)
                        );
                    });
                });
            }

            case "migrate" -> {
                if (!sender.hasPermission("tiauth.admin.commands.migrate")) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.noPermission
                    );
                    return;
                }

                if (args.length < 3) {
                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.migrate.usage
                    );
                    return;
                }

                DatabaseMigrator.SourcePlugin sourcePlugin = DatabaseMigrator.SourcePlugin.valueOf(args[1].toUpperCase());
                DatabaseType sourceDatabase = DatabaseType.valueOf(args[2].toUpperCase());

                DatabaseMigrator databaseMigrator = new DatabaseMigrator(plugin);
                databaseMigrator.setSourcePlugin(sourcePlugin);
                databaseMigrator.setSourceDatabase(sourceDatabase);

                switch (sourceDatabase) {
                    case SQLITE -> {
                        if (args.length < 4) {
                            Utils.sendMessage(
                                    sender,
                                    CachedMessages.IMP.admin.migrate.usage
                            );
                            return;
                        }

                        databaseMigrator.setSourceDatabaseFile(new File(plugin.getDataFolder(), args[3]).getAbsolutePath());
                    }

                    case H2 -> {
                        if (args.length < 6) {
                            Utils.sendMessage(
                                    sender,
                                    CachedMessages.IMP.admin.migrate.usage
                            );
                            return;
                        }

                        databaseMigrator.setSourceDatabaseFile(new File(plugin.getDataFolder(), args[3]).getAbsolutePath());
                        if (!args[4].equals("empty")) {
                            databaseMigrator.setSourceDatabaseUser(args[4]);
                        }
                        if (!args[5].equals("empty")) {
                            databaseMigrator.setSourceDatabasePassword(args[5]);
                        }
                    }

                    case MYSQL, POSTGRESQL -> {
                        if (args.length < 8) {
                            Utils.sendMessage(
                                    sender,
                                    CachedMessages.IMP.admin.migrate.usage
                            );
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
                        Utils.sendMessage(
                                sender,
                                CachedMessages.IMP.admin.migrate.error
                        );
                        return;
                    }

                    Utils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.migrate.success
                    );
                });
            }
        }
    }
}
