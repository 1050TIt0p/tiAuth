package ru.matveylegenda.tiauth.manager;

import net.md_5.bungee.api.chat.TextComponent;
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
import ru.matveylegenda.tiauth.util.Utils;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static ru.matveylegenda.tiauth.util.Utils.colorize;
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

    public AuthManager(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.authCache = plugin.getAuthCache();
        this.premiumCache = plugin.getPremiumCache();
        this.sessionCache = plugin.getSessionCache();
        this.mainConfig = plugin.getMainConfig();
        this.messagesConfig = plugin.getMessagesConfig();
        this.utils = plugin.getUtils();
    }

    public void registerPlayer(ProxiedPlayer player, String password, String repeatPassword) {
        if (!password.equals(repeatPassword)) {
            utils.sendMessage(
                    player,
                    messagesConfig.register.mismatch
            );

            return;
        }

        if (password.length() < mainConfig.auth.minPasswordLength ||
                password.length() > mainConfig.auth.maxPasswordLength) {
            utils.sendMessage(
                    player,
                    messagesConfig.checkPassword.invalidLength
                            .replace("{min}", String.valueOf(mainConfig.auth.minPasswordLength))
                            .replace("{max}", String.valueOf(mainConfig.auth.maxPasswordLength))
            );
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.kickPlayer(
                        player,
                        messagesConfig.database.queryError
                );
                return;
            }

            if (user != null) {
                utils.sendMessage(
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
                    ), success1 -> {
                        if (!success1) {
                            utils.kickPlayer(
                                    player,
                                    messagesConfig.database.queryError
                            );
                            return;
                        }

                        utils.sendMessage(
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

    public void unregisterPlayer(ProxiedPlayer player, String password) {
        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.sendMessage(
                        player,
                        messagesConfig.database.queryError
                );
                return;
            }

            Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(password, hashedPassword)) {
                utils.sendMessage(
                        player,
                        messagesConfig.checkPassword.wrongPassword
                );
                return;
            }

            unregisterPlayer(player);
        });
    }

    public void unregisterPlayer(ProxiedPlayer player) {
        database.getAuthUserRepository().deleteUser(player.getName(), success -> {
            if (!success) {
                utils.sendMessage(
                        player,
                        messagesConfig.database.queryError
                );
                return;
            }

            sessionCache.removePlayer(player.getName());

            utils.kickPlayer(
                    player,
                    messagesConfig.unregister.success
            );
        });
    }

    public void loginPlayer(ProxiedPlayer player, String password) {
        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.kickPlayer(
                        player,
                        messagesConfig.database.queryError
                );
                return;
            }

            if (user == null) {
                utils.sendMessage(
                        player,
                        messagesConfig.login.notRegistered
                );

                return;
            }

            if (authCache.isAuthenticated(player.getName())) {
                utils.sendMessage(
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
                utils.sendMessage(
                        player,
                        messagesConfig.checkPassword.wrongPassword
                );
            }
        });
    }

    public void loginPlayer(ProxiedPlayer player) {
        utils.sendMessage(
                player,
                messagesConfig.login.success
        );

        String ip = player.getAddress().getAddress().getHostAddress();

        authCache.setAuthenticated(player.getName());
        database.getAuthUserRepository().updateLastLogin(player.getName());
        database.getAuthUserRepository().updateLastIp(player.getName(), ip);
        sessionCache.addPlayer(player.getName(), ip);

        connectToBackend(player);
    }

    public void changePasswordPlayer(ProxiedPlayer player, String oldPassword, String newPassword) {
        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.sendMessage(
                        player,
                        messagesConfig.database.queryError
                );
                return;
            }

            Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(oldPassword, hashedPassword)) {
                utils.sendMessage(
                        player,
                        messagesConfig.checkPassword.wrongPassword
                );
                return;
            }

            changePasswordPlayer(player, newPassword);
        });
    }

    public void changePasswordPlayer(ProxiedPlayer player, String password) {
        if (password.length() < mainConfig.auth.minPasswordLength ||
                password.length() > mainConfig.auth.maxPasswordLength) {
            utils.sendMessage(
                    player,
                    messagesConfig.checkPassword.invalidLength
                            .replace("{min}", String.valueOf(mainConfig.auth.minPasswordLength))
                            .replace("{max}", String.valueOf(mainConfig.auth.maxPasswordLength))
            );
            return;
        }

        Hash hash = HashFactory.create(mainConfig.auth.hashAlgorithm);
        String hashedPassword = hash.hashPassword(password);

        database.getAuthUserRepository().updatePassword(player.getName(), hashedPassword, success -> {
            if (!success) {
                utils.sendMessage(
                        player,
                        messagesConfig.database.queryError
                );
                return;
            }

            utils.sendMessage(
                    player,
                    messagesConfig.changePassword.success
            );
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
                            messagesConfig.database.queryError
                    );
                    return;
                }

                premiumCache.removePremium(player.getName());

                utils.sendMessage(
                        player,
                        messagesConfig.premium.disabled
                );
            });
        } else {
            database.getAuthUserRepository().setPremium(player.getName(), true, success -> {
                if (!success) {
                    utils.sendMessage(
                            player,
                            messagesConfig.database.queryError
                    );
                    return;
                }

                premiumCache.addPremium(player.getName());

                utils.sendMessage(
                        player,
                        messagesConfig.premium.enabled
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
                            messagesConfig.database.queryError
                    );
                    return;
                }

                if (user != null && !player.getName().equals(user.getRealName())) {
                    utils.kickPlayer(
                            player,
                            messagesConfig.kick.realname
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

                ScheduledTask[] taskHolder = new ScheduledTask[2];
                taskHolder[0] = plugin.getProxy().getScheduler().schedule(plugin, () -> {
                    if (!player.isConnected() || authCache.isAuthenticated(player.getName())) {
                        taskHolder[0].cancel();
                        return;
                    }

                    utils.kickPlayer(
                            player,
                            messagesConfig.kick.timeout
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

                    utils.sendMessage(
                            player,
                            reminderMessage
                    );
                }, 0, mainConfig.auth.reminderInterval, TimeUnit.SECONDS);
            } finally {
                if (event != null) {
                    event.completeIntent(plugin);
                }
            }
        });
    }

    public void showLoginDialog(ProxiedPlayer player) {
        if (!mainConfig.auth.useDialogs) {
            return;
        }

        if (player.getPendingConnection().getVersion() < 771) {
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                utils.kickPlayer(
                        player,
                        messagesConfig.database.queryError
                );
                return;
            }

            Dialog dialog;
            if (user != null) {
                dialog = new NoticeDialog(new DialogBase(colorizeComponent(messagesConfig.dialog.login.title))
                        .inputs(
                                List.of(
                                        new TextInput("password", colorizeComponent(messagesConfig.dialog.login.passwordField))
                                )
                        ))
                        .action(
                                new ActionButton(
                                        colorizeComponent(messagesConfig.dialog.login.confirmButton),
                                        new CustomClickAction("tiauth_login")
                                )
                        );
            } else {
                dialog = new NoticeDialog(new DialogBase(colorizeComponent(messagesConfig.dialog.register.title))
                        .inputs(
                                List.of(
                                        new TextInput("password", colorizeComponent(messagesConfig.dialog.register.passwordField)),
                                        new TextInput("repeatPassword", colorizeComponent(messagesConfig.dialog.register.repeatPasswordField))
                                )
                        ))
                        .action(
                                new ActionButton(
                                        colorizeComponent(messagesConfig.dialog.register.confirmButton),
                                        new CustomClickAction("tiauth_register")
                                )
                        );
            }

            plugin.getProxy().getScheduler().schedule(plugin, () -> {
                player.showDialog(dialog);
            }, 50, TimeUnit.MILLISECONDS);
        });
    }

    public void connectToAuthServer(PostLoginEvent event) {
        ServerInfo authServer = plugin.getProxy().getServerInfo(mainConfig.servers.auth);
        event.setTarget(authServer);
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
