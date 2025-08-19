package ru.matveylegenda.tiauth;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bstats.bungeecord.Metrics;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.command.LoginCommand;
import ru.matveylegenda.tiauth.command.LogoutCommand;
import ru.matveylegenda.tiauth.command.PremiumCommand;
import ru.matveylegenda.tiauth.command.RegisterCommand;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.listener.AuthListener;
import ru.matveylegenda.tiauth.listener.ChatListener;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.ChatUtils;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public final class TiAuth extends Plugin {
    @Getter
    private static Logger logger;
    private Database database;
    private final MainConfig mainConfig = new MainConfig();
    private final MessagesConfig messagesConfig = new MessagesConfig();
    private final AuthCache authCache = new AuthCache();
    private final PremiumCache premiumCache = new PremiumCache();
    private final SessionCache sessionCache = new SessionCache(mainConfig);
    private final ChatUtils chatUtils = new ChatUtils(messagesConfig);
    private AuthManager authManager;

    @Override
    public void onEnable() {
        logger = getLogger();
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        initializeDatabase();
        loadConfigs();
        premiumCache.load(database.getAuthUserRepository());
        authManager = new AuthManager(this);

        PluginManager pluginManager = getProxy().getPluginManager();
        registerListeners(pluginManager);
        registerCommands(pluginManager);

        new Metrics(this, 26921);
    }

    @Override
    public void onDisable() {
        if (database != null) {
            try {
                database.getAuthUserRepository().getExecutor().shutdown();
                database.close();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error during database closing", e);
            }
        }
    }

    private void loadConfigs() {
        mainConfig.reload(Path.of(getDataFolder().getAbsolutePath(), "config.yml"));
        messagesConfig.reload(Path.of(getDataFolder().getAbsolutePath(), "messages.yml"));
    }

    private void initializeDatabase() {
        try {
            database = new Database(new File(getDataFolder(), "auth.db"));
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error during database initialization", e);
        }
    }

    private void registerListeners(PluginManager pluginManager) {
        pluginManager.registerListener(this, new AuthListener(this));
        pluginManager.registerListener(this, new ChatListener(this));
    }

    private void registerCommands(PluginManager pluginManager) {
        pluginManager.registerCommand(this, new LoginCommand(this, "login", "log", "l"));
        pluginManager.registerCommand(this, new RegisterCommand(this, "register", "reg"));
        pluginManager.registerCommand(this, new PremiumCommand(this, "premium"));
        pluginManager.registerCommand(this, new LogoutCommand(this, "logout"));
    }
}
