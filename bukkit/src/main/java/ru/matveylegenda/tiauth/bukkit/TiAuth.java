package ru.matveylegenda.tiauth.bukkit;

import lombok.Getter;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import ru.matveylegenda.tiauth.bukkit.api.TiAuthAPI;
import ru.matveylegenda.tiauth.bukkit.command.admin.TiAuthCommand;
import ru.matveylegenda.tiauth.bukkit.command.player.*;
import ru.matveylegenda.tiauth.bukkit.listener.AuthListener;
import ru.matveylegenda.tiauth.bukkit.listener.ChatListener;
import ru.matveylegenda.tiauth.bukkit.listener.RestrictionListener;
import ru.matveylegenda.tiauth.bukkit.manager.AuthManager;
import ru.matveylegenda.tiauth.bukkit.manager.TaskManager;
import ru.matveylegenda.tiauth.bukkit.storage.CachedMessages;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.util.Utils;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public final class TiAuth extends JavaPlugin {
    public static Logger logger;

    private Database database;
    private TaskManager taskManager;
    private AuthManager authManager;

    @Override
    public void onLoad() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        MainConfig.IMP.reload();
        MessagesConfig.IMP.reload();
        loadLibraries();
    }

    @Override
    public void onEnable() {
        logger = getLogger();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        initializeDatabase();

        Utils.initializeColorizer(MainConfig.IMP.serializer);
        CachedMessages.IMP = new CachedMessages(MessagesConfig.IMP);

        taskManager = new TaskManager(this);
        authManager = new AuthManager(this);

        registerListeners();

        registerCommands();

        new Metrics(this, 26922);
        new TiAuthAPI(this);
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

    public void runSync(Runnable task) {
        if (getServer().isPrimaryThread()) {
            task.run();
            return;
        }

        getServer().getScheduler().runTask(this, task);
    }

    private void loadLibraries() {
        BukkitLibraryManager libraryManager = new BukkitLibraryManager(this);
        libraryManager.addMavenCentral();

        libraryManager.loadLibrary(Library.builder()
                .groupId("org{}xerial")
                .artifactId("sqlite-jdbc")
                .version(MainConfig.IMP.libraries.sqlite.version)
                .build());
        libraryManager.loadLibrary(Library.builder()
                .groupId("com{}h2database")
                .artifactId("h2")
                .version(MainConfig.IMP.libraries.h2.version)
                .build());
        libraryManager.loadLibrary(Library.builder()
                .groupId("com{}mysql")
                .artifactId("mysql-connector-j")
                .version(MainConfig.IMP.libraries.mysql.version)
                .build());
        libraryManager.loadLibrary(Library.builder()
                .groupId("org{}postgresql")
                .artifactId("postgresql")
                .version(MainConfig.IMP.libraries.postgresql.version)
                .build());
    }

    private void initializeDatabase() {
        try {
            File dataFolder = getDataFolder();
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
            logger.log(Level.SEVERE, "Error during database initialization. Disabling plugin...", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerListeners() {
        registerListener(new AuthListener(this));
        registerListener(new ChatListener());
        registerListener(new RestrictionListener());
    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void registerCommands() {
        registerCommand("tiauth", new TiAuthCommand(this));
        registerCommand("login", new LoginCommand(this));
        registerCommand("register", new RegisterCommand(this));
        registerCommand("unregister", new UnregisterCommand(this));
        registerCommand("changepassword", new ChangePasswordCommand(this));
        registerCommand("logout", new LogoutCommand(this));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command " + name + " is missing from plugin.yml");
        }

        command.setExecutor(executor);
    }
}
