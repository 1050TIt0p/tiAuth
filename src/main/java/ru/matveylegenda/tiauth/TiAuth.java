package ru.matveylegenda.tiauth;

import lombok.Getter;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bstats.bungeecord.Metrics;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.command.*;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.listener.AuthListener;
import ru.matveylegenda.tiauth.listener.ChatListener;
import ru.matveylegenda.tiauth.listener.DialogListener;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.manager.TaskManager;
import ru.matveylegenda.tiauth.util.Utils;
import ua.nanit.limbo.server.LimboServer;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public final class TiAuth extends Plugin {
    public static Logger logger;
    private Database database;
    private final MainConfig mainConfig = new MainConfig();
    private final MessagesConfig messagesConfig = new MessagesConfig();
    private final AuthCache authCache = new AuthCache();
    private final PremiumCache premiumCache = new PremiumCache();
    private SessionCache sessionCache;
    private final Utils utils = new Utils(messagesConfig);
    private TaskManager taskManager;
    private AuthManager authManager;

    @Override
    public void onEnable() {
        logger = getLogger();
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        loadConfigs();
        initializeDatabase();
        startLimboServer();
        premiumCache.load(database.getAuthUserRepository());
        sessionCache = new SessionCache(mainConfig.auth.sessionLifetimeMinutes);
        taskManager = new TaskManager(this);
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
                database.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error during database closing", e);
            }
        }
    }

    private void loadConfigs() {
        mainConfig.reload(Path.of(getDataFolder().getAbsolutePath(), "config.yml"));
        messagesConfig.reload(Path.of(getDataFolder().getAbsolutePath(), "messages.yml"));
    }

    private void initializeDatabase() {
        try {
            switch (mainConfig.database.type) {
                case SQLITE -> database = Database.forSQLite(new File(getDataFolder(), "auth.db"));
                case H2 -> database = Database.forH2(
                        new File(getDataFolder(), "auth-v2"),
                        mainConfig.database.connectionTimeoutMs,
                        mainConfig.database.idleTimeoutMs,
                        mainConfig.database.maxLifetimeMs,
                        mainConfig.database.maxPoolSize,
                        mainConfig.database.minIdle
                );
                case MYSQL -> database = Database.forMySQL(
                        mainConfig.database.host,
                        mainConfig.database.port,
                        mainConfig.database.database,
                        mainConfig.database.user,
                        mainConfig.database.password,
                        mainConfig.database.connectionTimeoutMs,
                        mainConfig.database.idleTimeoutMs,
                        mainConfig.database.maxLifetimeMs,
                        mainConfig.database.maxPoolSize,
                        mainConfig.database.minIdle
                );
                case POSTGRESQL -> database = Database.forPostgreSQL(
                        mainConfig.database.host,
                        mainConfig.database.port,
                        mainConfig.database.database,
                        mainConfig.database.user,
                        mainConfig.database.password,
                        mainConfig.database.connectionTimeoutMs,
                        mainConfig.database.idleTimeoutMs,
                        mainConfig.database.maxLifetimeMs,
                        mainConfig.database.maxPoolSize,
                        mainConfig.database.minIdle
                );
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error during database initialization", e);
        }
    }

    private void startLimboServer() {
        if (mainConfig.servers.useVirtualServer) {
            try {
                Path limboPath = getDataFolder().toPath().resolve("limbo");
                if (!limboPath.toFile().exists()) {
                    limboPath.toFile().mkdir();
                }
                LimboServer limboServer = new LimboServer();
                limboServer.start(limboPath);

                ServerInfo authServer = getProxy().constructServerInfo(mainConfig.servers.auth, limboServer.getConfig().getAddress(), "auth server", false);
                getProxy().getServers().put(
                        mainConfig.servers.auth,
                        authServer
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error when starting the virtual server", e);
                getProxy().stop();
            }
        }
    }

    private void registerListeners(PluginManager pluginManager) {
        pluginManager.registerListener(this, new AuthListener(this));
        pluginManager.registerListener(this, new DialogListener(this));
        pluginManager.registerListener(this, new ChatListener(this));
    }

    private void registerCommands(PluginManager pluginManager) {
        pluginManager.registerCommand(this, new LoginCommand(this, "login", "log", "l"));
        pluginManager.registerCommand(this, new RegisterCommand(this, "register", "reg"));
        pluginManager.registerCommand(this, new UnregisterCommand(this, "unregister", "unreg"));
        pluginManager.registerCommand(this, new ChangePasswordCommand(this, "changepassword", "changepass"));
        pluginManager.registerCommand(this, new PremiumCommand(this, "premium"));
        pluginManager.registerCommand(this, new LogoutCommand(this, "logout"));
    }
}
