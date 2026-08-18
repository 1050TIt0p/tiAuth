package ru.matveylegenda.tiauth.bungee.command.admin;

import net.md_5.bungee.api.CommandSender;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.database.backup.DatabaseBackup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class BackupCommand implements AdminSubcommand {
    private static final String FILE_EXTENSION = ".backup";
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9._-]{0,63}$");
    private static final DateTimeFormatter DEFAULT_NAME_FORMAT = DateTimeFormatter.ofPattern(
            "HH-mm-ss_dd-MM-yyyy"
    );
    private static final List<String> ACTIONS = List.of("create", "restore");
    private static final List<String> COMPRESSION_LEVELS = List.of(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
    );

    private final DatabaseBackup databaseBackup;
    private final Path backupDirectory;
    private final AtomicBoolean operationInProgress = new AtomicBoolean();

    public BackupCommand(TiAuth plugin) {
        this.databaseBackup = plugin.getDatabaseBackup();
        this.backupDirectory = plugin.getDataFolder().toPath().resolve("backups");
    }

    @Override
    public String permission() {
        return "tiauth.admin.commands.backup";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "restore" -> restore(sender, args);
            default -> sendUsage(sender);
        }
    }

    @Override
    public List<String> suggest(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(ACTIONS, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("restore")) {
            return getBackupNames(args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return filter(COMPRESSION_LEVELS, args[2]);
        }

        return List.of();
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length > 3) {
            sendUsage(sender);
            return;
        }

        String name = args.length >= 2
                ? normalizeName(args[1])
                : LocalDateTime.now().format(DEFAULT_NAME_FORMAT);
        if (!isValidName(name)) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.backup.invalidFileName);
            return;
        }

        int compressionLevel = 0;
        if (args.length == 3) {
            try {
                compressionLevel = Integer.parseInt(args[2]);
            } catch (NumberFormatException exception) {
                BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.backup.invalidCompression);
                return;
            }

            if (compressionLevel < 0 || compressionLevel > 9) {
                BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.backup.invalidCompression);
                return;
            }
        }

        Path backupFile = getBackupFile(name);
        if (Files.exists(backupFile)) {
            sendWithName(sender, CachedMessages.IMP.admin.backup.alreadyExists, name);
            return;
        }

        if (!operationInProgress.compareAndSet(false, true)) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.processing);
            return;
        }

        sendWithName(sender, CachedMessages.IMP.admin.backup.creating, name);
        try {
            databaseBackup.createBackup(backupFile.toFile(), compressionLevel)
                    .whenComplete((success, throwable) -> {
                        operationInProgress.set(false);
                        if (throwable != null || !Boolean.TRUE.equals(success)) {
                            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.backup.createError);
                            return;
                        }

                        sendWithName(sender, CachedMessages.IMP.admin.backup.createSuccess, name);
                    });
        } catch (RuntimeException exception) {
            operationInProgress.set(false);
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.backup.createError);
        }
    }

    private void restore(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        String name = normalizeName(args[1]);
        if (!isValidName(name)) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.backup.invalidFileName);
            return;
        }

        Path backupFile = getBackupFile(name);
        if (!Files.isRegularFile(backupFile)) {
            sendWithName(sender, CachedMessages.IMP.admin.backup.notFound, name);
            return;
        }

        if (!operationInProgress.compareAndSet(false, true)) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.processing);
            return;
        }

        sendWithName(sender, CachedMessages.IMP.admin.backup.restoring, name);
        try {
            databaseBackup.restoreBackup(backupFile.toFile())
                    .whenComplete((success, throwable) -> {
                        operationInProgress.set(false);
                        if (throwable != null || !Boolean.TRUE.equals(success)) {
                            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.backup.restoreError);
                            return;
                        }

                        sendWithName(sender, CachedMessages.IMP.admin.backup.restoreSuccess, name);
                    });
        } catch (RuntimeException exception) {
            operationInProgress.set(false);
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.backup.restoreError);
        }
    }

    private List<String> getBackupNames(String input) {
        if (!Files.isDirectory(backupDirectory)) {
            return List.of();
        }

        String prefix = input.toLowerCase(Locale.ROOT);
        try (Stream<Path> files = Files.list(backupDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(FILE_EXTENSION))
                    .map(name -> name.substring(0, name.length() - FILE_EXTENSION.length()))
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private List<String> filter(List<String> values, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.startsWith(prefix))
                .toList();
    }

    private Path getBackupFile(String name) {
        return backupDirectory.resolve(name + FILE_EXTENSION);
    }

    private String normalizeName(String name) {
        if (name.toLowerCase(Locale.ROOT).endsWith(FILE_EXTENSION)) {
            return name.substring(0, name.length() - FILE_EXTENSION.length());
        }
        return name;
    }

    private boolean isValidName(String name) {
        return NAME_PATTERN.matcher(name).matches();
    }

    private void sendUsage(CommandSender sender) {
        BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.backup.usage);
    }

    private void sendWithName(CommandSender sender, String message, String name) {
        BungeeUtils.sendMessage(sender, message.replace("{backup}", name));
    }
}
