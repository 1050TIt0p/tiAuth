package ru.matveylegenda.tiauth.velocity.manager;

import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.api.event.PlayerAuthEvent;
import ru.matveylegenda.tiauth.velocity.api.event.PlayerRegisterEvent;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

    public void registerPlayer(Player player, String password, String repeatPassword) {
        String name = player.getUsername();

        if (!password.equals(repeatPassword)) {
            player.sendMessage(CachedComponents.IMP.player.register.mismatch);
            if (supportsDialog(player)) {
                showLoginDialog(player, CachedComponents.IMP.player.dialog.notifications.mismatch);
            }
            return;
        }

        if (rejectEmptyPassword(player, password) ||
                rejectInvalidPasswordLength(player, password) ||
                rejectInvalidPasswordPattern(player, password)) {
            return;
        }

        if (!inProcess.add(name)) {
            return;
        }

        getUserAsync(name)
                .thenCompose(user -> {
                    if (user != null) {
                        player.sendMessage(CachedComponents.IMP.player.register.alreadyRegistered);
                        return CompletableFuture.completedFuture(null);
                    }

                    return completeRegistrationAsync(player, name, password);
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        player.disconnect(CachedComponents.IMP.queryError);
                    }
                    inProcess.remove(name);
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

    public void unregisterPlayer(Player player, String password) {
        String name = player.getUsername();

        if (!inProcess.add(name)) {
            return;
        }

        if (rejectInvalidPasswordLength(player, password)) {
            inProcess.remove(name);
            return;
        }

        getUserAsync(name)
                .thenCompose(user -> {
                    if (!hash.verifyPassword(password, user.getPassword())) {
                        player.sendMessage(CachedComponents.IMP.player.checkPassword.wrongPassword);
                        return CompletableFuture.completedFuture(null);
                    }

                    return deleteUserAsync(name)
                            .thenAccept(result -> {
                                SessionCache.removePlayer(name);
                                player.disconnect(CachedComponents.IMP.player.unregister.success);
                            });
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        player.sendMessage(CachedComponents.IMP.queryError);
                    }
                    inProcess.remove(name);
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

    public void loginPlayer(Player player, String password) {
        String name = player.getUsername();

        if (AuthCache.isAuthenticated(name)) {
            player.sendMessage(CachedComponents.IMP.player.login.alreadyLogged);
            return;
        }

        if (plugin.getTotpManager().isTotpPending(name)) {
            player.sendMessage(CachedComponents.IMP.player.totp.prompt);
            return;
        }

        if (rejectEmptyPassword(player, password)) {
            return;
        }

        if (!inProcess.add(name)) {
            return;
        }

        getUserAsync(name)
                .thenCompose(user -> {
                    if (user == null) {
                        player.sendMessage(CachedComponents.IMP.player.login.notRegistered);
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
                        player.disconnect(CachedComponents.IMP.queryError);
                    }
                    inProcess.remove(name);
                });
    }

    public void loginPlayer(Player player, Runnable callback) {
        loginPlayer(player, callback, false);
    }

    public void loginPlayer(Player player, Runnable callback, boolean forceLogin) {
        String name = player.getUsername();
        authenticatePlayer(player, name, forceLogin)
                .thenRun(callback);
    }

    public void changePasswordPlayer(Player player, String oldPassword, String newPassword) {
        String name = player.getUsername();

        if (rejectEmptyPassword(player, oldPassword) || rejectEmptyPassword(player, newPassword)) {
            return;
        }

        if (rejectInvalidPasswordLength(player, oldPassword) || rejectInvalidPasswordLength(player, newPassword)) {
            return;
        }

        if (rejectInvalidPasswordPattern(player, newPassword)) {
            return;
        }

        if (!inProcess.add(name)) {
            return;
        }

        getUserAsync(name)
                .thenCompose(user -> {
                    if (!hash.verifyPassword(oldPassword, user.getPassword())) {
                        player.sendMessage(CachedComponents.IMP.player.checkPassword.wrongPassword);
                        return CompletableFuture.completedFuture(null);
                    }

                    return updatePasswordAsync(name, newPassword)
                            .thenAccept(result -> {
                                player.sendMessage(CachedComponents.IMP.player.changePassword.success);
                            });
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        player.sendMessage(CachedComponents.IMP.queryError);
                    }
                    inProcess.remove(name);
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

    public void logoutPlayer(Player player) {
        taskManager.cancelTasks(player);
        AuthCache.logout(player.getUsername());
        SessionCache.removePlayer(player.getUsername());
    }

    public void resetLoginAttempts(String lowerName) {
        loginAttempts.remove(lowerName);
    }

    public void togglePremium(Player player) {
        String name = player.getUsername();

        if (!inProcess.add(name)) {
            return;
        }

        boolean isPremium = PremiumCache.isPremium(name);

        setPremiumAsync(name, !isPremium)
                .thenAccept(result -> {
                    if (isPremium) {
                        PremiumCache.removePremium(name);
                        player.sendMessage(CachedComponents.IMP.player.premium.disabled);
                    } else {
                        PremiumCache.addPremium(name);
                        player.sendMessage(CachedComponents.IMP.player.premium.enabled);
                    }
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        player.sendMessage(CachedComponents.IMP.queryError);
                    }
                    inProcess.remove(name);
                });
    }

    public void forceAuth(Player player, PlayerChooseInitialServerEvent event, CompletableFuture<Void> future) {
        String name = player.getUsername();

        getUserAsync(name)
                .whenComplete((user, throwable) -> {
                    try {
                        if (throwable != null) {
                            player.disconnect(CachedComponents.IMP.queryError);
                            return;
                        }

                        if (user != null && !player.getUsername().equals(user.getRealName())) {
                            player.disconnect(CachedComponents.IMP.player.kick.realname
                                    .replaceText(builder -> builder
                                            .match(VelocityUtils.REAL_NAME)
                                            .replacement(user.getRealName()))
                                    .replaceText(builder -> builder
                                            .match(VelocityUtils.NAME)
                                            .replacement(player.getUsername())));
                            return;
                        }

                        String sessionIP = SessionCache.getIP(name);
                        String remoteIp = player.getRemoteAddress().getAddress().getHostAddress();

                        if (PremiumCache.isPremium(name) || (sessionIP != null && sessionIP.equals(remoteIp))) {
                            AuthCache.setAuthenticated(name);
                            if (event != null) {
                                connectToBackend(event);
                            } else {
                                connectToBackend(player);
                            }
                            return;
                        }

                        if (event == null && future == null) {
                            connectToAuthServer(player);
                        } else if (event != null) {
                            Optional<RegisteredServer> authOpt = plugin.getServer().getServer(MainConfig.IMP.servers.auth);
                            authOpt.ifPresent(event::setInitialServer);
                        }

                        Component reminderMessage = (user != null)
                                ? CachedComponents.IMP.player.reminder.login
                                : CachedComponents.IMP.player.reminder.register;

                        taskManager.startAuthTimeoutTask(player);
                        taskManager.startAuthReminderTask(player, reminderMessage);
                    } finally {
                        if (event != null && future != null) {
                            future.complete(null);
                        }
                    }
                });
    }

    public void showLoginDialog(Player player) {
    }

    public void showLoginDialog(Player player, Object noticeComponent) {
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

    private CompletableFuture<Void> completeRegistrationAsync(Player player, String name, String password) {
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        return registerUserAsync(name, password, ip)
                .thenRun(() -> {
                    player.sendMessage(CachedComponents.IMP.player.register.success);
                    AuthCache.setAuthenticated(name);
                    SessionCache.addPlayer(name, ip);
                    taskManager.cancelTasks(player);

                    PlayerRegisterEvent playerRegisterEvent = new PlayerRegisterEvent(player);
                    plugin.getServer().getEventManager().fire(playerRegisterEvent).thenAccept(firedEvent -> {
                        if (firedEvent.isMoveToBackendServer()) {
                            connectToBackend(player);
                        }
                    });
                });
    }

    private void handleWrongPasswordAttempt(Player player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        int attempts = loginAttempts.merge(lowerName, 1, Integer::sum);

        if (attempts >= MainConfig.IMP.auth.loginAttempts) {
            player.disconnect(CachedComponents.IMP.player.kick.tooManyAttempts);
            if (MainConfig.IMP.auth.banPlayer) {
                BanCache.addPlayer(player.getRemoteAddress().getAddress().getHostAddress());
            }
            loginAttempts.remove(lowerName);
            return;
        }

        player.sendMessage(
                CachedComponents.IMP.player.login.wrongPassword.replaceText(builder -> builder
                        .match(VelocityUtils.ATTEMPTS)
                        .replacement(String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts)))
        );

        if (supportsDialog(player)) {
            showLoginDialog(player,
                    CachedComponents.IMP.player.dialog.notifications.wrongPassword.replaceText(builder -> builder
                            .match(VelocityUtils.ATTEMPTS)
                            .replacement(String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts)))
            );
        }
    }

    private CompletableFuture<Void> processSuccessfulLoginAsync(Player player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);

        return authenticatePlayer(player, name, false)
                .thenRun(() -> {
                    player.sendMessage(CachedComponents.IMP.player.login.success);

                    if (MainConfig.IMP.title.enabledOnAuth) {
                        Title componentTitle = Title.title(
                                CachedComponents.IMP.player.title.onAuthTitle,
                                CachedComponents.IMP.player.title.onAuthSubTitle,
                                0,
                                21,
                                0);
                        player.showTitle(componentTitle);
                    }

                    loginAttempts.remove(lowerName);
                });
    }

    private CompletableFuture<Void> authenticatePlayer(Player player, String name, boolean forceLogin) {
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        AuthCache.setAuthenticated(name);
        database.getAuthUserRepository().updateLastLogin(name);
        database.getAuthUserRepository().updateLastIp(name, ip);
        SessionCache.addPlayer(name, ip);
        taskManager.cancelTasks(player);

        PlayerAuthEvent playerAuthEvent = new PlayerAuthEvent(player, forceLogin);
        return plugin.getServer().getEventManager().fire(playerAuthEvent)
                .thenAccept(firedEvent -> {
                    if (firedEvent.isMoveToBackendServer()) {
                        connectToBackend(player);
                    }
                });
    }

    private void connectToAuthServer(Player player) {
        java.util.Optional<RegisteredServer> serverOpt = plugin.getServer().getServer(MainConfig.IMP.servers.auth);
        if (serverOpt.isEmpty()) {
            return;
        }

        connect(player, serverOpt.get());
    }

    private void connectToBackend(PlayerChooseInitialServerEvent event) {
        String targetName = event.getPlayer().getVirtualHost()
                .flatMap(this::getForcedHost)
                .orElse(MainConfig.IMP.servers.backend);
        plugin.getServer().getServer(targetName).ifPresent(event::setInitialServer);
    }

    private void connectToBackend(Player player) {
        String targetName = player.getVirtualHost()
                .flatMap(this::getForcedHost)
                .orElse(MainConfig.IMP.servers.backend);

        Optional<RegisteredServer> serverOpt = plugin.getServer().getServer(targetName);
        if (serverOpt.isEmpty()) {
            return;
        }

        connect(player, serverOpt.get());
    }

    private void connect(Player player, RegisteredServer target) {
        player.getCurrentServer().ifPresentOrElse(current -> {
            if (!current.getServer().equals(target)) {
                player.createConnectionRequest(target).connect();
            }
        }, () -> player.createConnectionRequest(target).connect());
    }

    private Optional<String> getForcedHost(InetSocketAddress virtualHost) {
        return Optional.ofNullable(MainConfig.IMP.servers.forcedHosts.get(virtualHost.getHostString().toLowerCase()));
    }

    private boolean supportsDialog(Player player) {
        return false;
    }

    private boolean rejectEmptyPassword(Player player, String password) {
        if (password.isEmpty()) {
            player.sendMessage(CachedComponents.IMP.player.checkPassword.passwordEmpty);
            if (supportsDialog(player)) {
                showLoginDialog(player, CachedComponents.IMP.player.dialog.notifications.passwordEmpty);
            }
            return true;
        }
        return false;
    }

    private boolean rejectInvalidPasswordLength(Player player, String password) {
        if (password.length() < MainConfig.IMP.auth.minPasswordLength ||
                password.length() > MainConfig.IMP.auth.maxPasswordLength) {
            player.sendMessage(
                    CachedComponents.IMP.player.checkPassword.invalidLength
                            .replaceText(builder -> builder
                                    .match(VelocityUtils.MIN)
                                    .replacement(String.valueOf(MainConfig.IMP.auth.minPasswordLength)))
                            .replaceText(builder -> builder
                                    .match(VelocityUtils.MAX)
                                    .replacement(String.valueOf(MainConfig.IMP.auth.maxPasswordLength)))
            );
            if (supportsDialog(player)) {
                showLoginDialog(player,
                        CachedComponents.IMP.player.dialog.notifications.invalidLength
                                .replaceText(builder -> builder
                                        .match(VelocityUtils.MIN)
                                        .replacement(String.valueOf(MainConfig.IMP.auth.minPasswordLength)))
                                .replaceText(builder -> builder
                                        .match(VelocityUtils.MAX)
                                        .replacement(String.valueOf(MainConfig.IMP.auth.maxPasswordLength)))
                );
            }
            return true;
        }
        return false;
    }

    private boolean rejectInvalidPasswordPattern(Player player, String password) {
        if (!passwordPattern.matcher(password).matches()) {
            player.sendMessage(CachedComponents.IMP.player.checkPassword.invalidPattern);
            if (supportsDialog(player)) {
                showLoginDialog(player, CachedComponents.IMP.player.dialog.notifications.invalidPattern);
            }
            return true;
        }
        return false;
    }
}
