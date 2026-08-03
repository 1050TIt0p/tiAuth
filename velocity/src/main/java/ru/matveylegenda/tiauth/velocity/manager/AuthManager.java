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
import ru.matveylegenda.tiauth.util.PasswordCheck;
import ru.matveylegenda.tiauth.util.PlayerLock;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class AuthManager {

    private final PlayerLock playerLock = new PlayerLock();
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

        if (rejectPassword(player, password, PasswordCheck.EMPTY, PasswordCheck.LENGTH, PasswordCheck.PATTERN)) {
            return;
        }

        playerLock.execute(name, () -> database.getAuthUserRepository().getUser(name)
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
                    }));
    }

    public CompletableFuture<Boolean> registerUser(String username, String password, String ip) {
        return registerUserAsync(username, password, ip)
                .thenApply(result -> true)
                .exceptionally(throwable -> false);
    }

    public void unregisterPlayer(Player player, String password) {
        String name = player.getUsername();

        if (rejectPassword(player, password, PasswordCheck.LENGTH)) {
            return;
        }

        playerLock.execute(name, () -> database.getAuthUserRepository().getUser(name)
                    .thenCompose(user -> {
                        if (!hash.verifyPassword(password, user.getPassword())) {
                            player.sendMessage(CachedComponents.IMP.player.checkPassword.wrongPassword);
                            return CompletableFuture.completedFuture(null);
                        }

                        return database.getAuthUserRepository().deleteUser(name)
                                .thenAccept(result -> {
                                    SessionCache.removePlayer(name);
                                    player.disconnect(CachedComponents.IMP.player.unregister.success);
                                });
                    })
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            player.sendMessage(CachedComponents.IMP.queryError);
                        }
                    }));
    }

    public CompletableFuture<Boolean> unregisterUser(String username) {
        return database.getAuthUserRepository().deleteUser(username)
                .thenApply(result -> true)
                .exceptionally(throwable -> false);
    }

    public void loginPlayer(Player player, String password) {
        String name = player.getUsername();

        if (AuthCache.isAuthenticated(name)) {
            player.sendMessage(CachedComponents.IMP.player.login.alreadyLogged);
            return;
        }

        if (AuthCache.isTotpPending(name)) {
            player.sendMessage(CachedComponents.IMP.player.totp.prompt);
            return;
        }

        if (AuthCache.isPendingVerification(name)) {
            player.sendMessage(CachedComponents.IMP.player.login.alreadyLogged);
            return;
        }

        if (rejectPassword(player, password, PasswordCheck.EMPTY)) {
            return;
        }

        playerLock.execute(name, () -> database.getAuthUserRepository().getUser(name)
                    .thenCompose(user -> {
                        if (user == null) {
                            player.sendMessage(CachedComponents.IMP.player.login.notRegistered);
                            return CompletableFuture.completedFuture(null);
                        }

                        if (!hash.verifyPassword(password, user.getPassword())) {
                            processFailedLogin(player, name);
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
                    }));
    }

    public CompletableFuture<Void> loginPlayer(Player player, boolean forceLogin) {
        String name = player.getUsername();
        return authenticatePlayer(player, name, forceLogin);
    }

    public void changePasswordPlayer(Player player, String oldPassword, String newPassword) {
        String name = player.getUsername();

        if (rejectPassword(player, oldPassword, PasswordCheck.EMPTY, PasswordCheck.LENGTH)) {
            return;
        }
        if (rejectPassword(player, newPassword, PasswordCheck.EMPTY, PasswordCheck.LENGTH, PasswordCheck.PATTERN)) {
            return;
        }

        playerLock.execute(name, () -> database.getAuthUserRepository().getUser(name)
                    .thenCompose(user -> {
                        if (!hash.verifyPassword(oldPassword, user.getPassword())) {
                            player.sendMessage(CachedComponents.IMP.player.checkPassword.wrongPassword);
                            return CompletableFuture.completedFuture(null);
                        }

                        return updatePasswordAsync(name, newPassword)
                                .thenAccept(result -> player.sendMessage(CachedComponents.IMP.player.changePassword.success));
                    })
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            player.sendMessage(CachedComponents.IMP.queryError);
                        }
                    }));
    }

    public CompletableFuture<Boolean> changePasswordUser(String username, String password) {
        return updatePasswordAsync(username, password)
                .thenApply(result -> true)
                .exceptionally(throwable -> false);
    }

    public void logoutPlayer(Player player) {
        taskManager.cancelTasks(player);
        AuthCache.logout(player.getUsername());
        SessionCache.removePlayer(player.getUsername());
    }

    public void togglePremium(Player player) {
        String name = player.getUsername();
        boolean isPremium = PremiumCache.isPremium(name);

        playerLock.execute(name, () -> database.getAuthUserRepository().setPremium(name, !isPremium)
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
                    }));
    }

    public void forceAuth(Player player, PlayerChooseInitialServerEvent event, CompletableFuture<Void> future) {
        String name = player.getUsername();

        database.getAuthUserRepository().getUser(name)
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
                        String remoteIp = VelocityUtils.getIp(player);

                        if (PremiumCache.isPremium(name) || (sessionIP != null && sessionIP.equals(remoteIp))) {
                            AuthCache.setAuthenticated(name);
                            if (event != null) {
                                plugin.getServer().getServer(MainConfig.IMP.servers.backend).ifPresent(event::setInitialServer);
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

    private CompletableFuture<Void> registerUserAsync(String username, String password, String ip) {
        return database.getAuthUserRepository().registerUser(
                new AuthUser(
                        username.toLowerCase(Locale.ROOT),
                        username,
                        hash.hashPassword(password),
                        false,
                        ip
                )
        );
    }

    private CompletableFuture<Void> updatePasswordAsync(String username, String password) {
        String hashedPassword = hash.hashPassword(password);
        return database.getAuthUserRepository().updatePassword(username, hashedPassword);
    }

    private CompletableFuture<Void> completeRegistrationAsync(Player player, String name, String password) {
        String ip = VelocityUtils.getIp(player);

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

    private void processFailedLogin(Player player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        int attempts = AuthCache.incrementLoginAttempts(lowerName);

        if (attempts >= MainConfig.IMP.auth.loginAttempts) {
            player.disconnect(CachedComponents.IMP.player.kick.tooManyAttempts);
            if (MainConfig.IMP.auth.banPlayer) {
                BanCache.addBan(VelocityUtils.getIp(player));
            }
            AuthCache.resetLoginAttempts(lowerName);
            return;
        }

        reject(
                player,
                CachedComponents.IMP.player.login.wrongPassword.replaceText(builder -> builder
                        .match(VelocityUtils.ATTEMPTS)
                        .replacement(String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts))),
                CachedComponents.IMP.player.dialog.notifications.wrongPassword.replaceText(builder -> builder
                        .match(VelocityUtils.ATTEMPTS)
                        .replacement(String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts)))
        );
    }

    private CompletableFuture<Void> processSuccessfulLoginAsync(Player player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);

        player.sendMessage(CachedComponents.IMP.player.login.success);

        return authenticatePlayer(player, name, false)
                .thenRun(() -> {
                    if (MainConfig.IMP.title.enabledOnAuth) {
                        Title componentTitle = Title.title(
                                CachedComponents.IMP.player.title.onAuthTitle,
                                CachedComponents.IMP.player.title.onAuthSubTitle,
                                0,
                                21,
                                6);
                        player.showTitle(componentTitle);
                    }

                    AuthCache.resetLoginAttempts(lowerName);
                });
    }

    private CompletableFuture<Void> authenticatePlayer(Player player, String name, boolean forceLogin) {
        String ip = VelocityUtils.getIp(player);
        String lowerName = name.toLowerCase(Locale.ROOT);

        AuthCache.setAuthenticated(name);
        database.getAuthUserRepository().updateLastLogin(name);
        database.getAuthUserRepository().updateLastIp(name, ip);
        SessionCache.addPlayer(name, ip);
        AuthCache.resetLoginAttempts(lowerName);
        taskManager.cancelTasks(player);

        PlayerAuthEvent playerAuthEvent = new PlayerAuthEvent(player, forceLogin);
        return plugin.getServer().getEventManager().fire(playerAuthEvent)
                .thenCompose(firedEvent -> {
                    if (firedEvent.isMoveToBackendServer()) {
                        return connectToBackend(player);
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }

    private void connectToAuthServer(Player player) {
        Optional<RegisteredServer> serverOpt = plugin.getServer().getServer(MainConfig.IMP.servers.auth);
        if (serverOpt.isEmpty()) {
            return;
        }

        connect(player, serverOpt.get());
    }

    private CompletableFuture<Void> connectToBackend(Player player) {
        return getBackend(player)
                .map(backend -> connect(player, backend))
                .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    private Optional<RegisteredServer> getBackend(Player player) {
        return getForcedBackend(player)
                .or(this::getDefaultBackend);
    }

    private Optional<RegisteredServer> getForcedBackend(Player player) {
        return player.getVirtualHost()
                .flatMap(this::getForcedHost)
                .flatMap(plugin.getServer()::getServer);
    }

    private Optional<RegisteredServer> getDefaultBackend() {
        return plugin.getServer().getServer(MainConfig.IMP.servers.backend);
    }

    private CompletableFuture<Void> connect(Player player, RegisteredServer target) {
        return player.getCurrentServer()
                .map(current -> {
                    if (!current.getServer().equals(target)) {
                        return player.createConnectionRequest(target).connect();
                    }
                    return CompletableFuture.completedFuture(true);
                })
                .orElseGet(() -> player.createConnectionRequest(target).connect())
                .exceptionally(throwable -> null)
                .thenRun(() -> {});
    }

    private Optional<String> getForcedHost(InetSocketAddress virtualHost) {
        return Optional.ofNullable(MainConfig.IMP.servers.forcedHosts.get(virtualHost.getHostString().toLowerCase()));
    }

    private boolean supportsDialog(Player player) {
        return false;
    }

    boolean reject(
            Player player,
            Component chat,
            Component dialog
    ) {
        player.sendMessage(chat);

        if (supportsDialog(player)) {
            showLoginDialog(player, dialog);
        }

        return true;
    }

    private boolean rejectPassword(Player player, String password, PasswordCheck... checks) {
        Set<PasswordCheck> checkSet = EnumSet.copyOf(Arrays.asList(checks));
        Component errorMessage;
        Component dialogMessage;

        if (checkSet.contains(PasswordCheck.EMPTY) && password.isEmpty()) {
            errorMessage = CachedComponents.IMP.player.checkPassword.passwordEmpty;
            dialogMessage = CachedComponents.IMP.player.dialog.notifications.passwordEmpty;
        } else if (checkSet.contains(PasswordCheck.LENGTH) && (password.length() < MainConfig.IMP.auth.minPasswordLength ||
                password.length() > MainConfig.IMP.auth.maxPasswordLength)) {
            errorMessage = CachedComponents.IMP.player.checkPassword.invalidLength
                    .replaceText(builder -> builder
                            .match(VelocityUtils.MIN)
                            .replacement(String.valueOf(MainConfig.IMP.auth.minPasswordLength)))
                    .replaceText(builder -> builder
                            .match(VelocityUtils.MAX)
                            .replacement(String.valueOf(MainConfig.IMP.auth.maxPasswordLength)));
            dialogMessage = CachedComponents.IMP.player.dialog.notifications.invalidLength
                    .replaceText(builder -> builder
                            .match(VelocityUtils.MIN)
                            .replacement(String.valueOf(MainConfig.IMP.auth.minPasswordLength)))
                    .replaceText(builder -> builder
                            .match(VelocityUtils.MAX)
                            .replacement(String.valueOf(MainConfig.IMP.auth.maxPasswordLength)));
        } else if (checkSet.contains(PasswordCheck.PATTERN) && !passwordPattern.matcher(password).matches()) {
            errorMessage = CachedComponents.IMP.player.checkPassword.invalidPattern;
            dialogMessage = CachedComponents.IMP.player.dialog.notifications.invalidPattern;
        } else {
            return false;
        }

        return reject(player, errorMessage, dialogMessage);
    }
}
