package ru.matveylegenda.tiauth.manager;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;
import ru.matveylegenda.tiauth.util.ChatUtils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static ru.matveylegenda.tiauth.util.ChatUtils.colorize;

public class AuthManager {
    private final TiAuth plugin;
    private final Database database;
    private final AuthCache authCache;
    private final PremiumCache premiumCache;
    private final SessionCache sessionCache;
    private final MainConfig mainConfig;
    private final MessagesConfig messagesConfig;
    private final ChatUtils chatUtils;

    public AuthManager(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.authCache = plugin.getAuthCache();
        this.premiumCache = plugin.getPremiumCache();
        this.sessionCache = plugin.getSessionCache();
        this.mainConfig = plugin.getMainConfig();
        this.messagesConfig = plugin.getMessagesConfig();
        this.chatUtils = plugin.getChatUtils();
    }

    public void registerPlayer(ProxiedPlayer player, String password, String repeatPassword) {
        if (!password.equals(repeatPassword)) {
            chatUtils.sendMessage(
                    player,
                    messagesConfig.register.mismatch
            );

            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), user -> {
            if (user != null) {
                chatUtils.sendMessage(
                        player,
                        messagesConfig.register.alreadyRegistered
                );
                return;
            }

            Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
            database.getAuthUserRepository().registerUser(
                    new AuthUser(
                            player.getName().toLowerCase(Locale.ROOT),
                            player.getName(),
                            hash.hashPassword(password),
                            false,
                            player.getAddress().getAddress().getHostAddress()
                    ), () -> {
                        chatUtils.sendMessage(
                                player,
                                messagesConfig.register.success
                        );
                        authCache.setAuthenticated(player.getName());

                        sessionCache.addPlayer(player.getName(), player.getAddress().getAddress().getHostAddress());
                        connectToBackend(player);
                    }
            );
        });
    }

    public void loginPlayer(ProxiedPlayer player, String password) {
        database.getAuthUserRepository().getUser(player.getName(), user -> {
            if (user == null) {
                chatUtils.sendMessage(
                        player,
                        messagesConfig.login.notRegistered
                );

                return;
            }

            if (authCache.isAuthenticated(player.getName())) {
                chatUtils.sendMessage(
                        player,
                        messagesConfig.login.alreadyLogged
                );

                return;
            }

            Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
            String hashedPassword = user.getPassword();

            if (hash.verifyPassword(password, hashedPassword)) {
                loginPlayer(player);
            } else {
                chatUtils.sendMessage(
                        player,
                        messagesConfig.login.wrongPassword
                );
            }
        });
    }

    public void loginPlayer(ProxiedPlayer player) {
        chatUtils.sendMessage(
                player,
                messagesConfig.login.success
        );

        authCache.setAuthenticated(player.getName());
        sessionCache.addPlayer(player.getName(), player.getAddress().getAddress().getHostAddress());

        connectToBackend(player);
    }

    public void logoutPlayer(ProxiedPlayer player) {
        authCache.logout(player.getName());
        sessionCache.removePlayer(player.getName());
    }

    public void togglePremium(ProxiedPlayer player) {
        if (premiumCache.isPremium(player.getName())) {
            database.getAuthUserRepository().setPremium(player.getName(), false);
            premiumCache.removePremium(player.getName());

            chatUtils.sendMessage(
                    player,
                    messagesConfig.premium.disabled
            );
        } else {
            database.getAuthUserRepository().setPremium(player.getName(), true);
            premiumCache.addPremium(player.getName());

            chatUtils.sendMessage(
                    player,
                    messagesConfig.premium.enabled
            );
        }
    }

    public void forceAuth(ProxiedPlayer player, AuthUser user) {
        if (user != null && !player.getName().equals(user.getRealName())) {
            player.disconnect(
                    colorize(
                            messagesConfig.kick.realname
                                    .replace("{prefix}", messagesConfig.prefix)
                                    .replace("{realname}", user.getRealName())
                                    .replace("{name}", player.getName())
                    )
            );

            return;
        }

        String sessionIP = sessionCache.getIP(player.getName());

        if (premiumCache.isPremium(player.getName()) ||
                (sessionIP != null && sessionIP.equals(player.getAddress().getAddress().getHostAddress()))) {
            authCache.setAuthenticated(player.getName());
            connectToBackend(player);

            return;
        }

        connectToAuthServer(player);

        ScheduledTask[] taskHolder = new ScheduledTask[2];
        taskHolder[0] = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (!player.isConnected() || authCache.isAuthenticated(player.getName())) {
                taskHolder[0].cancel();
                return;
            }

            player.disconnect(
                    colorize(
                            messagesConfig.kick.timeout
                                    .replace("{prefix}", messagesConfig.prefix)
                    )
            );
        }, mainConfig.auth.timeoutSeconds, TimeUnit.SECONDS);

        String reminderMessage = (user != null)
                ? messagesConfig.reminder.login
                : messagesConfig.reminder.register;
        taskHolder[1] = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (!player.isConnected() || authCache.isAuthenticated(player.getName())) {
                taskHolder[1].cancel();
                return;
            }

            chatUtils.sendMessage(
                    player,
                    reminderMessage
            );
        }, 0, mainConfig.auth.reminderInterval, TimeUnit.SECONDS);
    }

    public void connectToAuthServer(ProxiedPlayer player) {
        ServerInfo authServer = plugin.getProxy().getServerInfo(mainConfig.servers.auth);
        player.connect(authServer);
    }

    public void connectToBackend(ProxiedPlayer player) {
        ServerInfo backendServer = plugin.getProxy().getServerInfo(mainConfig.servers.backend);
        player.connect(backendServer);
    }
}
