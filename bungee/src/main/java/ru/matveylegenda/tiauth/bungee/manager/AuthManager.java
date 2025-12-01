package ru.matveylegenda.tiauth.bungee.manager;

import lombok.Setter;
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
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class AuthManager {
    private final Set<String> inProcess = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final TiAuth plugin;
    private final Database database;
    private final TaskManager taskManager;

    @Setter
    private Pattern passwordPattern;
    @Setter
    private Hash hash;

    public AuthManager(TiAuth plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.taskManager = plugin.getTaskManager();
        this.passwordPattern = Pattern.compile(MainConfig.IMP.auth.passwordPattern);
        this.hash = HashFactory.create(MainConfig.IMP.auth.hashAlgorithm);
    }

    public void registerPlayer(ProxiedPlayer player, String password, String repeatPassword) {
        if (!password.equals(repeatPassword)) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.register.mismatch
            );

            if (supportDialog(player)) {
                showLoginDialog(
                        player, CachedMessages.IMP.player.dialog.notifications.mismatch
                );
            }

            return;
        }

        if (password.isEmpty()) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.passwordEmpty
            );

            if (supportDialog(player)) {
                showLoginDialog(
                        player,
                        CachedMessages.IMP.player.dialog.notifications.passwordEmpty
                );
            }

            return;
        }

        if (password.length() < MainConfig.IMP.auth.minPasswordLength ||
                password.length() > MainConfig.IMP.auth.maxPasswordLength) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.invalidLength
                            .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                            .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength))
            );

            if (supportDialog(player)) {
                showLoginDialog(
                        player,
                        CachedMessages.IMP.player.dialog.notifications.invalidLength
                                .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                                .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength))
                );
            }

            return;
        }

        if (!passwordPattern.matcher(password).matches()) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.invalidPattern
            );

            if (supportDialog(player)) {
                showLoginDialog(
                        player,
                        CachedMessages.IMP.player.dialog.notifications.invalidPattern
                );
            }

            return;
        }

        if (!beginProcess(player)) {
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                player.disconnect(CachedMessages.IMP.queryError);
                endProcess(player);
                return;
            }

            if (user != null) {
                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.player.register.alreadyRegistered
                );
                endProcess(player);
                return;
            }

            registerPlayer(player.getName(), password, player.getAddress().getAddress().getHostAddress(), success1 -> {
                if (!success1) {
                    player.disconnect(CachedMessages.IMP.queryError);
                    endProcess(player);
                    return;
                }

                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.player.register.success
                );
                AuthCache.setAuthenticated(player.getName());

                SessionCache.addPlayer(player.getName(), player.getAddress().getAddress().getHostAddress());
                taskManager.cancelTasks(player);

                connectToBackend(player);

                endProcess(player);
            });
        });
    }

    public void registerPlayer(String playerName, String password, String ip, Consumer<Boolean> callback) {

        database.getAuthUserRepository().registerUser(
                new AuthUser(
                        playerName.toLowerCase(),
                        playerName,
                        hash.hashPassword(password),
                        false,
                        ip
                ), success -> {
                    if (!success) {
                        callback.accept(false);
                        return;
                    }
                    callback.accept(true);
                }
        );
    }

    public void unregisterPlayer(ProxiedPlayer player, String password) {
        if (!beginProcess(player)) {
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.queryError
                );
                endProcess(player);
                return;
            }


            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(password, hashedPassword)) {
                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.player.checkPassword.wrongPassword
                );
                endProcess(player);
                return;
            }

            unregisterPlayer(player.getName(), success1 -> {
                if (!success1) {
                    BungeeUtils.sendMessage(
                            player,
                            CachedMessages.IMP.queryError
                    );
                    endProcess(player);
                    return;
                }

                SessionCache.removePlayer(player.getName());

                player.disconnect(CachedMessages.IMP.player.unregister.success);

                endProcess(player);
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
        if (AuthCache.isAuthenticated(player.getName())) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.login.alreadyLogged
            );
            return;
        }

        if (password.isEmpty()) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.passwordEmpty
            );

            if (supportDialog(player)) {
                showLoginDialog(
                        player,
                        CachedMessages.IMP.player.dialog.notifications.passwordEmpty
                );
            }

            return;
        }

        if (!beginProcess(player)) {
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                player.disconnect(CachedMessages.IMP.queryError);
                endProcess(player);
                return;
            }

            if (user == null) {
                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.player.login.notRegistered
                );
                endProcess(player);
                return;
            }


            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(password, hashedPassword)) {
                int attempts = loginAttempts.merge(player.getName(), 1, Integer::sum);

                if (attempts >= MainConfig.IMP.auth.loginAttempts) {
                    player.disconnect(CachedMessages.IMP.player.kick.tooManyAttempts);

                    if (MainConfig.IMP.auth.banPlayer) {
                        BanCache.addPlayer(player.getAddress().getAddress().getHostAddress());
                    }

                    loginAttempts.remove(player.getName());
                    endProcess(player);
                    return;
                }

                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.player.login.wrongPassword
                                .replace("{attempts}", String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts))
                );

                if (supportDialog(player)) {
                    showLoginDialog(
                            player,
                            CachedMessages.IMP.player.dialog.notifications.wrongPassword
                                    .replace("{attempts}", String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts))
                    );
                }
                endProcess(player);
                return;
            }

            loginPlayer(player, () -> {
                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.player.login.success
                );

                loginAttempts.remove(player.getName());
                endProcess(player);
            });
        });
    }

    public void loginPlayer(ProxiedPlayer player, Runnable callback) {
        String ip = player.getAddress().getAddress().getHostAddress();

        AuthCache.setAuthenticated(player.getName());
        database.getAuthUserRepository().updateLastLogin(player.getName());
        database.getAuthUserRepository().updateLastIp(player.getName(), ip);
        SessionCache.addPlayer(player.getName(), ip);
        taskManager.cancelTasks(player);

        connectToBackend(player);

        callback.run();
    }

    public void changePasswordPlayer(ProxiedPlayer player, String oldPassword, String newPassword) {
        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.passwordEmpty
            );

            if (supportDialog(player)) {
                showLoginDialog(
                        player,
                        CachedMessages.IMP.player.dialog.notifications.passwordEmpty
                );
            }

            return;
        }

        if (newPassword.length() < MainConfig.IMP.auth.minPasswordLength || newPassword.length() > MainConfig.IMP.auth.maxPasswordLength) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.invalidLength
                            .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                            .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength))
            );

            if (supportDialog(player)) {
                showLoginDialog(
                        player,
                        CachedMessages.IMP.player.dialog.notifications.invalidLength
                                .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                                .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength))
                );
            }

            return;
        }

        if (!passwordPattern.matcher(newPassword).matches()) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.invalidPattern
            );

            if (supportDialog(player)) {
                showLoginDialog(
                        player,
                        CachedMessages.IMP.player.dialog.notifications.invalidPattern
                );
            }

            return;
        }

        if (!beginProcess(player)) {
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.queryError
                );
                endProcess(player);
                return;
            }


            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(oldPassword, hashedPassword)) {
                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.player.checkPassword.wrongPassword
                );
                endProcess(player);
                return;
            }

            changePasswordPlayer(player.getName(), newPassword, success1 -> {
                if (!success1) {
                    BungeeUtils.sendMessage(
                            player,
                            CachedMessages.IMP.queryError
                    );
                    endProcess(player);
                    return;
                }

                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.player.changePassword.success
                );

                endProcess(player);
            });
        });
    }

    public void changePasswordPlayer(String playerName, String password, Consumer<Boolean> callback) {

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
        taskManager.cancelTasks(player);
        AuthCache.logout(player.getName());
        SessionCache.removePlayer(player.getName());
    }

    public void togglePremium(ProxiedPlayer player) {
        if (!beginProcess(player)) {
            return;
        }

        boolean isPremium = PremiumCache.isPremium(player.getName());

        database.getAuthUserRepository().setPremium(player.getName(), !isPremium, success -> {
            if (!success) {
                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.queryError
                );
                endProcess(player);
                return;
            }

            if (isPremium) {
                PremiumCache.removePremium(player.getName());

                BungeeUtils.sendMessage(
                        player,
                        CachedMessages.IMP.player.premium.disabled
                );
                endProcess(player);
                return;
            }

            PremiumCache.addPremium(player.getName());

            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.premium.enabled
            );

            endProcess(player);
        });
    }

    public void forceAuth(ProxiedPlayer player) {
        forceAuth(player, null);
    }

    public void forceAuth(ProxiedPlayer player, PostLoginEvent event) {
        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            try {
                if (!success) {
                    player.disconnect(CachedMessages.IMP.queryError);
                    return;
                }

                if (user != null && !player.getName().equals(user.getRealName())) {
                    player.disconnect(CachedMessages.IMP.player.kick.realname
                            .replace("{realname}", user.getRealName())
                            .replace("{name}", player.getName())
                    );

                    return;
                }

                String sessionIP = SessionCache.getIP(player.getName());

                if (PremiumCache.isPremium(player.getName()) ||
                        (sessionIP != null && sessionIP.equals(player.getAddress().getAddress().getHostAddress()))) {
                    AuthCache.setAuthenticated(player.getName());
                    connectToBackend(player);

                    return;
                }

                if (event != null) {
                    connectToAuthServer(event);
                } else {
                    connectToAuthServer(player);
                }

                String reminderMessage = (user != null)
                        ? CachedMessages.IMP.player.reminder.login
                        : CachedMessages.IMP.player.reminder.register;

                taskManager.startAuthTimeoutTask(player);
                taskManager.startAuthReminderTask(player, reminderMessage);
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
        if (!MainConfig.IMP.auth.useDialogs) {
            return;
        }

        if (!supportDialog(player)) {
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                player.disconnect(CachedMessages.IMP.queryError);
                return;
            }

            Dialog dialog;
            if (user != null) {
                dialog = new NoticeDialog(new DialogBase(TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.login.title))
                        .inputs(
                                List.of(
                                        new TextInput("password", TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.login.passwordField))
                                )
                        ))
                        .action(
                                new ActionButton(
                                        TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.login.confirmButton),
                                        new CustomClickAction("tiauth_login")
                                )
                        );
            } else {
                dialog = new NoticeDialog(new DialogBase(TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.register.title))
                        .inputs(
                                List.of(
                                        new TextInput("password", TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.register.passwordField)),
                                        new TextInput("repeatPassword", TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.register.repeatPasswordField))
                                )
                        ))
                        .action(
                                new ActionButton(
                                        TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.register.confirmButton),
                                        new CustomClickAction("tiauth_register")
                                )
                        );
            }

            if (noticeMessage != null) {
                dialog.getBase().body(
                        List.of(
                                new PlainMessageBody(TextComponent.fromLegacy(noticeMessage))
                        )
                );
            }

            plugin.getProxy().getScheduler().schedule(plugin, () -> player.showDialog(dialog), 50, TimeUnit.MILLISECONDS);
        });
    }

    private void connectToAuthServer(PostLoginEvent event) {
        ServerInfo authServer = plugin.getProxy().getServerInfo(MainConfig.IMP.servers.auth);
        event.setTarget(authServer);
    }

    private void connectToAuthServer(ProxiedPlayer player) {
        ServerInfo currentServer = player.getServer().getInfo();
        ServerInfo authServer = plugin.getProxy().getServerInfo(MainConfig.IMP.servers.auth);

        if (currentServer == null || !currentServer.equals(authServer)) {
            player.connect(authServer);
        }
    }

    private void connectToBackend(ProxiedPlayer player) {
        System.out.println("[DEBUG] Start connecting to backend server");
        ServerInfo currentServer = player.getServer().getInfo();
        ServerInfo backendServer = plugin.getProxy().getServerInfo(MainConfig.IMP.servers.backend);

        if (currentServer == null || !currentServer.equals(backendServer)) {
            System.out.println("[DEBUG] Connecting to backend server...");
            player.connect(backendServer);
        }
    }

    private boolean supportDialog(ProxiedPlayer player) {
        return player.getPendingConnection().getVersion() >= 771;
    }

    private boolean beginProcess(ProxiedPlayer player) {
        if (!inProcess.add(player.getName())) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.processing);
            return false;
        }

        return true;
    }

    private void endProcess(ProxiedPlayer player) {
        inProcess.remove(player.getName());
    }
}
