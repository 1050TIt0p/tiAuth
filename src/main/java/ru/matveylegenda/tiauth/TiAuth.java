package ru.matveylegenda.tiauth;

import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import ru.matveylegenda.tiauth.database.Database;

import java.io.File;
import java.sql.SQLException;

public final class TiAuth extends Plugin {
    public Database database;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        initializeDatabase();
        new Metrics(this, 26921);
    }

    @Override
    public void onDisable() {
        if (database != null) {
            try {
                database.close();
            } catch (Exception e) {
                getLogger().warning("Error during database closing: " + e.getMessage());
            }
        }
    }

    public void initializeDatabase() {
        try {
            database = new Database(new File(getDataFolder(), "tiauth.db"));
        } catch (SQLException e) {
            getLogger().warning("Error during database initialization: " + e.getMessage());
        }
    }
}
