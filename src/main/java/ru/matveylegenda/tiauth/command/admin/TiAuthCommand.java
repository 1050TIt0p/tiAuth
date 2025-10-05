package ru.matveylegenda.tiauth.command.admin;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.DatabaseMigrator;
import ru.matveylegenda.tiauth.database.DatabaseType;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;
import ru.matveylegenda.tiauth.util.colorizer.ColorizedMessages;

import java.io.File;
import java.util.Locale;

public class TiAuthCommand extends Command {
    private final TiAuth plugin;
    private final Database database;
    private final Utils utils;
    private final ColorizedMessages colorizedMessages;
    private final AuthManager authManager;
    private final AuthCache authCache;
    private final SessionCache sessionCache;

    public TiAuthCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.utils = plugin.getUtils();
        this.colorizedMessages = plugin.getColorizedMessages();
        this.authManager = plugin.getAuthManager();
        this.authCache = plugin.getAuthCache();
        this.sessionCache = plugin.getSessionCache();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            utils.sendMessage(
                    sender,
                    colorizedMessages.admin.usage
            );
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("tiauth.admin.commands.reload")) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.noPermission
                    );
                    return;
                }

                plugin.loadConfigs(plugin.getDataFolder());
                colorizedMessages.load(plugin.getMessagesConfig());
                utils.sendMessage(
                        sender,
                        colorizedMessages.admin.config.reload
                );
            }

            case "unregister", "unreg" -> {
                if (!sender.hasPermission("tiauth.admin.commands.unregister")) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.noPermission
                    );
                    return;
                }

                if (args.length < 2) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.admin.unregister.usage
                    );
                    return;
                }

                String playerName = args[1];
                authManager.unregisterPlayer(playerName, success -> {
                    if (!success) {
                        utils.sendMessage(
                                sender,
                                colorizedMessages.queryError
                        );
                        return;
                    }

                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);
                    if (player != null) {
                        sessionCache.removePlayer(playerName);

                        utils.kickPlayer(
                                player,
                                colorizedMessages.player.unregister.success
                        );
                    }

                    utils.sendMessage(
                            sender,
                            colorizedMessages.admin.unregister.success
                                    .replace("{player}", playerName)
                    );
                });
            }

            case "changepassword", "changepass" -> {
                if (!sender.hasPermission("tiauth.admin.commands.changepassword")) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.noPermission
                    );
                    return;
                }

                if (args.length < 3) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.admin.changePassword.usage
                    );
                    return;
                }

                String playerName = args[1];
                String password = args[2];
                authManager.changePasswordPlayer(playerName, password, success -> {
                    if (!success) {
                        utils.sendMessage(
                                sender,
                                colorizedMessages.queryError
                        );
                        return;
                    }

                    utils.sendMessage(
                            sender,
                            colorizedMessages.admin.changePassword.success
                                    .replace("{player}", playerName)
                    );
                });
            }

            case "forcelogin" -> {
                if (!sender.hasPermission("tiauth.admin.commands.forcelogin")) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.noPermission
                    );
                    return;
                }

                if (args.length < 2) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.admin.forceLogin.usage
                    );
                    return;
                }

                ProxiedPlayer player = plugin.getProxy().getPlayer(args[1]);
                if (player == null) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.playerNotFound
                    );
                    return;
                }

                if (authCache.isAuthenticated(player.getName())) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.admin.forceLogin.isAuthenticated
                                    .replace("{player}", player.getName())
                    );
                    return;
                }

                authManager.loginPlayer(player, () -> {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.admin.forceLogin.success
                                    .replace("{player}", player.getName())
                    );
                });
            }

            case "forceregister" -> {
                // /auth forceregister <игрок> <пароль>
                if (!sender.hasPermission("tiauth.admin.commands.forceregister")) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.noPermission
                    );
                    return;
                }

                if (args.length < 3) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.admin.forceRegister.usage
                    );
                    return;
                }

                String playerName = args[1];
                String password = args[2];

                database.getAuthUserRepository().getUser(playerName.toLowerCase(Locale.ROOT), (user, success) -> {
                    if (!success) {
                        utils.sendMessage(
                                sender,
                                colorizedMessages.queryError
                        );
                        return;
                    }

                    if (user != null) {
                        utils.sendMessage(
                                sender,
                                colorizedMessages.admin.forceRegister.alreadyRegistered
                                        .replace("{player}", playerName)
                        );
                        return;
                    }

                    authManager.registerPlayer(playerName, password, null, success1 -> {
                        if (!success1) {
                            utils.sendMessage(
                                    sender,
                                    colorizedMessages.queryError
                            );
                            return;
                        }

                        utils.sendMessage(
                                sender,
                                colorizedMessages.admin.forceRegister.success
                                        .replace("{player}", playerName)
                        );
                    });
                });
            }

            case "migrate" -> {
                if (!sender.hasPermission("tiauth.admin.commands.migrate")) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.noPermission
                    );
                    return;
                }

                if (args.length < 3) {
                    utils.sendMessage(
                            sender,
                            colorizedMessages.admin.migrate.usage
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
                            utils.sendMessage(
                                    sender,
                                    colorizedMessages.admin.migrate.usage
                            );
                            return;
                        }

                        databaseMigrator.setSourceDatabaseFile(new File(plugin.getDataFolder(), args[3]).getAbsolutePath());
                    }

                    case H2 -> {
                        if (args.length < 6) {
                            utils.sendMessage(
                                    sender,
                                    colorizedMessages.admin.migrate.usage
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
                            utils.sendMessage(
                                    sender,
                                    colorizedMessages.admin.migrate.usage
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
                        utils.sendMessage(
                                sender,
                                colorizedMessages.admin.migrate.error
                        );
                        return;
                    }

                    utils.sendMessage(
                            sender,
                            colorizedMessages.admin.migrate.success
                    );
                });
            }
        }
    }
}
