package ru.matveylegenda.tiauth.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.Getter;
import ru.matveylegenda.tiauth.database.model.AuthUser;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AuthUserRepository {
    @Getter
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
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                callback.accept(user);
            } catch (SQLException e) {
                callback.accept(null);
                e.printStackTrace();
            }
        });
    }

    public void getAllUsers(Consumer<List<AuthUser>> callback) {
        executor.submit(() -> {
            try {
                List<AuthUser> users = authUserDao.queryForAll();
                callback.accept(users);
            } catch (SQLException e) {
                callback.accept(null);
                e.printStackTrace();
            }
        });
    }

    public void updatePassword(String username, String newPassword, Runnable callback) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
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
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
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
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    user.setLastIp(ip);
                    authUserDao.update(user);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void setPremium(String username, boolean enabled) {
        executor.submit(() -> {
            try {
                AuthUser user = authUserDao.queryForId(username.toLowerCase(Locale.ROOT));
                if (user != null) {
                    user.setPremium(enabled);
                    authUserDao.update(user);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
