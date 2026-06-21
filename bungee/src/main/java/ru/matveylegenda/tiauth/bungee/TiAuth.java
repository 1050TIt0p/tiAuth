package ru.matveylegenda.tiauth.bungee;

import lombok.Getter;
import net.byteflux.libby.BungeeLibraryManager;
import net.byteflux.libby.Library;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bstats.bungeecord.Metrics;
import ru.matveylegenda.tiauth.bungee.api.TiAuthAPI;
import ru.matveylegenda.tiauth.bungee.command.admin.TiAuthCommand;
import ru.matveylegenda.tiauth.bungee.command.player.*;
import ru.matveylegenda.tiauth.bungee.listener.AuthListener;
import ru.matveylegenda.tiauth.bungee.listener.ChatListener;
import ru.matveylegenda.tiauth.bungee.listener.DialogListener;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.manager.TaskManager;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.picolimbo.LibraryLoader;
import ru.matveylegenda.tiauth.picolimbo.PicoLimboRunner;
import ru.matveylegenda.tiauth.util.KeyLoader;
import ru.matveylegenda.tiauth.util.Utils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public final class TiAuth extends Plugin {
    public static Logger logger;
    private Database database;
    private TaskManager taskManager;
    private AuthManager authManager;

    private byte[] secretKey;

    private PicoLimboRunner worker;

    @Override
    public void onLoad() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        MainConfig.IMP.reload();
        MessagesConfig.IMP = new MessagesConfig(MessagesConfig.getMessagesPath(MainConfig.IMP.lang));
        MessagesConfig.IMP.reload();
        initializeSecretKey(dataFolder);
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
        new TiAuthAPI(this);

        Utils.checkUpdates()
                .thenAccept(version -> {
                    if (version.equals(getDescription().getVersion())) {
                        logger.info("You are using the latest version");
                        return;
                    }

                    logger.info("A new version is available: " + version);
                    logger.info("Download: https://github.com/1050TIt0p/tiAuth/releases");
                })
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Update check failed", e);
                    return null;
                });
    }

    private boolean isSupportedVersion() {
        return Integer.parseInt(ProxyServer.getInstance().getVersion().split("-")[0].split("\\.")[1]) >= 21;
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
                .version(MainConfig.IMP.libraries.sqlite.version)
                .build();

        Library h2Jdbc = Library.builder()
                .groupId("com{}h2database")
                .artifactId("h2")
                .version(MainConfig.IMP.libraries.h2.version)
                .build();

        Library mysqlJdbc = Library.builder()
                .groupId("com{}mysql")
                .artifactId("mysql-connector-j")
                .version(MainConfig.IMP.libraries.mysql.version)
                .build();

        Library postgresqlJdbc = Library.builder()
                .groupId("org{}postgresql")
                .artifactId("postgresql")
                .version(MainConfig.IMP.libraries.postgresql.version)
                .build();

        String adventureVersion = "4.26.1";

        Library adventureApi = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-api")
                .version(adventureVersion)
                .build();

        Library adventureMinimessage = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-minimessage")
                .version(adventureVersion)
                .build();

        Library adventureLegacy = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-serializer-legacy")
                .version(adventureVersion)
                .build();

        Library adventureGson = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-serializer-gson")
                .version(adventureVersion)
                .build();

        Library adventureJson = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-serializer-json")
                .version(adventureVersion)
                .build();

        Library adventureJsonLegacyImpl = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-serializer-json-legacy-impl")
                .version(adventureVersion)
                .build();

        Library adventurePlain = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-serializer-plain")
                .version(adventureVersion)
                .build();

        Library adventureNBT = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-nbt")
                .version(adventureVersion)
                .build();

        Library adventureKey = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-key")
                .version(adventureVersion)
                .build();

        Library kyoriExamination = Library.builder()
                .groupId("net.kyori")
                .artifactId("examination-api")
                .version("1.3.0")
                .build();

        Library kyoriOption = Library.builder()
                .groupId("net.kyori")
                .artifactId("option")
                .version("1.1.0")
                .build();

        BungeeLibraryManager libraryManager = new BungeeLibraryManager(this);
        libraryManager.addMavenCentral();
        libraryManager.loadLibrary(sqliteJdbc);
        libraryManager.loadLibrary(h2Jdbc);
        libraryManager.loadLibrary(mysqlJdbc);
        libraryManager.loadLibrary(postgresqlJdbc);
        libraryManager.loadLibrary(adventureApi);
        libraryManager.loadLibrary(adventureMinimessage);
        libraryManager.loadLibrary(adventureLegacy);
        libraryManager.loadLibrary(adventureGson);
        libraryManager.loadLibrary(adventureJson);
        libraryManager.loadLibrary(adventureJsonLegacyImpl);
        libraryManager.loadLibrary(adventurePlain);
        libraryManager.loadLibrary(adventureNBT);
        libraryManager.loadLibrary(adventureKey);
        libraryManager.loadLibrary(kyoriExamination);
        libraryManager.loadLibrary(kyoriOption);
    }

    private void initializeSecretKey(File dataFolder) {
        try {
            secretKey = KeyLoader.loadOrGenerateKey(dataFolder.toPath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error during secret key initialization. Stopping server...", e);
            getProxy().stop();
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
            logger.log(Level.SEVERE, "Error during database initialization. Stopping server...", e);
            getProxy().stop();
        }
    }

    private void startLimboServer(File dataFolder) {
        if (MainConfig.IMP.servers.useVirtualServer) {
            Path limboPath = dataFolder.toPath().resolve("picolimbo");

            if (!Files.exists(limboPath)) {
                try {
                    Files.createDirectories(limboPath);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error when starting the virtual server. Stopping server...", e);
                    getProxy().stop();
                    return;
                }
            }

            Path configFile = limboPath.resolve("config.toml");

            try {
                LibraryLoader.RustLib lib = LibraryLoader.loadOrDownloadLib(limboPath);

                this.worker = new PicoLimboRunner(MainConfig.IMP.servers.virtualServerPort, configFile, lib);

                getProxy().getScheduler().runAsync(this, worker);

                getProxy().getServers().put(
                        MainConfig.IMP.servers.auth,
                        getProxy().constructServerInfo(
                                MainConfig.IMP.servers.auth,
                                new InetSocketAddress("127.0.0.1", MainConfig.IMP.servers.virtualServerPort),
                                "auth server",
                                false
                        )
                );
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error when starting the virtual server. Stopping server...", e);
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
        pluginManager.registerCommand(this, new TotpCommand(this, "2fa", "totp"));
    }
}
