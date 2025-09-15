package ru.matveylegenda.tiauth.manager;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.dialog.Dialog;
import net.md_5.bungee.api.dialog.DialogBase;
import net.md_5.bungee.api.dialog.NoticeDialog;
import net.md_5.bungee.api.dialog.action.ActionButton;
import net.md_5.bungee.api.dialog.action.CustomClickAction;
import net.md_5.bungee.api.dialog.body.PlainMessageBody;
import net.md_5.bungee.api.dialog.input.TextInput;
import net.md_5.bungee.api.event.PostLoginEvent;
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
import ru.matveylegenda.tiauth.util.Utils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ru.matveylegenda.tiauth.util.Utils.colorizeComponent;

public class AuthManager {
    private final TiAuth plugin;
    private final Database database;
    private final AuthCache authCache;
    private final PremiumCache premiumCache;
    private final SessionCache sessionCache;
    private final MainConfig mainConfig;
    private final MessagesConfig messagesConfig;
    private final Utils utils;
    private final TaskManager taskManager;

    public AuthManager(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.authCache = plugin.getAuthCache();
        this.premiumCache = plugin.getPremiumCache();
        this.sessionCache = plugin.getSessionCache();
        this.mainConfig = plugin.getMainConfig();
        this.messagesConfig = plugin.getMessagesConfig();
        this.utils = plugin.getUtils();
        this.taskManager = plugin.getTaskManager();
    }

