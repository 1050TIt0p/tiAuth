package ru.matveylegenda.tiauth.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.database.model.AuthUser;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

public class AuthUserRepository {
    private ExecutorService executor;
    private final Dao<AuthUser, String> authUserDao;

    public AuthUserRepository(ConnectionSource connectionSource, ExecutorService executor) throws SQLException {
        authUserDao = DaoManager.createDao(connectionSource, AuthUser.class);
        TableUtils.createTableIfNotExists(connectionSource, AuthUser.class);
        this.executor = executor;
    }

    public void registerUser(AuthUser user, Consumer<Boolean> callback) {
        executor.submit(() -> {
            try {
                authUserDao.create(user);
                if (callback != null) {
                    callback.accept(true);
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.accept(false);
                }
                TiAuth.logger.log(Level.WARNING, "Error during database query", e);
            }
        });
    }

    public void deleteUser(String username, Consumer<Boolean> callback) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    authUserDao.delete(user);
                }

                if (callback != null) {
                    callback.accept(true);
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.accept(false);
                }
                TiAuth.logger.log(Level.WARNING, "Error during database delete query", e);
            }
        });
    }


    public void getUser(String username, BiConsumer<AuthUser, Boolean> callback) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                callback.accept(user, true);
            } catch (SQLException e) {
                callback.accept(null, false);
                TiAuth.logger.log(Level.WARNING, "Error during database query", e);
            }
        });
    }

    public void updatePassword(String username, String newPassword, Consumer<Boolean> callback) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    user.setPassword(newPassword);
                    authUserDao.update(user);
                }

                if (callback != null) {
                    callback.accept(true);
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.accept(false);
                }
                TiAuth.logger.log(Level.WARNING, "Error during database query", e);
            }
        });
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
                TiAuth.logger.log(Level.WARNING, "Error during database query", e);
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
                TiAuth.logger.log(Level.WARNING, "Error during database query", e);
            }
        });
    }

    public void setPremium(String username, boolean enabled, Consumer<Boolean> callback) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    user.setPremium(enabled);
                    authUserDao.update(user);
                }

                if (callback != null) {
                    callback.accept(true);
                }
            } catch (SQLException e) {
                if (callback != null) {
                    callback.accept(false);
                }
                TiAuth.logger.log(Level.WARNING, "Error during database query", e);
            }
        });
    }
}
