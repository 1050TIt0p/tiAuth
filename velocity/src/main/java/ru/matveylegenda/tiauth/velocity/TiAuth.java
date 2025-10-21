package ru.matveylegenda.tiauth.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.byteflux.libby.Library;
import net.byteflux.libby.VelocityLibraryManager;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.util.Utils;
import ru.matveylegenda.tiauth.velocity.command.admin.TiAuthCommand;
import ru.matveylegenda.tiauth.velocity.command.player.*;
import ru.matveylegenda.tiauth.velocity.listener.AuthListener;
import ru.matveylegenda.tiauth.velocity.listener.ChatListener;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.manager.TaskManager;
import ua.nanit.limbo.server.LimboServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.sql.SQLException;

@Getter
@Plugin(
        id = "tiauth",
        name = "tiAuth",
        version = "1.3.0",
        authors = {"1050TI_top", "OverwriteMC"}
)
public final class TiAuth {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataFolder;

    private final Metrics.Factory metricsFactory;

    private Database database;
    private TaskManager taskManager;
    private AuthManager authManager;

    @Inject
    public TiAuth(ProxyServer server, Logger logger, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataFolder = Path.of("plugins/tiAuth/");
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {

        MainConfig.IMP.reload();
        MessagesConfig.IMP.reload();
        loadLibraries();
        initializeDatabase(dataFolder.toFile());
        startLimboServer(dataFolder.toFile());

        Utils.initializeColorizer(MainConfig.IMP.serializer);
        taskManager = new TaskManager(this);
        authManager = new AuthManager(this);

        registerListeners();
        registerCommands();

        metricsFactory.make(this, 27629);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (database != null) {
            try {
                database.close();
            } catch (Exception e) {
                logger.warn("Error during database closing", e);
            }
        }
    }

    private void loadLibraries() {
        Library sqliteJdbc = Library.builder()
                .groupId("org.xerial")
                .artifactId("sqlite-jdbc")
                .version(MainConfig.IMP.libraries.sqlite.version)
                .build();

        Library h2Jdbc = Library.builder()
                .groupId("com.h2database")
                .artifactId("h2")
                .version(MainConfig.IMP.libraries.h2.version)
                .build();

        Library mysqlJdbc = Library.builder()
                .groupId("com.mysql")
                .artifactId("mysql-connector-j")
                .version(MainConfig.IMP.libraries.mysql.version)
                .build();

        Library postgresqlJdbc = Library.builder()
                .groupId("org.postgresql")
                .artifactId("postgresql")
                .version(MainConfig.IMP.libraries.postgresql.version)
                .build();

        Library adventureApi = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-api")
                .version("4.24.0")
                .build();

        Library adventureMinimessage = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-minimessage")
                .version("4.24.0")
                .build();

        Library adventureLegacy = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-serializer-legacy")
                .version("4.24.0")
                .build();

        VelocityLibraryManager<TiAuth> libraryManager = new VelocityLibraryManager<>(
                logger,
                dataFolder,
                server.getPluginManager(),
                this
        );

        libraryManager.addMavenCentral();

        libraryManager.loadLibrary(sqliteJdbc);
        libraryManager.loadLibrary(h2Jdbc);
        libraryManager.loadLibrary(mysqlJdbc);
        libraryManager.loadLibrary(postgresqlJdbc);
        libraryManager.loadLibrary(adventureApi);
        libraryManager.loadLibrary(adventureMinimessage);
        libraryManager.loadLibrary(adventureLegacy);
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
            logger.error("Error during database initialization", e);
            server.shutdown();
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

                server.registerServer(
                        new com.velocitypowered.api.proxy.server.ServerInfo(
                                MainConfig.IMP.servers.auth,
                                (InetSocketAddress) limboServer.getConfig().getAddress()
                        )
                );
            } catch (Exception e) {
                logger.warn("Error when starting the virtual server", e);
                server.shutdown();
            }
        }
    }

    private void registerListeners() {
        server.getEventManager().register(this, new AuthListener(this));
        server.getEventManager().register(this, new ChatListener());
    }

    private void registerCommands() {
        CommandManager commandManager = server.getCommandManager();
        commandManager.register(commandManager.metaBuilder("tiauth").aliases("auth").build(), new TiAuthCommand(this));
        commandManager.register(commandManager.metaBuilder("login").aliases("l").build(), new LoginCommand(this));
        commandManager.register(commandManager.metaBuilder("register").aliases("reg").build(), new RegisterCommand(this));
        commandManager.register(commandManager.metaBuilder("unregister").aliases("unreg").build(), new UnregisterCommand(this));
        commandManager.register(commandManager.metaBuilder("changepassword").aliases("changepass").build(), new ChangePasswordCommand(this));
        commandManager.register(commandManager.metaBuilder("premium").build(), new PremiumCommand(this));
        commandManager.register(commandManager.metaBuilder("logout").build(), new LogoutCommand(this));
    }
}
