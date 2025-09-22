package ru.matveylegenda.tiauth.command.admin;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.DatabaseMigrator;
import ru.matveylegenda.tiauth.database.DatabaseType;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;

import java.io.File;

public class TiAuthCommand extends Command {
    private final TiAuth plugin;
    private final MessagesConfig messagesConfig;
    private final Utils utils;
    private final AuthManager authManager;
    private final AuthCache authCache;
    private final SessionCache sessionCache;

    public TiAuthCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.plugin = plugin;
        this.messagesConfig = plugin.getMessagesConfig();
        this.utils = plugin.getUtils();
        this.authManager = plugin.getAuthManager();
        this.authCache = plugin.getAuthCache();
        this.sessionCache = plugin.getSessionCache();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            utils.sendMessage(
                    sender,
                    messagesConfig.admin.usage
            );
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("tiauth.admin.commands.reload")) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.noPermission
                    );
                    return;
                }

                plugin.loadConfigs();
                utils.sendMessage(
                        sender,
                        messagesConfig.admin.config.reload
                );
            }

            case "unregister", "unreg" -> {
                if (!sender.hasPermission("tiauth.admin.commands.unregister")) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.noPermission
                    );
                    return;
                }

                if (args.length < 2) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.unregister.usage
                    );
                    return;
                }

                String playerName = args[1];
                authManager.unregisterPlayer(playerName, success -> {
                    if (!success) {
                        utils.sendMessage(
                                sender,
                                messagesConfig.queryError
                        );
                        return;
                    }

                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);
                    if (player != null) {
                        sessionCache.removePlayer(playerName);

                        utils.kickPlayer(
                                player,
                                messagesConfig.player.unregister.success
                        );
                    }

                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.unregister.success
                                    .replace("{player}", playerName)
                    );
                });
            }

            case "changepassword", "changepass" -> {
                if (!sender.hasPermission("tiauth.admin.commands.changepassword")) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.noPermission
                    );
                    return;
                }

                if (args.length < 3) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.changePassword.usage
                    );
                    return;
                }

                String playerName = args[1];
                String password = args[2];
                authManager.changePasswordPlayer(playerName, password, success -> {
                    if (!success) {
                        utils.sendMessage(
                                sender,
                                messagesConfig.queryError
                        );
                        return;
                    }

                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.changePassword.success
                                    .replace("{player}", playerName)
                    );
                });
            }

            case "forcelogin" -> {
                if (!sender.hasPermission("tiauth.admin.commands.forcelogin")) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.noPermission
                    );
                    return;
                }

                if (args.length < 2) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.forceLogin.usage
                    );
                    return;
                }

                ProxiedPlayer player = plugin.getProxy().getPlayer(args[1]);
                if (player == null) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.playerNotFound
                    );
                    return;
                }

                if (authCache.isAuthenticated(player.getName())) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.forceLogin.isAuthenticated
                                    .replace("{player}", player.getName())
                    );
                    return;
                }

                authManager.loginPlayer(player, () -> {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.forceLogin.success
                                    .replace("{player}", player.getName())
                    );
                });
            }

            case "migrate" -> {
                if (!sender.hasPermission("tiauth.admin.commands.migrate")) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.noPermission
                    );
                    return;
                }

                if (args.length < 3) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.migrate.usage
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
                                    messagesConfig.admin.migrate.usage
                            );
                            return;
                        }

                        databaseMigrator.setSourceDatabaseFile(new File(plugin.getDataFolder(), args[3]).getAbsolutePath());
                    }

                    case H2 -> {
                        if (args.length < 6) {
                            utils.sendMessage(
                                    sender,
                                    messagesConfig.admin.migrate.usage
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
                                    messagesConfig.admin.migrate.usage
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
                                messagesConfig.admin.migrate.error
                        );
                        return;
                    }

                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.migrate.success
                    );
                });
            }
        }
    }
}
