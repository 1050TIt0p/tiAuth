package ru.matveylegenda.tiauth.bungee.manager;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.dialog.Dialog;
import net.md_5.bungee.api.dialog.DialogBase;
import net.md_5.bungee.api.dialog.NoticeDialog;
import net.md_5.bungee.api.dialog.action.ActionButton;
import net.md_5.bungee.api.dialog.action.CustomClickAction;
import net.md_5.bungee.api.dialog.body.PlainMessageBody;
import net.md_5.bungee.api.dialog.input.DialogInput;
import net.md_5.bungee.api.dialog.input.TextInput;
import net.md_5.bungee.api.event.PostLoginEvent;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.api.event.PlayerAuthEvent;
import ru.matveylegenda.tiauth.bungee.api.event.PlayerRegisterEvent;
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

import java.net.InetSocketAddress;
import java.util.*;
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
    @Getter
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
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.register.mismatch);
            if (supportDialog(player)) {
                showLoginDialog(player, CachedMessages.IMP.player.dialog.notifications.mismatch);
            }
            return;
        }

        if (checkPasswordEmpty(player, password) ||
                checkPasswordLength(player, password) ||
                checkPasswordPattern(player, password)) {
            return;
        }

        if (!beginProcess(player)) {
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                endProcess(player);
                return;
            }

            if (user != null) {
                BungeeUtils.sendMessage(player, CachedMessages.IMP.player.register.alreadyRegistered);
                endProcess(player);
                return;
            }

            completeRegistration(player, player.getName(), password);
        });
    }

    private void completeRegistration(ProxiedPlayer player, String name, String password) {
        String ip = ((InetSocketAddress) player.getSocketAddress()).getAddress().getHostAddress();

        registerPlayer(name, password, ip, success1 -> {
            if (!success1) {
                player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                endProcess(player);
                return;
            }

            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.register.success);
            AuthCache.setAuthenticated(name);
            SessionCache.addPlayer(name, ip);
            taskManager.cancelTasks(player);

            PlayerRegisterEvent playerRegisterEvent = new PlayerRegisterEvent(player);
            plugin.getProxy().getPluginManager().callEvent(playerRegisterEvent);

            if (playerRegisterEvent.isMoveToBackendServer()) {
                connectToBackend(player);
            }

            endProcess(player);
        });
    }

    public void registerPlayer(String playerName, String password, String ip, Consumer<Boolean> callback) {

        database.getAuthUserRepository().registerUser(
                new AuthUser(
                        playerName.toLowerCase(Locale.ROOT),
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

        if (checkPasswordLength(player, password)) {
            endProcess(player);
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                endProcess(player);
                return;
            }

            if (!hash.verifyPassword(password, user.getPassword())) {
                BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.wrongPassword);
                endProcess(player);
                return;
            }

            unregisterPlayer(player.getName(), success1 -> {
                if (!success1) {
                    BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                    endProcess(player);
                    return;
                }

                SessionCache.removePlayer(player.getName());
                player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.player.unregister.success));
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
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.alreadyLogged);
            return;
        }

        if (plugin.getTotpManager().isTotpPending(player.getName())) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.prompt);
            return;
        }

        if (checkPasswordEmpty(player, password)) {
            return;
        }

        if (!beginProcess(player)) {
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                endProcess(player);
                return;
            }

            if (user == null) {
                BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.notRegistered);
                endProcess(player);
                return;
            }

            if (!hash.verifyPassword(password, user.getPassword())) {
                handleWrongPasswordAttempt(player, player.getName());
                return;
            }

            if (plugin.getTotpManager().isTotpLoginRequired(player, user)) {
                endProcess(player);
                return;
            }

            processSuccessfulLogin(player, player.getName());
        });
    }

    private void handleWrongPasswordAttempt(ProxiedPlayer player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        int attempts = loginAttempts.merge(lowerName, 1, Integer::sum);

        if (attempts >= MainConfig.IMP.auth.loginAttempts) {
            player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.tooManyAttempts));
            if (MainConfig.IMP.auth.banPlayer) {
                BanCache.addPlayer(((InetSocketAddress) player.getSocketAddress()).getAddress().getHostAddress());
            }
            loginAttempts.remove(lowerName);
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
    }

    private void processSuccessfulLogin(ProxiedPlayer player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        loginPlayer(player, () -> {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.success);

            if (MainConfig.IMP.title.enabledOnAuth) {
                Title title = ProxyServer.getInstance().createTitle();
                title.title(TextComponent.fromLegacy(CachedMessages.IMP.player.title.onAuthTitle));
                title.subTitle(TextComponent.fromLegacy(CachedMessages.IMP.player.title.onAuthSubTitle));
                title.fadeIn(0);
                title.stay(21);
                title.fadeOut(0);
                player.sendTitle(title);
            }

            loginAttempts.remove(lowerName);
            endProcess(player);
        });
    }

    public void loginPlayer(ProxiedPlayer player, Runnable callback) {
        loginPlayer(player, callback, false);
    }

    public void loginPlayer(ProxiedPlayer player, Runnable callback, boolean forceLogin) {
        String name = player.getName();
        String ip = ((InetSocketAddress) player.getSocketAddress()).getAddress().getHostAddress();

        AuthCache.setAuthenticated(name);
        database.getAuthUserRepository().updateLastLogin(name);
        database.getAuthUserRepository().updateLastIp(name, ip);
        SessionCache.addPlayer(name, ip);
        taskManager.cancelTasks(player);

        PlayerAuthEvent playerAuthEvent = new PlayerAuthEvent(player, forceLogin);
        plugin.getProxy().getPluginManager().callEvent(playerAuthEvent);

        if (playerAuthEvent.isMoveToBackendServer()) {
            connectToBackend(player);
        }

        callback.run();
    }

    public void changePasswordPlayer(ProxiedPlayer player, String oldPassword, String newPassword) {
        if (checkPasswordEmpty(player, oldPassword) || checkPasswordEmpty(player, newPassword)) {
            return;
        }

        if (checkPasswordLength(player, oldPassword) || checkPasswordLength(player, newPassword)) {
            return;
        }

        if (checkPasswordPattern(player, newPassword)) {
            return;
        }

        if (!beginProcess(player)) {
            return;
        }

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            if (!success) {
                BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                endProcess(player);
                return;
            }

            if (!hash.verifyPassword(oldPassword, user.getPassword())) {
                BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.wrongPassword);
                endProcess(player);
                return;
            }

            changePasswordPlayer(player.getName(), newPassword, success1 -> {
                if (!success1) {
                    BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                    endProcess(player);
                    return;
                }

                BungeeUtils.sendMessage(player, CachedMessages.IMP.player.changePassword.success);
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

    public void clearLoginAttempts(String lowerName) {
        loginAttempts.remove(lowerName);
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

    public void forceAuth(ProxiedPlayer player, PostLoginEvent event) {
        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> {
            try {
                if (!success) {
                    player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                    return;
                }

                if (user != null && !player.getName().equals(user.getRealName())) {
                    player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.realname
                            .replace("{realname}", user.getRealName())
                            .replace("{name}", player.getName()))
                    );

                    return;
                }

                String sessionIP = SessionCache.getIP(player.getName());

                if (PremiumCache.isPremium(player.getName()) ||
                        (sessionIP != null && sessionIP.equals(((InetSocketAddress) player.getSocketAddress()).getAddress().getHostAddress()))) {
                    AuthCache.setAuthenticated(player.getName());

                    if (event != null) {
                        connectToBackend(event);
                    } else {
                        connectToBackend(player);
                    }

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
                player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
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
                List<DialogInput> inputList = new ArrayList<>();

                inputList.add(new TextInput("password", TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.register.passwordField)));
                if (MainConfig.IMP.auth.repeatPasswordWhenRegister) {
                    inputList.add(new TextInput("repeatPassword", TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.register.repeatPasswordField)));
                }
                dialog = new NoticeDialog(new DialogBase(TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.register.title))
                        .inputs(
                                inputList
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

            plugin.getProxy().getScheduler().schedule(plugin, () -> {
                if (player.isConnected()) {
                    player.showDialog(dialog);
                }
            }, 50, TimeUnit.MILLISECONDS);
        });
    }

    private void connectToAuthServer(PostLoginEvent event) {
        ServerInfo authServer = plugin.getProxy().getServerInfo(MainConfig.IMP.servers.auth);
        event.setTarget(authServer);
    }

    private void connectToAuthServer(ProxiedPlayer player) {
        ServerInfo authServer = plugin.getProxy().getServerInfo(MainConfig.IMP.servers.auth);
        connect(player, authServer);
    }

    private void connectToBackend(PostLoginEvent event) {
        String targetName = Optional.ofNullable(event.getPlayer().getPendingConnection().getVirtualHost())
                .flatMap(this::getForcedHost)
                .orElse(MainConfig.IMP.servers.backend);
        ServerInfo backendServer = plugin.getProxy().getServerInfo(targetName);
        event.setTarget(backendServer);
    }

    private void connectToBackend(ProxiedPlayer player) {
        String targetName = Optional.ofNullable(player.getPendingConnection().getVirtualHost())
                .flatMap(this::getForcedHost)
                .orElse(MainConfig.IMP.servers.backend);
        ServerInfo targetServer = plugin.getProxy().getServerInfo(targetName);
        connect(player, targetServer);
    }

    private void connect(ProxiedPlayer player, ServerInfo target) {
        ServerInfo currentServer = player.getServer().getInfo();
        if (currentServer == null || !currentServer.equals(target)) {
            player.connect(target);
        }
    }

    private Optional<String> getForcedHost(InetSocketAddress virtualHost) {
        return Optional.ofNullable(MainConfig.IMP.servers.forcedHosts.get(virtualHost.getHostString().toLowerCase()));
    }

    private boolean supportDialog(ProxiedPlayer player) {
        return player.getPendingConnection().getVersion() >= 771;
    }

    private boolean beginProcess(ProxiedPlayer player) {
        return inProcess.add(player.getName());
    }

    private void endProcess(ProxiedPlayer player) {
        inProcess.remove(player.getName());
    }

    private boolean checkPasswordEmpty(ProxiedPlayer player, String password) {
        if (password.isEmpty()) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.passwordEmpty);
            if (supportDialog(player)) {
                showLoginDialog(player, CachedMessages.IMP.player.dialog.notifications.passwordEmpty);
            }
            return true;
        }
        return false;
    }

    private boolean checkPasswordLength(ProxiedPlayer player, String password) {
        if (password.length() < MainConfig.IMP.auth.minPasswordLength ||
                password.length() > MainConfig.IMP.auth.maxPasswordLength) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.invalidLength
                            .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                            .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength))
            );
            if (supportDialog(player)) {
                showLoginDialog(player,
                        CachedMessages.IMP.player.dialog.notifications.invalidLength
                                .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                                .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength))
                );
            }
            return true;
        }
        return false;
    }

    private boolean checkPasswordPattern(ProxiedPlayer player, String password) {
        if (!passwordPattern.matcher(password).matches()) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.invalidPattern);
            if (supportDialog(player)) {
                showLoginDialog(player, CachedMessages.IMP.player.dialog.notifications.invalidPattern);
            }
            return true;
        }
        return false;
    }
}
