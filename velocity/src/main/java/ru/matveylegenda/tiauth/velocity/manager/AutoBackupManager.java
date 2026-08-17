package ru.matveylegenda.tiauth.velocity.manager;

import com.velocitypowered.api.scheduler.ScheduledTask;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.velocity.TiAuth;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

        task = plugin.getServer().getScheduler().buildTask(plugin, this::createBackup)
                .delay(settings.intervalMinutes, TimeUnit.MINUTES)
                .repeat(settings.intervalMinutes, TimeUnit.MINUTES)
                .schedule();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private boolean isValid(MainConfig.Database.Backup settings) {
        if (settings.intervalMinutes < 1) {
            plugin.getLogger().warn("Automatic database backup interval must be at least 1 minute");
            return false;
        }
        if (settings.compressionLevel < 0 || settings.compressionLevel > 9) {
            plugin.getLogger().warn("Automatic database backup compression level must be between 0 and 9");
            return false;
        }
        return true;
    }

    private void createBackup() {
        if (!backupInProgress.compareAndSet(false, true)) {
            return;
        }

        MainConfig.Database.Backup settings = MainConfig.IMP.database.backup;
        Path backupFile = plugin.getDataFolder()
                .resolve("backups")
                .resolve(LocalDateTime.now().format(NAME_FORMAT));

        try {
            plugin.getDatabaseBackup().createBackup(backupFile.toFile(), settings.compressionLevel)
                    .whenComplete((success, throwable) -> {
                        backupInProgress.set(false);
                        if (throwable != null || !Boolean.TRUE.equals(success)) {
                            plugin.getLogger().warn("Could not create automatic database backup");
                            return;
                        }

                        plugin.getLogger().info("Automatic database backup created: {}", backupFile.getFileName());
                    });
        } catch (RuntimeException exception) {
            backupInProgress.set(false);
            plugin.getLogger().warn("Could not create automatic database backup", exception);
        }
    }
}
