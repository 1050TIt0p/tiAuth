package ru.matveylegenda.tiauth.database;

import lombok.Setter;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.database.model.AuthUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

@Setter
public class DatabaseMigrator {
    private final TiAuth plugin;
    private final Database database;

    private DatabaseType sourceDatabase;
    private SourcePlugin sourcePlugin;

    private String sourceDatabaseFile;
    private String sourceDatabaseHost;
    private String sourceDatabasePort;
    private String sourceDatabaseName;
    private String sourceDatabaseUser = "";
    private String sourceDatabasePassword = "";

    public DatabaseMigrator(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
    }

    public void migrate(Consumer<Boolean> callback) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            List<AuthUser> authUsers = new ArrayList<>();

            try (Connection connection = getConnection(sourceDatabase);
                 Statement statement = connection.createStatement()) {
                switch (sourcePlugin) {
                    case TIAUTH -> {
                        ResultSet resultSet = statement.executeQuery("SELECT * FROM auth_users");

                        while (resultSet.next()) {
                            authUsers.add(
                                    new AuthUser(
                                            resultSet.getString("username"),
                                            resultSet.getString("realName"),
                                            resultSet.getString("password"),
                                            resultSet.getBoolean("premium"),
                                            resultSet.getString("lastIp"),
                                            resultSet.getString("regIp"),
                                            resultSet.getLong("lastLogin"),
                                            resultSet.getLong("regDate")
                                    )
                            );
                        }
                    }

                    case MCAUTH -> {
                        ResultSet resultSet = statement.executeQuery("SELECT * FROM mc_auth_accounts");

                        while (resultSet.next()) {
                            authUsers.add(
                                    new AuthUser(
                                            resultSet.getString("player_id"),
                                            resultSet.getString("player_name"),
                                            resultSet.getString("password_hash"),
                                            false,
                                            resultSet.getString("last_ip"),
                                            resultSet.getString("last_ip"),
                                            resultSet.getLong("last_session_start"),
                                            resultSet.getLong("last_session_start")
                                    )
                            );
                        }
                    }

                    case LIMBOAUTH -> {
                        ResultSet resultSet = statement.executeQuery("SELECT * FROM AUTH");

                        while (resultSet.next()) {
                            authUsers.add(
                                    new AuthUser(
                                            resultSet.getString("LOWERCASENICKNAME"),
                                            resultSet.getString("NICKNAME"),
                                            resultSet.getString("HASH"),
                                            resultSet.getString("PREMIUMUUID") != null,
                                            resultSet.getString("LOGINIP"),
                                            resultSet.getString("IP"),
                                            resultSet.getLong("LOGINDATE"),
                                            resultSet.getLong("REGDATE")
                                    )
                            );
                        }
                    }

                    case AUTHME -> {
                        ResultSet resultSet = statement.executeQuery("SELECT * FROM authme");

                        while (resultSet.next()) {
                            Timestamp lastLogin = resultSet.getTimestamp("lastlogin");
                            long lastLoginTime = (lastLogin != null) ? lastLogin.getTime() : System.currentTimeMillis();

                            Timestamp regDate = resultSet.getTimestamp("regdate");
                            long regDateTime = (regDate != null) ? regDate.getTime() : System.currentTimeMillis();

                            authUsers.add(
                                    new AuthUser(
                                            resultSet.getString("username"),
                                            resultSet.getString("realname"),
                                            resultSet.getString("password"),
                                            false,
                                            resultSet.getString("regip"),
                                            resultSet.getString("regip"),
                                            lastLoginTime,
                                            regDateTime
                                    )
                            );
                        }
                    }
                }

                database.getAuthUserRepository().registerUsers(authUsers, callback);
            } catch (SQLException e) {
                callback.accept(false);
                e.printStackTrace();
            }
        });
    }

    private Connection getConnection(DatabaseType sourceDatabase) throws SQLException {
        return switch (sourceDatabase) {
            case SQLITE -> {
                try {
                    Driver driver = new org.sqlite.JDBC();
                    Properties props = new Properties();
                    yield driver.connect("jdbc:sqlite:" + sourceDatabaseFile, props);
                } catch (Exception e) {
                    throw new SQLException("Failed to connect to SQLite", e);
                }
            }
            case H2 -> {
                try {
                    Driver driver = new org.h2.Driver();
                    Properties props = new Properties();
                    props.setProperty("user", sourceDatabaseUser);
                    props.setProperty("password", sourceDatabasePassword);
                    yield driver.connect("jdbc:h2:" + sourceDatabaseFile, props);
                } catch (Exception e) {
                    throw new SQLException("Failed to connect to H2", e);
                }
            }
            case MYSQL -> {
                try {
                    Driver driver = new com.mysql.cj.jdbc.Driver();
                    Properties props = new Properties();
                    props.setProperty("user", sourceDatabaseUser);
                    props.setProperty("password", sourceDatabasePassword);
                    yield driver.connect(
                            "jdbc:mysql://" + sourceDatabaseHost + ":" + sourceDatabasePort + "/" + sourceDatabaseName,
                            props
                    );
                } catch (Exception e) {
                    throw new SQLException("Failed to connect to MySQL", e);
                }
            }
            case POSTGRESQL -> {
                try {
                    Driver driver = new org.postgresql.Driver();
                    Properties props = new Properties();
                    props.setProperty("user", sourceDatabaseUser);
                    props.setProperty("password", sourceDatabasePassword);
                    yield driver.connect(
                            "jdbc:postgresql://" + sourceDatabaseHost + ":" + sourceDatabasePort + "/" + sourceDatabaseName,
                            props
                    );
                } catch (Exception e) {
                    throw new SQLException("Failed to connect to PostgreSQL", e);
                }
            }
        };
    }

    public enum SourcePlugin {
        TIAUTH,
        MCAUTH,
        LIMBOAUTH,
        AUTHME
    }
}