package ru.matveylegenda.tiauth.database;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import lombok.Getter;
import ru.matveylegenda.tiauth.database.repository.AuthUserRepository;

import java.io.File;
import java.sql.SQLException;

public class Database {
    private final ConnectionSource connectionSource;
    @Getter
    private final AuthUserRepository authUserRepository;

    public Database(File file) throws SQLException {
        connectionSource = new JdbcConnectionSource("jdbc:sqlite:" + file.getAbsolutePath());

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        authUserRepository = new AuthUserRepository(connectionSource);
    }

    public void close() throws Exception {
        connectionSource.close();
    }
}
