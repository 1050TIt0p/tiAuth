package ru.matveylegenda.tiauth;

import lombok.Getter;
import net.byteflux.libby.BungeeLibraryManager;
import net.byteflux.libby.Library;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bstats.bungeecord.Metrics;
import ru.matveylegenda.tiauth.command.admin.TiAuthCommand;
import ru.matveylegenda.tiauth.command.player.*;
import ru.matveylegenda.tiauth.config.MainConfig;
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
    private TaskManager taskManager;
    private AuthManager authManager;

    @Override
    public void onLoad() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        loadConfigs(dataFolder);
        loadLibraries();
    }

    @Override
    public void onEnable() {
        logger = getLogger();
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        initializeDatabase(dataFolder);
        startLimboServer(dataFolder);
        Utils.initializeColorizer(MainConfig.IMP.serializer);
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

    private void loadLibraries() {
        Library sqliteJdbc = Library.builder()
                .groupId("org{}xerial")
                .artifactId("sqlite-jdbc")
                .version(mainConfig.libraries.sqlite.version)
                .build();

        Library h2Jdbc = Library.builder()
                .groupId("com{}h2database")
                .artifactId("h2")
                .version(mainConfig.libraries.h2.version)
                .build();

        Library mysqlJdbc = Library.builder()
                .groupId("com{}mysql")
                .artifactId("mysql-connector-j")
                .version(mainConfig.libraries.mysql.version)
                .build();

        Library postgresqlJdbc = Library.builder()
                .groupId("org{}postgresql")
                .artifactId("postgresql")
                .version(mainConfig.libraries.postgresql.version)
                .build();

        BungeeLibraryManager libraryManager = new BungeeLibraryManager(this);
        libraryManager.addMavenCentral();
        libraryManager.loadLibrary(sqliteJdbc);
        libraryManager.loadLibrary(h2Jdbc);
        libraryManager.loadLibrary(mysqlJdbc);
        libraryManager.loadLibrary(postgresqlJdbc);
    }

    private void initializeDatabase(File dataFolder) {
        try {
            switch (MainConfig.IMP.database.type) {
                case SQLITE -> database = Database.forSQLite(new File(dataFolder, "auth.db"));
                case H2 -> database = Database.forH2(
                        new File(dataFolder, "auth-v2"),
                        MainConfig.IMP.database.connectionTimeoutMs,
                        MainConfig.IMP.database.idleTimeoutMs,
                        MainConfig.IMP.database.maxLifetimeMs,
                        MainConfig.IMP.database.maxPoolSize,
                        MainConfig.IMP.database.minIdle
                );
                case MYSQL -> database = Database.forMySQL(
                        MainConfig.IMP.database.host,
                        MainConfig.IMP.database.port,
                        MainConfig.IMP.database.database,
                        MainConfig.IMP.database.user,
                        MainConfig.IMP.database.password,
                        MainConfig.IMP.database.connectionTimeoutMs,
                        MainConfig.IMP.database.idleTimeoutMs,
                        MainConfig.IMP.database.maxLifetimeMs,
                        MainConfig.IMP.database.maxPoolSize,
                        MainConfig.IMP.database.minIdle
                );
                case POSTGRESQL -> database = Database.forPostgreSQL(
                        MainConfig.IMP.database.host,
                        MainConfig.IMP.database.port,
                        MainConfig.IMP.database.database,
                        MainConfig.IMP.database.user,
                        MainConfig.IMP.database.password,
                        MainConfig.IMP.database.connectionTimeoutMs,
                        MainConfig.IMP.database.idleTimeoutMs,
                        MainConfig.IMP.database.maxLifetimeMs,
                        MainConfig.IMP.database.maxPoolSize,
                        MainConfig.IMP.database.minIdle
                );
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error during database initialization", e);
            getProxy().stop();
        }
    }

    private void startLimboServer(File dataFolder) {
        if (MainConfig.IMP.servers.useVirtualServer) {
            try {
                Path limboPath = dataFolder.toPath().resolve("limbo");
                if (!limboPath.toFile().exists()) {
                    limboPath.toFile().mkdir();
                }
                LimboServer limboServer = new LimboServer();
                limboServer.start(limboPath);

                ServerInfo authServer = getProxy().constructServerInfo(MainConfig.IMP.servers.auth, limboServer.getConfig().getAddress(), "auth server", false);
                getProxy().getServers().put(
                        MainConfig.IMP.servers.auth,
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
        pluginManager.registerListener(this, new ChatListener());
    }

    private void registerCommands(PluginManager pluginManager) {
        pluginManager.registerCommand(this, new TiAuthCommand(this, "tiauth", "auth"));
        pluginManager.registerCommand(this, new LoginCommand(this, "login", "log", "l"));
        pluginManager.registerCommand(this, new RegisterCommand(this, "register", "reg"));
        pluginManager.registerCommand(this, new UnregisterCommand(this, "unregister", "unreg"));
        pluginManager.registerCommand(this, new ChangePasswordCommand(this, "changepassword", "changepass"));
        pluginManager.registerCommand(this, new PremiumCommand(this, "premium"));
        pluginManager.registerCommand(this, new LogoutCommand(this, "logout"));
    }
}
