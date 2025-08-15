package ru.matveylegenda.tiauth.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import ru.matveylegenda.tiauth.database.model.AuthUser;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AuthUserRepository {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Dao<AuthUser, String> authUserDao;

    public AuthUserRepository(ConnectionSource connectionSource) throws SQLException {
        authUserDao = DaoManager.createDao(connectionSource, AuthUser.class);
        TableUtils.createTableIfNotExists(connectionSource, AuthUser.class);
    }

    public void registerUser(AuthUser user, Runnable callback) {
        executor.submit(() -> {
            try {
                authUserDao.create(user);
                if (callback != null) {
                    callback.run();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void getUser(String username, Consumer<AuthUser> callback) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username);
                callback.accept(user);
            } catch (SQLException e) {
                callback.accept(null);
                e.printStackTrace();
            }
        });
    }

    public void updatePassword(String username, String newPassword, Runnable callback) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username);
                if (user != null) {
                    user.setPassword(newPassword);
                    authUserDao.update(user);
                    if (callback != null) {
                        callback.run();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateLastLogin(String username) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username);
                if (user != null) {
                    user.setLastLogin(System.currentTimeMillis());
                    authUserDao.update(user);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateLastIp(String username, String ip) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username);
                if (user != null) {
                    user.setLastIp(ip);
                    authUserDao.update(user);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
