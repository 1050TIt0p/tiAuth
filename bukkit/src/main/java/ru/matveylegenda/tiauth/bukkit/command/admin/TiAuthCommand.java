package ru.matveylegenda.tiauth.bukkit.command.admin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.matveylegenda.tiauth.bukkit.TiAuth;
import ru.matveylegenda.tiauth.bukkit.manager.AuthManager;
import ru.matveylegenda.tiauth.bukkit.storage.CachedMessages;
import ru.matveylegenda.tiauth.bukkit.util.BukkitUtils;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.hash.HashFactory;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

public class TiAuthCommand implements CommandExecutor {
    private final TiAuth plugin;
    private final Database database;
    private final AuthManager authManager;

    public TiAuthCommand(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.usage);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("tiauth.admin.commands.reload")) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.noPermission);
                    return true;
                }

                MainConfig.IMP.reload();
                MessagesConfig.IMP.reload();
                plugin.getAuthManager().setPasswordPattern(Pattern.compile(MainConfig.IMP.auth.passwordPattern));
                plugin.getAuthManager().setHash(HashFactory.create(MainConfig.IMP.auth.hashAlgorithm));
                CachedMessages.IMP = new CachedMessages(MessagesConfig.IMP);
                BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.config.reload);
            }

            case "unregister", "unreg" -> {
                if (!sender.hasPermission("tiauth.admin.commands.unregister")) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.noPermission);
                    return true;
                }

                if (args.length < 2) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.unregister.usage);
                    return true;
                }

                String playerName = args[1];
                authManager.unregisterPlayer(playerName, success -> plugin.runSync(() -> {
                    if (!success) {
                        BukkitUtils.sendMessage(sender, CachedMessages.IMP.queryError);
                        return;
                    }

                    Player player = plugin.getServer().getPlayer(playerName);
                    if (player != null) {
                        SessionCache.removePlayer(playerName);
                        player.kickPlayer(BukkitUtils.colorize(CachedMessages.IMP.player.unregister.success));
                    }

                    BukkitUtils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.unregister.success
                                    .replace("{player}", playerName)
                    );
                }));
            }

            case "changepassword", "changepass" -> {
                if (!sender.hasPermission("tiauth.admin.commands.changepassword")) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.noPermission);
                    return true;
                }

                if (args.length < 3) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.changePassword.usage);
                    return true;
                }

                String playerName = args[1];
                String password = args[2];
                authManager.changePasswordPlayer(playerName, password, success -> plugin.runSync(() -> {
                    if (!success) {
                        BukkitUtils.sendMessage(sender, CachedMessages.IMP.queryError);
                        return;
                    }

                    BukkitUtils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.changePassword.success
                                    .replace("{player}", playerName)
                    );
                }));
            }

            case "forcelogin" -> {
                if (!sender.hasPermission("tiauth.admin.commands.forcelogin")) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.noPermission);
                    return true;
                }

                if (args.length < 2) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.forceLogin.usage);
                    return true;
                }

                Player player = plugin.getServer().getPlayer(args[1]);
                if (player == null) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.playerNotFound);
                    return true;
                }

                if (AuthCache.isAuthenticated(player.getName())) {
                    BukkitUtils.sendMessage(
                            sender,
                            CachedMessages.IMP.admin.forceLogin.isAuthenticated
                                    .replace("{player}", player.getName())
                    );
                    return true;
                }

                authManager.loginPlayer(player, () -> plugin.runSync(() -> BukkitUtils.sendMessage(
                        sender,
                        CachedMessages.IMP.admin.forceLogin.success
                                .replace("{player}", player.getName())
                )));
            }

            case "forceregister" -> {
                if (!sender.hasPermission("tiauth.admin.commands.forceregister")) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.noPermission);
                    return true;
                }

                if (args.length < 3) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.forceRegister.usage);
                    return true;
                }

                String playerName = args[1];
                String password = args[2];

                database.getAuthUserRepository().getUser(playerName.toLowerCase(Locale.ROOT), (user, success) -> plugin.runSync(() -> {
                    if (!success) {
                        BukkitUtils.sendMessage(sender, CachedMessages.IMP.queryError);
                        return;
                    }

                    if (user != null) {
                        BukkitUtils.sendMessage(
                                sender,
                                CachedMessages.IMP.admin.forceRegister.alreadyRegistered
                                        .replace("{player}", playerName)
                        );
                        return;
                    }

                    authManager.registerPlayer(playerName, password, null, success1 -> plugin.runSync(() -> {
                        if (!success1) {
                            BukkitUtils.sendMessage(sender, CachedMessages.IMP.queryError);
                            return;
                        }

                        BukkitUtils.sendMessage(
                                sender,
                                CachedMessages.IMP.admin.forceRegister.success
                                        .replace("{player}", playerName)
                        );
                    }));
                }));
            }

            case "migrate" -> {
                if (!sender.hasPermission("tiauth.admin.commands.migrate")) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.noPermission);
                    return true;
                }

                if (args.length < 3) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.usage);
                    return true;
                }

                try {
                    ru.matveylegenda.tiauth.database.DatabaseMigrator.SourcePlugin sourcePlugin = ru.matveylegenda.tiauth.database.DatabaseMigrator.SourcePlugin.valueOf(args[1].toUpperCase());
                    ru.matveylegenda.tiauth.database.DatabaseType sourceDatabase = ru.matveylegenda.tiauth.database.DatabaseType.valueOf(args[2].toUpperCase());

                    ru.matveylegenda.tiauth.database.DatabaseMigrator databaseMigrator = new ru.matveylegenda.tiauth.database.DatabaseMigrator(plugin.getDatabase());
                    databaseMigrator.setSourcePlugin(sourcePlugin);
                    databaseMigrator.setSourceDatabase(sourceDatabase);

                    switch (sourceDatabase) {
                        case SQLITE -> {
                            if (args.length < 4) {
                                BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.usage);
                                return true;
                            }
                            databaseMigrator.setSourceDatabaseFile(new File(plugin.getDataFolder(), args[3]).getAbsolutePath());
                        }

                        case H2 -> {
                            if (args.length < 6) {
                                BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.usage);
                                return true;
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
                                BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.usage);
                                return true;
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

                    databaseMigrator.migrate(success -> plugin.runSync(() -> {
                        if (!success) {
                            BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.error);
                            return;
                        }

                        BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.success);
                    }));
                } catch (IllegalArgumentException e) {
                    BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.usage);
                }
            }

            default -> BukkitUtils.sendMessage(sender, CachedMessages.IMP.admin.usage);
        }

        return true;
    }
}
