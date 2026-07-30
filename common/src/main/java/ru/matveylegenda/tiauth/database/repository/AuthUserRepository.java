package ru.matveylegenda.tiauth.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public class AuthUserRepository {

    private final ExecutorService executor;
    private final Dao<AuthUser, String> authUserDao;

    public AuthUserRepository(ConnectionSource connectionSource, ExecutorService executor) throws SQLException {
        authUserDao = DaoManager.createDao(connectionSource, AuthUser.class);
        TableUtils.createTableIfNotExists(connectionSource, AuthUser.class);
        migrateTotpColumn();
        migratePremiumUuidColumn();
        this.executor = executor;
    }

    private void migrateTotpColumn() {
        try {
            authUserDao.executeRawNoArgs(
                    "ALTER TABLE auth_users ADD COLUMN totpToken VARCHAR(255) DEFAULT ''"
            );
        } catch (SQLException ignored) {
        }
    }

    private void migratePremiumUuidColumn() {
        try {
            authUserDao.executeRawNoArgs(
                    "ALTER TABLE auth_users ADD COLUMN premiumUuid VARCHAR(36)"
            );
        } catch (SQLException ignored) {
        }
    }

    public CompletableFuture<Void> registerUser(AuthUser user) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                authUserDao.create(user);
                future.complete(null);
            } catch (SQLException e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> registerPremiumUser(String username, UUID premiumUuid, String ip) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                String lowerName = username.toLowerCase(Locale.ROOT);
                AuthUser user = authUserDao.queryForId(lowerName);

                if (user == null) {
                    user = new AuthUser(lowerName, username, "", true, ip);
                    user.setPremiumUuid(premiumUuid.toString());
                    authUserDao.create(user);
                } else if (user.isAutomaticPremium()) {
                    if (!premiumUuid.toString().equalsIgnoreCase(user.getPremiumUuid())) {
                        throw new IllegalStateException("Premium account is already bound to another UUID");
                    }
                } else if (!user.isPremium()) {
                    throw new IllegalStateException("Nickname is already registered with a password");
                }

                future.complete(null);
            } catch (SQLException | RuntimeException exception) {
                future.completeExceptionally(exception);
                Database.LOGGER.log(Level.WARNING, "Error while registering premium account", exception);
            }
        });
        return future;
    }

    public CompletableFuture<Void> registerUsers(List<AuthUser> users) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                authUserDao.callBatchTasks(() -> {
                    for (AuthUser user : users) {
                        AuthUser exist = authUserDao.queryForId(user.getUsername());
                        if (exist == null) {
                            authUserDao.create(user);
                        }
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

    public CompletableFuture<Void> deleteUser(String username) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    authUserDao.delete(user);
                }
                future.complete(null);
            } catch (SQLException e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database delete query", e);
            }
        });
        return future;
    }

    public CompletableFuture<AuthUser> getUser(String username) {
        CompletableFuture<AuthUser> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                future.complete(user);
            } catch (SQLException e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
        return future;
    }

    public CompletableFuture<Integer> getUserCountByIp(String ip) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                int count = (int) authUserDao.queryBuilder()
                        .where()
                        .eq("lastIp", ip)
                        .and()
                        .ne("password", "")
                        .countOf();
                future.complete(count);
            } catch (SQLException e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during IP count query", e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> updatePassword(String username, String newPassword) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    user.setPassword(newPassword);
                    authUserDao.update(user);
                }
                future.complete(null);
            } catch (SQLException e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
        return future;
    }

    public void updateLastLogin(String username) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    user.setLastLogin(System.currentTimeMillis());
                    authUserDao.update(user);
                }
            } catch (SQLException e) {
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
    }

    public void updateLastIp(String username, String ip) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    user.setLastIp(ip);
                    authUserDao.update(user);
                }
            } catch (SQLException e) {
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
    }

    public CompletableFuture<Void> updateTotpToken(String username, String totpToken) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    user.setTotpToken(totpToken);
                    authUserDao.update(user);
                }
                future.complete(null);
            } catch (SQLException e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> setPremium(String username, boolean enabled) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    if (!enabled && user.isAutomaticPremium() && !user.hasPassword()) {
                        throw new IllegalStateException("Premium account needs a password before disabling premium mode");
                    }

                    user.setPremium(enabled);
                    if (!enabled) {
                        user.setPremiumUuid(null);
                    }
                    authUserDao.update(user);
                }
                future.complete(null);
            } catch (SQLException | RuntimeException exception) {
                future.completeExceptionally(exception);
                Database.LOGGER.log(Level.WARNING, "Error during database query", exception);
            }
        });
        return future;
    }
}
