package ru.matveylegenda.tiauth.bungee.command.admin;

import net.md_5.bungee.api.CommandSender;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.database.DatabaseMigrator;
import ru.matveylegenda.tiauth.database.DatabaseType;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

public class MigrateCommand implements AdminSubcommand {
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private final TiAuth plugin;

    public MigrateCommand(TiAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public String permission() {
        return "tiauth.admin.commands.migrate";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.usage);
            return;
        }

        DatabaseMigrator.SourcePlugin sourcePlugin = DatabaseMigrator.SourcePlugin.valueOf(
                args[0].toUpperCase(Locale.ROOT)
        );
        DatabaseType sourceDatabase = DatabaseType.valueOf(args[1].toUpperCase(Locale.ROOT));

        DatabaseMigrator databaseMigrator = new DatabaseMigrator(plugin.getDatabase());
        databaseMigrator.setSourcePlugin(sourcePlugin);
        databaseMigrator.setSourceDatabase(sourceDatabase);

        switch (sourceDatabase) {
            case SQLITE -> {
                if (args.length < 3) {
                    BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.usage);
                    return;
                }

                String fileName = args[2];
                if (!isValidFileName(fileName)) {
                    BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.invalidFileName);
                    return;
                }

                databaseMigrator.setSourceDatabaseFile(
                        new File(plugin.getDataFolder(), fileName).getAbsolutePath()
                );
            }

            case H2 -> {
                if (args.length < 5) {
                    BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.usage);
                    return;
                }

                String fileName = args[2];
                if (!isValidFileName(fileName)) {
                    BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.invalidFileName);
                    return;
                }

                databaseMigrator.setSourceDatabaseFile(
                        new File(plugin.getDataFolder(), fileName).getAbsolutePath()
                );
                if (!args[3].equals("empty")) {
                    databaseMigrator.setSourceDatabaseUser(args[3]);
                }
                if (!args[4].equals("empty")) {
                    databaseMigrator.setSourceDatabasePassword(args[4]);
                }
            }

            case MYSQL, POSTGRESQL -> {
                if (args.length < 7) {
                    BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.usage);
                    return;
                }

                if (!args[2].equals("empty")) {
                    databaseMigrator.setSourceDatabaseUser(args[2]);
                }
                if (!args[3].equals("empty")) {
                    databaseMigrator.setSourceDatabasePassword(args[3]);
                }
                databaseMigrator.setSourceDatabaseHost(args[4]);
                databaseMigrator.setSourceDatabasePort(args[5]);
                databaseMigrator.setSourceDatabaseName(args[6]);
            }
        }

        databaseMigrator.migrate()
                .thenAccept(result -> BungeeUtils.sendMessage(
                        sender,
                        CachedMessages.IMP.admin.migrate.success
                ))
                .exceptionally(throwable -> {
                    BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.migrate.error);
                    return null;
                });
    }

    private boolean isValidFileName(String fileName) {
        return FILE_NAME_PATTERN.matcher(fileName).matches() && !fileName.contains("..");
    }
}
