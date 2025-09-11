package ru.matveylegenda.tiauth.database;

import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.database.repository.AuthUserRepository;

import java.io.File;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class Database {
    private final ConnectionSource connectionSource;
    private final HikariDataSource dataSource;
    @Getter
    private final AuthUserRepository authUserRepository;
    private ExecutorService executor;

    private Database(ConnectionSource connectionSource) throws SQLException {
        this.connectionSource = connectionSource;
        this.dataSource = null;
        executor = Executors.newSingleThreadExecutor();
        this.authUserRepository = new AuthUserRepository(connectionSource, executor);
    }

    private Database(ConnectionSource connectionSource, HikariDataSource dataSource) throws SQLException {
        this.connectionSource = connectionSource;
        this.dataSource = dataSource;
        executor = Executors.newFixedThreadPool(dataSource.getMaximumPoolSize());
        this.authUserRepository = new AuthUserRepository(connectionSource, executor);
    }

    public static Database forSQLite(File file) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            TiAuth.logger.log(Level.WARNING, "SQLite JDBC driver not found", e);
        }

        ConnectionSource connectionSource = new JdbcConnectionSource("jdbc:sqlite:" + file.getAbsolutePath());
        return new Database(connectionSource);
    }

    public static Database forH2(File file,
                                    long connectionTimeout, long idleTimeout, long maxLifetime, int maxPoolSize, int minIdle) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:" + file.getAbsolutePath());
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setMaximumPoolSize(maxPoolSize);
        if (minIdle > 0) config.setMinimumIdle(minIdle);

        HikariDataSource dataSource = new HikariDataSource(config);
        ConnectionSource connectionSource = new DataSourceConnectionSource(dataSource, dataSource.getJdbcUrl());

        return new Database(connectionSource, dataSource);
    }

    public static Database forMySQL(String host, int port, String database, String user, String password,
                                    long connectionTimeout, long idleTimeout, long maxLifetime, int maxPoolSize, int minIdle) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(user);
        config.setPassword(password);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setMaximumPoolSize(maxPoolSize);
        if (minIdle > 0) config.setMinimumIdle(minIdle);

        HikariDataSource dataSource = new HikariDataSource(config);
        ConnectionSource connectionSource = new DataSourceConnectionSource(dataSource, dataSource.getJdbcUrl());

        return new Database(connectionSource, dataSource);
    }

    public static Database forPostgreSQL(String host, int port, String database, String user, String password,
                                    long connectionTimeout, long idleTimeout, long maxLifetime, int maxPoolSize, int minIdle) throws SQLException {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        config.setUsername(user);
        config.setPassword(password);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setMaximumPoolSize(maxPoolSize);
        if (minIdle > 0) config.setMinimumIdle(minIdle);

        config.setDriverClassName("org.postgresql.Driver");

        HikariDataSource dataSource = new HikariDataSource(config);
        ConnectionSource connectionSource = new DataSourceConnectionSource(dataSource, dataSource.getJdbcUrl());

        return new Database(connectionSource, dataSource);
    }

    public void close() throws Exception {
        if (dataSource != null) {
            dataSource.close();
        }

        executor.shutdown();
        connectionSource.close();
    }
}
