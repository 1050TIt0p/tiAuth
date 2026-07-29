package ru.matveylegenda.tiauth.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.PremiumIdentity;

import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public final class PremiumIdentityRepository {
    private final ExecutorService executor;
    private final Dao<PremiumIdentity, String> identityDao;

    public PremiumIdentityRepository(ConnectionSource connectionSource, ExecutorService executor) throws SQLException {
        identityDao = DaoManager.createDao(connectionSource, PremiumIdentity.class);
        TableUtils.createTableIfNotExists(connectionSource, PremiumIdentity.class);
        this.executor = executor;
    }

    public CompletableFuture<UUID> getUuid(String username) {
        CompletableFuture<UUID> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                PremiumIdentity identity = identityDao.queryForId(username.toLowerCase(Locale.ROOT));
                future.complete(identity == null ? null : UUID.fromString(identity.getUuid()));
            } catch (SQLException | IllegalArgumentException exception) {
                future.completeExceptionally(exception);
                Database.LOGGER.log(Level.WARNING, "Error during premium identity query", exception);
            }
        });
        return future;
    }

    public CompletableFuture<Void> bind(String username, UUID uuid) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                String lowerName = username.toLowerCase(Locale.ROOT);
                PremiumIdentity identity = identityDao.queryForId(lowerName);

                if (identity == null) {
                    identityDao.create(new PremiumIdentity(lowerName, uuid));
                } else if (!identity.getUuid().equals(uuid.toString())) {
                    throw new IllegalStateException("Premium identity is already bound to another UUID");
                }

                future.complete(null);
            } catch (SQLException | RuntimeException exception) {
                future.completeExceptionally(exception);
                Database.LOGGER.log(Level.WARNING, "Error while binding premium identity", exception);
            }
        });
        return future;
    }
}
