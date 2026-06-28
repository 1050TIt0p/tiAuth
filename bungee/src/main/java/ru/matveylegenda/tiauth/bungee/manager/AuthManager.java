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
import net.md_5.bungee.api.dialog.MultiActionDialog;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
        String name = player.getName();

        if (!password.equals(repeatPassword)) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.register.mismatch);
            if (supportsDialog(player)) {
                showLoginDialog(player, CachedMessages.IMP.player.dialog.notifications.mismatch);
            }
            return;
        }

        if (rejectEmptyPassword(player, password) ||
                rejectInvalidPasswordLength(player, password) ||
                rejectInvalidPasswordPattern(player, password)) {
            return;
        }

        if (!inProcess.add(player.getName())) {
            return;
        }

        getUserAsync(name)
                .thenCompose(user -> {
                    if (user != null) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.register.alreadyRegistered);
                        return CompletableFuture.completedFuture(null);
                    }

                    return completeRegistrationAsync(player, name, password);
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                    }
                    inProcess.remove(player.getName());
                });
    }

    public void registerPlayer(String playerName, String password, String ip, Consumer<Boolean> callback) {
        registerUserAsync(playerName, password, ip)
                .thenAccept(result -> callback.accept(true))
                .exceptionally(throwable -> {
                    callback.accept(false);
                    return null;
                });
    }

    public void unregisterPlayer(ProxiedPlayer player, String password) {
        String name = player.getName();

        if (!inProcess.add(player.getName())) {
            return;
        }

        if (rejectInvalidPasswordLength(player, password)) {
            inProcess.remove(player.getName());
            return;
        }

        getUserAsync(name)
                .thenCompose(user -> {
                    if (!hash.verifyPassword(password, user.getPassword())) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.wrongPassword);
                        return CompletableFuture.completedFuture(null);
                    }

                    return deleteUserAsync(name)
                            .thenAccept(result -> {
                                SessionCache.removePlayer(name);
                                player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.player.unregister.success));
                            });
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                    }
                    inProcess.remove(player.getName());
                });
    }

    public void unregisterPlayer(String playerName, Consumer<Boolean> callback) {
        deleteUserAsync(playerName)
                .thenAccept(result -> callback.accept(true))
                .exceptionally(throwable -> {
                    callback.accept(false);
                    return null;
                });
    }

    public void loginPlayer(ProxiedPlayer player, String password) {
        String name = player.getName();

        if (AuthCache.isAuthenticated(name)) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.alreadyLogged);
            return;
        }

        if (plugin.getTotpManager().isTotpPending(name)) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.prompt);
            return;
        }

        if (rejectEmptyPassword(player, password)) {
            return;
        }

        if (!inProcess.add(player.getName())) {
            return;
        }

        getUserAsync(name)
                .thenCompose(user -> {
                    if (user == null) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.notRegistered);
                        return CompletableFuture.completedFuture(null);
                    }

                    if (!hash.verifyPassword(password, user.getPassword())) {
                        handleWrongPasswordAttempt(player, name);
                        return CompletableFuture.completedFuture(null);
                    }

                    if (plugin.getTotpManager().requireTotpChallenge(player, user)) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return processSuccessfulLoginAsync(player, name);
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                    }
                    inProcess.remove(player.getName());
                });
    }

    public void loginPlayer(ProxiedPlayer player, Runnable callback) {
        loginPlayer(player, callback, false);
    }

    public void loginPlayer(ProxiedPlayer player, Runnable callback, boolean forceLogin) {
        String name = player.getName();
        authenticatePlayer(player, name, forceLogin);
        callback.run();
    }

    public void changePasswordPlayer(ProxiedPlayer player, String oldPassword, String newPassword) {
        String name = player.getName();

        if (rejectEmptyPassword(player, oldPassword) || rejectEmptyPassword(player, newPassword)) {
            return;
        }

        if (rejectInvalidPasswordLength(player, oldPassword) || rejectInvalidPasswordLength(player, newPassword)) {
            return;
        }

        if (rejectInvalidPasswordPattern(player, newPassword)) {
            return;
        }

        if (!inProcess.add(player.getName())) {
            return;
        }

        getUserAsync(name)
                .thenCompose(user -> {
                    if (!hash.verifyPassword(oldPassword, user.getPassword())) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.wrongPassword);
                        return CompletableFuture.completedFuture(null);
                    }

                    return updatePasswordAsync(name, newPassword)
                            .thenAccept(result -> {
                                BungeeUtils.sendMessage(player, CachedMessages.IMP.player.changePassword.success);
                            });
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                    }
                    inProcess.remove(player.getName());
                });
    }

    public void changePasswordPlayer(String playerName, String password, Consumer<Boolean> callback) {
        updatePasswordAsync(playerName, password)
                .thenAccept(result -> callback.accept(true))
                .exceptionally(throwable -> {
                    callback.accept(false);
                    return null;
                });
    }

    public void logoutPlayer(ProxiedPlayer player) {
        taskManager.cancelTasks(player);
        AuthCache.logout(player.getName());
        SessionCache.removePlayer(player.getName());
    }

    public void resetLoginAttempts(String lowerName) {
        loginAttempts.remove(lowerName);
    }

    public void togglePremium(ProxiedPlayer player) {
        String name = player.getName();

        if (!inProcess.add(player.getName())) {
            return;
        }

        boolean isPremium = PremiumCache.isPremium(name);

        setPremiumAsync(name, !isPremium)
                .thenAccept(result -> {
                    if (isPremium) {
                        PremiumCache.removePremium(name);
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.premium.disabled);
                    } else {
                        PremiumCache.addPremium(name);
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.premium.enabled);
                    }
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                    }
                    inProcess.remove(player.getName());
                });
    }

    public void forceAuth(ProxiedPlayer player, PostLoginEvent event) {
        String name = player.getName();

        getUserAsync(name)
                .whenComplete((user, throwable) -> {
                    try {
                        if (throwable != null) {
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

                        String sessionIP = SessionCache.getIP(name);

                        if (PremiumCache.isPremium(name) ||
                                (sessionIP != null && sessionIP.equals(((InetSocketAddress) player.getSocketAddress()).getAddress().getHostAddress()))) {
                            AuthCache.setAuthenticated(name);

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

        if (!supportsDialog(player)) {
            return;
        }

        getUserAsync(player.getName())
                .thenAccept(user -> {
                    Dialog dialog;
                    if (user != null) {
                        dialog = new MultiActionDialog(
                                new DialogBase(TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.login.title))
                                        .inputs(
                                                List.of(
                                                        new TextInput("password", TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.login.passwordField))
                                                )
                                        ),
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
                        dialog = new MultiActionDialog(
                                new DialogBase(TextComponent.fromLegacy(CachedMessages.IMP.player.dialog.register.title))
                                        .inputs(inputList),
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
                })
                .exceptionally(throwable -> {
                    player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                    return null;
                });
    }

    private CompletableFuture<AuthUser> getUserAsync(String name) {
        CompletableFuture<AuthUser> future = new CompletableFuture<>();
        database.getAuthUserRepository().getUser(name, (user, success) -> {
            if (success) {
                future.complete(user);
            } else {
                future.completeExceptionally(new RuntimeException("Database query error"));
            }
        });
        return future;
    }

    private CompletableFuture<Void> registerUserAsync(String playerName, String password, String ip) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        database.getAuthUserRepository().registerUser(
                new AuthUser(
                        playerName.toLowerCase(Locale.ROOT),
                        playerName,
                        hash.hashPassword(password),
                        false,
                        ip
                ), success -> {
                    if (success) {
                        future.complete(null);
                    } else {
                        future.completeExceptionally(new RuntimeException("Database register error"));
                    }
                }
        );
        return future;
    }

    private CompletableFuture<Void> deleteUserAsync(String playerName) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        database.getAuthUserRepository().deleteUser(playerName, success -> {
            if (success) {
                future.complete(null);
            } else {
                future.completeExceptionally(new RuntimeException("Database delete error"));
            }
        });
        return future;
    }

    private CompletableFuture<Void> updatePasswordAsync(String playerName, String password) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        String hashedPassword = hash.hashPassword(password);
        database.getAuthUserRepository().updatePassword(playerName, hashedPassword, success -> {
            if (success) {
                future.complete(null);
            } else {
                future.completeExceptionally(new RuntimeException("Database update password error"));
            }
        });
        return future;
    }

    private CompletableFuture<Void> setPremiumAsync(String playerName, boolean enabled) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        database.getAuthUserRepository().setPremium(playerName, enabled, success -> {
            if (success) {
                future.complete(null);
            } else {
                future.completeExceptionally(new RuntimeException("Database set premium error"));
            }
        });
        return future;
    }

    private CompletableFuture<Void> completeRegistrationAsync(ProxiedPlayer player, String name, String password) {
        String ip = ((InetSocketAddress) player.getSocketAddress()).getAddress().getHostAddress();

        return registerUserAsync(name, password, ip)
                .thenRun(() -> {
                    BungeeUtils.sendMessage(player, CachedMessages.IMP.player.register.success);
                    AuthCache.setAuthenticated(name);
                    SessionCache.addPlayer(name, ip);
                    taskManager.cancelTasks(player);

                    PlayerRegisterEvent playerRegisterEvent = new PlayerRegisterEvent(player);
                    plugin.getProxy().getPluginManager().callEvent(playerRegisterEvent);

                    if (playerRegisterEvent.isMoveToBackendServer()) {
                        connectToBackend(player);
                    }
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
            return;
        }

        BungeeUtils.sendMessage(
                player,
                CachedMessages.IMP.player.login.wrongPassword
                        .replace("{attempts}", String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts))
        );

        if (supportsDialog(player)) {
            showLoginDialog(
                    player,
                    CachedMessages.IMP.player.dialog.notifications.wrongPassword
                            .replace("{attempts}", String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts))
            );
        }
    }
    private CompletableFuture<Void> processSuccessfulLoginAsync(ProxiedPlayer player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);

        authenticatePlayer(player, name, false);

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

        return CompletableFuture.completedFuture(null);
    }

    private void authenticatePlayer(ProxiedPlayer player, String name, boolean forceLogin) {
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

    private boolean supportsDialog(ProxiedPlayer player) {
        return player.getPendingConnection().getVersion() >= 771;
    }

    private boolean rejectEmptyPassword(ProxiedPlayer player, String password) {
        if (password.isEmpty()) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.passwordEmpty);
            if (supportsDialog(player)) {
                showLoginDialog(player, CachedMessages.IMP.player.dialog.notifications.passwordEmpty);
            }
            return true;
        }
        return false;
    }

    private boolean rejectInvalidPasswordLength(ProxiedPlayer player, String password) {
        if (password.length() < MainConfig.IMP.auth.minPasswordLength ||
                password.length() > MainConfig.IMP.auth.maxPasswordLength) {
            BungeeUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.invalidLength
                            .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                            .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength))
            );
            if (supportsDialog(player)) {
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

    private boolean rejectInvalidPasswordPattern(ProxiedPlayer player, String password) {
        if (!passwordPattern.matcher(password).matches()) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.invalidPattern);
            if (supportsDialog(player)) {
                showLoginDialog(player, CachedMessages.IMP.player.dialog.notifications.invalidPattern);
            }
            return true;
        }
        return false;
    }
}
