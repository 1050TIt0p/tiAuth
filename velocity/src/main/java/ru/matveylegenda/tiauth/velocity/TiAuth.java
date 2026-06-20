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
import ru.matveylegenda.tiauth.util.KeyLoader;
import ru.matveylegenda.tiauth.util.Utils;
import ru.matveylegenda.tiauth.velocity.api.TiAuthAPI;
import ru.matveylegenda.tiauth.velocity.command.admin.TiAuthCommand;
import ru.matveylegenda.tiauth.velocity.command.player.ChangePasswordCommand;
import ru.matveylegenda.tiauth.velocity.command.player.LoginCommand;
import ru.matveylegenda.tiauth.velocity.command.player.LogoutCommand;
import ru.matveylegenda.tiauth.velocity.command.player.PremiumCommand;
import ru.matveylegenda.tiauth.velocity.command.player.RegisterCommand;
import ru.matveylegenda.tiauth.velocity.command.player.TotpCommand;
import ru.matveylegenda.tiauth.velocity.command.player.UnregisterCommand;
import ru.matveylegenda.tiauth.velocity.listener.AuthListener;
import ru.matveylegenda.tiauth.velocity.listener.ChatListener;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.manager.TaskManager;
import ua.nanit.limbo.server.LimboServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

@Getter
@Plugin(
        id = "tiauth",
        name = "tiAuth",
        version = "1.3.6",
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

    private byte[] secretKey;

    @Inject
    public TiAuth(ProxyServer server, Logger logger, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataFolder = Path.of("plugins/tiAuth/");
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        if (!isSupportedVersion()) {
            logger.warn("*** ВНИМАНИЕ ***");
            logger.warn("tiAuth поддерживает Velocity версии 3.4.0 и выше!");
            logger.warn("Вы пытаетесь запустить плагин на версии {}", server.getVersion().getVersion());
            logger.warn("Обновите прокси, если хотите использовать tiAuth.");
            return;
        }
        MainConfig.IMP.reload();
        MessagesConfig.IMP.reload();
        initializeSecretKey(dataFolder.toFile());
        loadLibraries();
        initializeDatabase(dataFolder.toFile());
        startLimboServer(dataFolder.toFile());

        Utils.initializeColorizer(MainConfig.IMP.serializer);
        taskManager = new TaskManager(this);
        authManager = new AuthManager(this);

        registerListeners();
        registerCommands();

        metricsFactory.make(this, 27629);

        new TiAuthAPI(this);

        Utils.checkUpdates()
                .thenAccept(version -> {
                    if (version.equals(getPluginVersion())) {
                        logger.info("You are using the latest version");
                        return;
                    }

                    logger.info("A new version is available: " + version);
                    logger.info("Download: https://github.com/1050TIt0p/tiAuth/releases");
                })
                .exceptionally(e -> {
                    logger.warn("Update check failed", e);
                    return null;
                });
    }

    private boolean isSupportedVersion() {
        return Integer.parseInt(server.getVersion().getVersion().split("-")[0].split("\\.")[1]) >= 4;
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
    }

    private void initializeSecretKey(File dataFolder) {
        try {
            secretKey = KeyLoader.loadOrGenerateKey(dataFolder.toPath());
        } catch (IOException e) {
            logger.error("Error during secret key initialization. Stopping server...", e);
            server.shutdown();
        }
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
        } catch (Exception e) {
            logger.error("Error during database initialization. Stopping server...", e);
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
                logger.warn("Error when starting the virtual server. Stopping server...", e);
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
        commandManager.register(commandManager.metaBuilder("2fa").aliases("totp").build(), new TotpCommand(this));
    }

    private String getPluginVersion() {
        return server.getPluginManager()
                .getPlugin("tiauth")
                .flatMap(container -> container.getDescription().getVersion())
                .orElse("unknown");
    }
}
