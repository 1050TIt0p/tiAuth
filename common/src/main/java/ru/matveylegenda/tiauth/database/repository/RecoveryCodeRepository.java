package ru.matveylegenda.tiauth.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.RecoveryCode;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public class RecoveryCodeRepository {

    private final ExecutorService executor;
    private final Dao<RecoveryCode, String> recoveryCodeDao;

    public RecoveryCodeRepository(ConnectionSource connectionSource, ExecutorService executor) throws SQLException {
        recoveryCodeDao = DaoManager.createDao(connectionSource, RecoveryCode.class);
        TableUtils.createTableIfNotExists(connectionSource, RecoveryCode.class);
        this.executor = executor;
    }

    public CompletableFuture<Void> addCodes(String[] codes, String username) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                recoveryCodeDao.callBatchTasks(() -> {
                    for (String code : codes) {
                        RecoveryCode recoveryCode = new RecoveryCode(code, username.toLowerCase(Locale.ROOT));
                        recoveryCodeDao.createOrUpdate(recoveryCode);
                    }
                    return null;
                });
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> removeCode(String code) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                RecoveryCode recoveryCode = recoveryCodeDao.queryForId(code);
                if (recoveryCode != null) {
                    recoveryCodeDao.delete(recoveryCode);
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> removeCodesByUsername(String username) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                DeleteBuilder<RecoveryCode, String> deleteBuilder = recoveryCodeDao.deleteBuilder();
                deleteBuilder.where().eq("username", username.toLowerCase(Locale.ROOT));
                deleteBuilder.delete();
                future.complete(null);
            } catch (SQLException e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
        return future;
    }

    public CompletableFuture<RecoveryCode> getRecoveryCode(String code) {
        CompletableFuture<RecoveryCode> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                RecoveryCode recoveryCode = recoveryCodeDao.queryForId(code);
                future.complete(recoveryCode);
            } catch (SQLException e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
        return future;
    }
}
