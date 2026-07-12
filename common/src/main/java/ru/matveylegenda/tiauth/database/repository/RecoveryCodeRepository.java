package ru.matveylegenda.tiauth.database.repository;

import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.RecoveryCode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public class RecoveryCodeRepository {

    private final ExecutorService executor;
    private final DataSource dataSource;

    public RecoveryCodeRepository(DataSource dataSource, ExecutorService executor) throws SQLException {
        this.dataSource = dataSource;
        this.executor = executor;
        createTable();
    }

    private void createTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS recovery_codes (" +
                    "recoveryCode VARCHAR(255) PRIMARY KEY," +
                    "username VARCHAR(255) NOT NULL" +
                    ")"
            );
        }
    }

    public CompletableFuture<Void> addCodes(String[] codes, String username) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement deleteStmt = connection.prepareStatement(
                         "DELETE FROM recovery_codes WHERE recoveryCode = ?"
                 );
                 PreparedStatement insertStmt = connection.prepareStatement(
                         "INSERT INTO recovery_codes (recoveryCode, username) VALUES (?, ?)"
                 )) {
                connection.setAutoCommit(false);
                for (String code : codes) {
                    deleteStmt.setString(1, code);
                    deleteStmt.executeUpdate();
                    insertStmt.setString(1, code);
                    insertStmt.setString(2, username.toLowerCase(Locale.ROOT));
                    insertStmt.executeUpdate();
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

    public CompletableFuture<Void> removeCode(String code) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM recovery_codes WHERE recoveryCode = ?"
                 )) {
                statement.setString(1, code);
                statement.executeUpdate();
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
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM recovery_codes WHERE username = ?"
                 )) {
                statement.setString(1, username.toLowerCase(Locale.ROOT));
                statement.executeUpdate();
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
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM recovery_codes WHERE recoveryCode = ?"
                 )) {
                statement.setString(1, code);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        RecoveryCode recoveryCode = new RecoveryCode();
                        recoveryCode.setRecoveryCode(resultSet.getString("recoveryCode"));
                        recoveryCode.setUsername(resultSet.getString("username"));
                        future.complete(recoveryCode);
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
}