    public void registerPlayer(ProxiedPlayer player, String password, String repeatPassword) {
        if (!password.equals(repeatPassword)) {
            utils.sendMessage(
                    player,
                    messagesConfig.player.register.mismatch
            );

            if (supportDialog(player)) {
                showLoginDialog(player, messagesConfig.player.dialog.notifications.mismatch);
            }

            return;
        }

        if (password.length() < mainConfig.auth.minPasswordLength ||
                password.length() > mainConfig.auth.maxPasswordLength) {
            utils.sendMessage(
                    player,
                    messagesConfig.player.checkPassword.invalidLength
                            .replace("{min}", String.valueOf(mainConfig.auth.minPasswordLength))
                            .replace("{max}", String.valueOf(mainConfig.auth.maxPasswordLength))
            );

            if (supportDialog(player)) {
                showLoginDialog(
                        player,
                        messagesConfig.player.dialog.notifications.invalidLength
                                .replace("{min}", String.valueOf(mainConfig.auth.minPasswordLength))
                                .replace("{max}", String.valueOf(mainConfig.auth.maxPasswordLength))
                );
            }

            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.kickPlayer(
                        player,
                        messagesConfig.queryError
                );
                return;
            }

            if (user != null) {
                utils.sendMessage(
                        player,
                        messagesConfig.player.register.alreadyRegistered
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
                    ), success1 -> {
                        if (!success1) {
                            utils.kickPlayer(
                                    player,
                                    messagesConfig.queryError
                            );
                            return;
                        }

                        utils.sendMessage(
                                player,
                                messagesConfig.player.register.success
                        );
                        authCache.setAuthenticated(player.getName());

                        sessionCache.addPlayer(player.getName(), player.getAddress().getAddress().getHostAddress());
                        connectToBackend(player);
                    }
            );
        });
    }

    public void unregisterPlayer(ProxiedPlayer player, String password) {
        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.sendMessage(
                        player,
                        messagesConfig.queryError
                );
                return;
            }

            Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(password, hashedPassword)) {
                utils.sendMessage(
                        player,
                        messagesConfig.player.checkPassword.wrongPassword
                );
                return;
            }

            unregisterPlayer(player.getName(), success1 -> {
                if (!success1) {
                    utils.sendMessage(
                            player,
                            messagesConfig.queryError
                    );
                    return;
                }

                sessionCache.removePlayer(player.getName());

                utils.kickPlayer(
                        player,
                        messagesConfig.player.unregister.success
                );
            });
        });
    }

    public void unregisterPlayer(String playerName, Consumer<Boolean> callback) {
        database.getAuthUserRepository().deleteUser(playerName, success -> {
            if (!success) {
                callback.accept(false);
                return;
            }

            callback.accept(true);
        });
    }

    public void loginPlayer(ProxiedPlayer player, String password) {
        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.kickPlayer(
                        player,
                        messagesConfig.queryError
                );
                return;
            }

            if (user == null) {
                utils.sendMessage(
                        player,
                        messagesConfig.player.login.notRegistered
                );

                return;
            }

            if (authCache.isAuthenticated(player.getName())) {
                utils.sendMessage(
                        player,
                        messagesConfig.player.login.alreadyLogged
                );

                return;
            }

            Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
            String hashedPassword = user.getPassword();

            if (hash.verifyPassword(password, hashedPassword)) {
                loginPlayer(player, () -> {
                    utils.sendMessage(
                            player,
                            messagesConfig.player.login.success
                    );
                });
            } else {
                utils.sendMessage(
                        player,
                        messagesConfig.player.checkPassword.wrongPassword
                );

                if (supportDialog(player)) {
                    showLoginDialog(player, messagesConfig.player.dialog.notifications.wrongPassword);
                }
            }
        });
    }

    public void loginPlayer(ProxiedPlayer player, Runnable callback) {
        String ip = player.getAddress().getAddress().getHostAddress();

        authCache.setAuthenticated(player.getName());
        database.getAuthUserRepository().updateLastLogin(player.getName());
        database.getAuthUserRepository().updateLastIp(player.getName(), ip);
        sessionCache.addPlayer(player.getName(), ip);

        connectToBackend(player);

        callback.run();
    }

    public void changePasswordPlayer(ProxiedPlayer player, String oldPassword, String newPassword) {
        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.sendMessage(
                        player,
                        messagesConfig.queryError
                );
                return;
            }

            if (newPassword.length() < mainConfig.auth.minPasswordLength ||
                    newPassword.length() > mainConfig.auth.maxPasswordLength) {
                utils.sendMessage(
                        player,
                        messagesConfig.player.checkPassword.invalidLength
                                .replace("{min}", String.valueOf(mainConfig.auth.minPasswordLength))
                                .replace("{max}", String.valueOf(mainConfig.auth.maxPasswordLength))
                );
                return;
            }

            Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(oldPassword, hashedPassword)) {
                utils.sendMessage(
                        player,
                        messagesConfig.player.checkPassword.wrongPassword
                );
                return;
            }

            changePasswordPlayer(player.getName(), newPassword, success1 -> {
                if (!success1) {
                    utils.sendMessage(
                            player,
                            messagesConfig.queryError
                    );
                    return;
                }

                utils.sendMessage(
                        player,
                        messagesConfig.player.changePassword.success
                );
            });
        });
    }

    public void changePasswordPlayer(String playerName, String password, Consumer<Boolean> callback) {
        Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
        String hashedPassword = hash.hashPassword(password);

        database.getAuthUserRepository().updatePassword(playerName, hashedPassword, success -> {
            if (!success) {
                callback.accept(false);
                return;
            }

            callback.accept(true);
        });
    }

    public void logoutPlayer(ProxiedPlayer player) {
        authCache.logout(player.getName());
        sessionCache.removePlayer(player.getName());
    }

    public void togglePremium(ProxiedPlayer player) {
        if (premiumCache.isPremium(player.getName())) {
            database.getAuthUserRepository().setPremium(player.getName(), false, success -> {
                if (!success) {
                    utils.sendMessage(
                            player,
                            messagesConfig.queryError
                    );
                    return;
                }

                premiumCache.removePremium(player.getName());

                utils.sendMessage(
                        player,
                        messagesConfig.player.premium.disabled
                );
            });
        } else {
            database.getAuthUserRepository().setPremium(player.getName(), true, success -> {
                if (!success) {
                    utils.sendMessage(
                            player,
                            messagesConfig.queryError
                    );
                    return;
                }

                premiumCache.addPremium(player.getName());

                utils.sendMessage(
                        player,
                        messagesConfig.player.premium.enabled
                );
            });
        }
    }

    public void forceAuth(ProxiedPlayer player) {
        forceAuth(player, null);
    }

    public void forceAuth(ProxiedPlayer player, PostLoginEvent event) {
        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            try {
                if (!success) {
                    utils.kickPlayer(
                            player,
                            messagesConfig.queryError
                    );
                    return;
                }

                if (user != null && !player.getName().equals(user.getRealName())) {
                    utils.kickPlayer(
                            player,
                            messagesConfig.player.kick.realname
                                    .replace("{realname}", user.getRealName())
                                    .replace("{name}", player.getName())
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

                if (event != null) {
                    connectToAuthServer(event);
                } else {
                    connectToAuthServer(player);
                }

                String reminderMessage = (user != null)
                        ? messagesConfig.player.reminder.login
                        : messagesConfig.player.reminder.register;

                plugin.getProxy().getScheduler().schedule(plugin, () -> {
                    taskManager.startAuthTimeoutTask(player);
                    taskManager.startAuthReminderTask(player, reminderMessage);
                    taskManager.startDisplayTimerTask(player);
                }, 100, TimeUnit.MILLISECONDS);
            } finally {
                if (event != null) {
                    event.completeIntent(plugin);
                }
            }
        });
    }

    public void showLoginDialog(ProxiedPlayer player) {
        showLoginDialog(player, null);
    }

    public void showLoginDialog(ProxiedPlayer player, String noticeMessage) {
        if (!mainConfig.auth.useDialogs) {
            return;
        }

        if (!supportDialog(player)) {
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.kickPlayer(
                        player,
                        messagesConfig.queryError
                );
                return;
            }

            Dialog dialog;
            if (user != null) {
                dialog = new NoticeDialog(new DialogBase(colorizeComponent(messagesConfig.player.dialog.login.title))
                        .inputs(
                                List.of(
                                        new TextInput("password", colorizeComponent(messagesConfig.player.dialog.login.passwordField))
                                )
                        ))
                        .action(
                                new ActionButton(
                                        colorizeComponent(messagesConfig.player.dialog.login.confirmButton),
                                        new CustomClickAction("tiauth_login")
                                )
                        );
            } else {
                dialog = new NoticeDialog(new DialogBase(colorizeComponent(messagesConfig.player.dialog.register.title))
                        .inputs(
                                List.of(
                                        new TextInput("password", colorizeComponent(messagesConfig.player.dialog.register.passwordField)),
                                        new TextInput("repeatPassword", colorizeComponent(messagesConfig.player.dialog.register.repeatPasswordField))
                                )
                        ))
                        .action(
                                new ActionButton(
                                        colorizeComponent(messagesConfig.player.dialog.register.confirmButton),
                                        new CustomClickAction("tiauth_register")
                                )
                        );
            }

            if (noticeMessage != null) {
                dialog.getBase().body(
                        List.of(
                                new PlainMessageBody(colorizeComponent(noticeMessage))
                        )
                );
            }

            plugin.getProxy().getScheduler().schedule(plugin, () -> {
                player.showDialog(dialog);
            }, 50, TimeUnit.MILLISECONDS);
        });
    }

    private void connectToAuthServer(PostLoginEvent event) {
        ServerInfo authServer = plugin.getProxy().getServerInfo(mainConfig.servers.auth);
        event.setTarget(authServer);
    }

    private void connectToAuthServer(ProxiedPlayer player) {
        ServerInfo currentServer = player.getServer().getInfo();
        ServerInfo authServer = plugin.getProxy().getServerInfo(mainConfig.servers.auth);

        if (currentServer == null || !currentServer.equals(authServer)) {
            player.connect(authServer);
        }
    }

    private void connectToBackend(ProxiedPlayer player) {
        ServerInfo currentServer = player.getServer().getInfo();
        ServerInfo backendServer = plugin.getProxy().getServerInfo(mainConfig.servers.backend);

        if (currentServer == null || !currentServer.equals(backendServer)) {
            player.connect(backendServer);
        }
    }

    public boolean supportDialog(ProxiedPlayer player) {
        return player.getPendingConnection().getVersion() >= 771;
    }
}
