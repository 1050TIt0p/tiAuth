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
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

public class RecoveryCodeRepository {

    private final ExecutorService executor;
    private final Dao<RecoveryCode, String> recoveryCodeDao;

    public RecoveryCodeRepository(ConnectionSource connectionSource, ExecutorService executor) throws SQLException {
        recoveryCodeDao = DaoManager.createDao(connectionSource, RecoveryCode.class);
        TableUtils.createTableIfNotExists(connectionSource, RecoveryCode.class);
        this.executor = executor;
    }

    public void addCodes(String[] codes, String username, Consumer<Boolean> callback) {
        executor.submit(() -> {
            try {
                recoveryCodeDao.callBatchTasks(() -> {
                    for (String code : codes) {
                        RecoveryCode recoveryCode = new RecoveryCode(code, username.toLowerCase(Locale.ROOT));
                        recoveryCodeDao.createOrUpdate(recoveryCode);
                    }
                    return null;
                });
                callback.accept(true);
            } catch (Exception e) {
                callback.accept(false);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
    }

    public void removeCode(String code, Consumer<Boolean> callback) {
        executor.submit(() -> {
            try {
                RecoveryCode recoveryCode = recoveryCodeDao.queryForId(code);
                if (recoveryCode != null) {
                    recoveryCodeDao.delete(recoveryCode);
                }

                callback.accept(true);
            } catch (Exception e) {
                callback.accept(false);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
    }

    public void removeCodesByUsername(String username, Consumer<Boolean> callback) {
        executor.submit(() -> {
            try {
                DeleteBuilder<RecoveryCode, String> deleteBuilder = recoveryCodeDao.deleteBuilder();
                deleteBuilder.where().eq("username", username.toLowerCase(Locale.ROOT));
                deleteBuilder.delete();

                callback.accept(true);
            } catch (SQLException e) {
                callback.accept(false);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
    }

    public void getRecoveryCode(String code, BiConsumer<RecoveryCode, Boolean> callback) {
        executor.submit(() -> {
            try {
                RecoveryCode recoveryCode = recoveryCodeDao.queryForId(code);
                callback.accept(recoveryCode, true);
            } catch (SQLException e) {
                callback.accept(null, false);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
    }
}
