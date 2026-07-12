package ru.matveylegenda.tiauth.database.repository;

import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public class AuthUserRepository {

    private final ExecutorService executor;
    private final DataSource dataSource;

    public AuthUserRepository(DataSource dataSource, ExecutorService executor) throws SQLException {
        this.dataSource = dataSource;
        this.executor = executor;
        createTable();
        migrateTotpColumn();
    }

    private void createTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS auth_users (" +
                    "username VARCHAR(255) PRIMARY KEY," +
                    "realName VARCHAR(255) NOT NULL," +
                    "password VARCHAR(255) NOT NULL," +
                    "premium BOOLEAN DEFAULT FALSE," +
                    "lastIp VARCHAR(255)," +
                    "regIp VARCHAR(255)," +
                    "lastLogin BIGINT," +
                    "regDate BIGINT," +
                    "totpToken VARCHAR(255) DEFAULT ''" +
                    ")"
            );
        }
    }

    private void migrateTotpColumn() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "ALTER TABLE auth_users ADD COLUMN totpToken VARCHAR(255) DEFAULT ''"
            );
        } catch (SQLException ignored) {
        }
    }

    public CompletableFuture<Void> registerUser(AuthUser user) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO auth_users (username, realName, password, premium, lastIp, regIp, lastLogin, regDate, totpToken) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                 )) {
                statement.setString(1, user.getUsername());
                statement.setString(2, user.getRealName());
                statement.setString(3, user.getPassword());
                statement.setBoolean(4, user.isPremium());
                statement.setString(5, user.getLastIp());
                statement.setString(6, user.getRegIp());
                statement.setLong(7, user.getLastLogin());
                statement.setLong(8, user.getRegDate());
                statement.setString(9, user.getTotpToken());
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> registerUsers(List<AuthUser> users) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement checkStatement = connection.prepareStatement(
                         "SELECT 1 FROM auth_users WHERE username = ?"
                 );
                 PreparedStatement insertStatement = connection.prepareStatement(
                         "INSERT INTO auth_users (username, realName, password, premium, lastIp, regIp, lastLogin, regDate, totpToken) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                 )) {
                connection.setAutoCommit(false);
                for (AuthUser user : users) {
                    checkStatement.setString(1, user.getUsername());
                    try (ResultSet rs = checkStatement.executeQuery()) {
                        if (!rs.next()) {
                            insertStatement.setString(1, user.getUsername());
                            insertStatement.setString(2, user.getRealName());
                            insertStatement.setString(3, user.getPassword());
                            insertStatement.setBoolean(4, user.isPremium());
                            insertStatement.setString(5, user.getLastIp());
                            insertStatement.setString(6, user.getRegIp());
                            insertStatement.setLong(7, user.getLastLogin());
                            insertStatement.setLong(8, user.getRegDate());
                            insertStatement.setString(9, user.getTotpToken());
                            insertStatement.executeUpdate();
                        }
                    }
                }
                connection.commit();
                connection.setAutoCommit(true);
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
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM auth_users WHERE username = ?"
                 )) {
                statement.setString(1, username.toLowerCase(Locale.ROOT));
                statement.executeUpdate();
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
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM auth_users WHERE username = ?"
                 )) {
                statement.setString(1, username.toLowerCase(Locale.ROOT));
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        AuthUser user = new AuthUser();
                        user.setUsername(resultSet.getString("username"));
                        user.setRealName(resultSet.getString("realName"));
                        user.setPassword(resultSet.getString("password"));
                        user.setPremium(resultSet.getBoolean("premium"));
                        user.setLastIp(resultSet.getString("lastIp"));
                        user.setRegIp(resultSet.getString("regIp"));
                        user.setLastLogin(resultSet.getLong("lastLogin"));
                        user.setRegDate(resultSet.getLong("regDate"));
                        user.setTotpToken(resultSet.getString("totpToken"));
                        future.complete(user);
                    } else {
                        future.complete(null);
                    }
                }
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
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT COUNT(*) FROM auth_users WHERE lastIp = ?"
                 )) {
                statement.setString(1, ip);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getInt(1));
                    } else {
                        future.complete(0);
                    }
                }
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
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE auth_users SET password = ? WHERE username = ?"
                 )) {
                statement.setString(1, newPassword);
                statement.setString(2, username.toLowerCase(Locale.ROOT));
                statement.executeUpdate();
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
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE auth_users SET lastLogin = ? WHERE username = ?"
                 )) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, username.toLowerCase(Locale.ROOT));
                statement.executeUpdate();
            } catch (SQLException e) {
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
    }

    public void updateLastIp(String username, String ip) {
        executor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE auth_users SET lastIp = ? WHERE username = ?"
                 )) {
                statement.setString(1, ip);
                statement.setString(2, username.toLowerCase(Locale.ROOT));
                statement.executeUpdate();
            } catch (SQLException e) {
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
    }

    public CompletableFuture<Void> updateTotpToken(String username, String totpToken) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE auth_users SET totpToken = ? WHERE username = ?"
                 )) {
                statement.setString(1, totpToken);
                statement.setString(2, username.toLowerCase(Locale.ROOT));
                statement.executeUpdate();
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
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE auth_users SET premium = ? WHERE username = ?"
                 )) {
                statement.setBoolean(1, enabled);
                statement.setString(2, username.toLowerCase(Locale.ROOT));
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                future.completeExceptionally(e);
                Database.LOGGER.log(Level.WARNING, "Error during database query", e);
            }
        });
        return future;
    }
}
