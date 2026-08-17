package ru.matveylegenda.tiauth.database.backup;

import com.google.gson.JsonElement;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.database.model.RecoveryCode;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;

public final class DatabaseBackup {
    public static final Logger LOGGER = Logger.getLogger("tiAuth-DatabaseBackup");

    private final Database database;
    private final BackupStorage storage = new BackupStorage();
    private final Map<String, AddonHandler<?>> addons = new ConcurrentHashMap<>();
    private final AtomicBoolean operationInProgress = new AtomicBoolean();

    public DatabaseBackup(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public <T> void registerAddon(
            String name,
            Class<T> dataType,
            Supplier<CompletableFuture<T>> createAction,
            Function<T, CompletableFuture<Void>> restoreAction
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Addon backup name cannot be empty");
        }

        AddonHandler<T> addon = new AddonHandler<>(
                Objects.requireNonNull(dataType, "dataType"),
                Objects.requireNonNull(createAction, "createAction"),
                Objects.requireNonNull(restoreAction, "restoreAction")
        );
        if (addons.putIfAbsent(name, addon) != null) {
            throw new IllegalArgumentException("Addon backup is already registered: " + name);
        }
    }

    public void unregisterAddon(String name) {
        addons.remove(name);
    }

    public CompletableFuture<Boolean> createBackup(File outputFile, int compressionLevel) {
        Objects.requireNonNull(outputFile, "outputFile");
        if (compressionLevel < Deflater.NO_COMPRESSION || compressionLevel > Deflater.BEST_COMPRESSION) {
            throw new IllegalArgumentException("Compression level must be between 0 and 9");
        }
        if (!operationInProgress.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            CompletableFuture<List<AuthUser>> users = database.getAuthUserRepository().getUsers();
            CompletableFuture<List<RecoveryCode>> recoveryCodes = database.getRecoveryCodeRepository().getCodes();
            CompletableFuture<Map<String, JsonElement>> addonData = createAddonData();

            return CompletableFuture.allOf(users, recoveryCodes, addonData)
                    .thenApply(ignored -> new BackupData(
                            BackupData.CURRENT_VERSION,
                            users.join(),
                            recoveryCodes.join(),
                            addonData.join()
                    ))
                    .thenApplyAsync(data -> {
                        storage.write(outputFile.toPath(), data, compressionLevel);
                        return true;
                    })
                    .exceptionally(throwable -> fail("Error during database backup creation", throwable))
                    .whenComplete((success, throwable) -> operationInProgress.set(false));
        } catch (RuntimeException exception) {
            operationInProgress.set(false);
            throw exception;
        }
    }

    public CompletableFuture<Boolean> restoreBackup(File backupFile) {
        Objects.requireNonNull(backupFile, "backupFile");
        if (!operationInProgress.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            return CompletableFuture.supplyAsync(() -> storage.read(backupFile.toPath()))
                    .thenCompose(this::restoreData)
                    .thenApply(unused -> true)
                    .exceptionally(throwable -> fail("Error during database backup restore", throwable))
                    .whenComplete((success, throwable) -> operationInProgress.set(false));
        } catch (RuntimeException exception) {
            operationInProgress.set(false);
            throw exception;
        }
    }

    private CompletableFuture<Void> restoreData(BackupData data) {
        return database.getAuthUserRepository().replaceUsers(data.users())
                .thenCompose(ignored -> database.getRecoveryCodeRepository().replaceCodes(data.recoveryCodes()))
                .whenComplete((ignored, throwable) -> clearCaches())
                .thenCompose(ignored -> restoreAddonData(data.addons()));
    }

    private CompletableFuture<Map<String, JsonElement>> createAddonData() {
        Map<String, CompletableFuture<JsonElement>> addonFutures = new LinkedHashMap<>();
        for (Map.Entry<String, AddonHandler<?>> entry : addons.entrySet()) {
            addonFutures.put(entry.getKey(), entry.getValue().createData());
        }

        CompletableFuture<?>[] futures = addonFutures.values().toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures)
                .thenApply(ignored -> {
                    Map<String, JsonElement> addonData = new LinkedHashMap<>();
                    addonFutures.forEach((name, future) -> addonData.put(name, future.join()));
                    return addonData;
                });
    }

    private CompletableFuture<Void> restoreAddonData(Map<String, JsonElement> data) {
        CompletableFuture<Void> restoreChain = CompletableFuture.completedFuture(null);

        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            AddonHandler<?> addon = addons.get(entry.getKey());
            if (addon == null) {
                LOGGER.warning("Skipping backup data for unloaded addon: " + entry.getKey());
                continue;
            }

            restoreChain = restoreChain.thenCompose(ignored -> addon.restoreData(entry.getValue()));
        }

        return restoreChain;
    }

    private void clearCaches() {
        AuthCache.clearAll();
        PremiumCache.clear();
        SessionCache.clear();
    }

    private boolean fail(String message, Throwable throwable) {
        LOGGER.log(Level.WARNING, message, throwable);
        return false;
    }

    private record AddonHandler<T>(
            Class<T> dataType,
            Supplier<CompletableFuture<T>> createAction,
            Function<T, CompletableFuture<Void>> restoreAction
    ) {
        private CompletableFuture<JsonElement> createData() {
            try {
                return createAction.get().thenApply(BackupStorage::toJson);
            } catch (RuntimeException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }

        private CompletableFuture<Void> restoreData(JsonElement json) {
            T data = BackupStorage.fromJson(json, dataType);
            return restoreAction.apply(data);
        }
    }
}
