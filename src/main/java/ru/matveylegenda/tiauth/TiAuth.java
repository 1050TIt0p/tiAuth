package ru.matveylegenda.tiauth;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bstats.bungeecord.Metrics;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.command.LoginCommand;
import ru.matveylegenda.tiauth.command.PremiumCommand;
import ru.matveylegenda.tiauth.command.RegisterCommand;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.listener.AuthListener;

import java.io.File;
import java.sql.SQLException;

public final class TiAuth extends Plugin {
    public Database database;
    public final AuthCache authCache = new AuthCache();
    public final PremiumCache premiumCache = new PremiumCache();

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        initializeDatabase();
        premiumCache.load(database.getAuthUserRepository());
        new Metrics(this, 26921);

        PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerListener(this, new AuthListener(this));

        pluginManager.registerCommand(this, new LoginCommand(this, "login", "log", "l"));
        pluginManager.registerCommand(this, new RegisterCommand(this, "register", "reg"));
        pluginManager.registerCommand(this, new PremiumCommand(this, "premium"));
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
