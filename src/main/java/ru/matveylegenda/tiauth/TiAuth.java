package ru.matveylegenda.tiauth;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bstats.bungeecord.Metrics;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.command.LoginCommand;
import ru.matveylegenda.tiauth.command.PremiumCommand;
import ru.matveylegenda.tiauth.command.RegisterCommand;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.listener.AuthListener;
import ru.matveylegenda.tiauth.listener.ChatListener;
import ru.matveylegenda.tiauth.util.ChatUtils;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;

public final class TiAuth extends Plugin {
    public Database database;
    public MainConfig mainConfig = new MainConfig();
    public MessagesConfig messagesConfig = new MessagesConfig();
    public final AuthCache authCache = new AuthCache();
    public final PremiumCache premiumCache = new PremiumCache();
    public final SessionCache sessionCache = new SessionCache(mainConfig);
    public final ChatUtils chatUtils = new ChatUtils(messagesConfig);

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        initializeDatabase();
        loadConfigs();
        premiumCache.load(database.getAuthUserRepository());

        PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerListener(this, new AuthListener(this));
        pluginManager.registerListener(this, new ChatListener(this));
        pluginManager.registerCommand(this, new LoginCommand(this, "login", "log", "l"));
        pluginManager.registerCommand(this, new RegisterCommand(this, "register", "reg"));
        pluginManager.registerCommand(this, new PremiumCommand(this, "premium"));

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

    private void loadConfigs() {
        mainConfig.reload(Path.of(getDataFolder().getAbsolutePath(), "config.yml"));
        messagesConfig.reload(Path.of(getDataFolder().getAbsolutePath(), "messages.yml"));
    }

    public void initializeDatabase() {
        try {
            database = new Database(new File(getDataFolder(), "auth.db"));
        } catch (SQLException e) {
            getLogger().warning("Error during database initialization: " + e.getMessage());
        }
    }
}
