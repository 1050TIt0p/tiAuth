package ru.matveylegenda.tiauth.bungee.manager;

import net.md_5.bungee.api.scheduler.ScheduledTask;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.config.MainConfig;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class AutoBackupManager {
    private static final DateTimeFormatter NAME_FORMAT = DateTimeFormatter.ofPattern(
            "'auto_'HH-mm-ss_dd-MM-yyyy'.backup'"
    );

    private final TiAuth plugin;
    private final AtomicBoolean backupInProgress = new AtomicBoolean();
    private ScheduledTask task;

    public AutoBackupManager(TiAuth plugin) {
        this.plugin = plugin;
    }

    public void restart() {
        stop();

        MainConfig.Database.Backup settings = MainConfig.IMP.database.backup;
        if (!settings.enabled) {
            return;
        }
        if (!isValid(settings)) {
            return;
        }

        task = plugin.getProxy().getScheduler().schedule(
                plugin,
                this::createBackup,
                settings.intervalMinutes,
                settings.intervalMinutes,
                TimeUnit.MINUTES
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private boolean isValid(MainConfig.Database.Backup settings) {
        if (settings.intervalMinutes < 1) {
            plugin.getLogger().warning("Automatic database backup interval must be at least 1 minute");
            return false;
        }
        if (settings.compressionLevel < 0 || settings.compressionLevel > 9) {
            plugin.getLogger().warning("Automatic database backup compression level must be between 0 and 9");
            return false;
        }
        return true;
    }

    private void createBackup() {
        if (!backupInProgress.compareAndSet(false, true)) {
            return;
        }

        MainConfig.Database.Backup settings = MainConfig.IMP.database.backup;
        Path backupFile = plugin.getDataFolder().toPath()
                .resolve("backups")
                .resolve(LocalDateTime.now().format(NAME_FORMAT));

        try {
            plugin.getDatabaseBackup().createBackup(backupFile.toFile(), settings.compressionLevel)
                    .whenComplete((success, throwable) -> {
                        backupInProgress.set(false);
                        if (throwable != null || !Boolean.TRUE.equals(success)) {
                            plugin.getLogger().warning("Could not create automatic database backup");
                            return;
                        }

                        plugin.getLogger().info("Automatic database backup created: " + backupFile.getFileName());
                    });
        } catch (RuntimeException exception) {
            backupInProgress.set(false);
            plugin.getLogger().log(Level.WARNING, "Could not create automatic database backup", exception);
        }
    }
}
