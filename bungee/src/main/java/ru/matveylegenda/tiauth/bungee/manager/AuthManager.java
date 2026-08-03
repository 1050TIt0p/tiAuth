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
import ru.matveylegenda.tiauth.util.PasswordCheck;
import ru.matveylegenda.tiauth.util.PlayerLock;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

    public void registerPlayer(ProxiedPlayer player, String password, String repeatPassword) {
        String name = player.getName();

        if (!password.equals(repeatPassword)) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.register.mismatch);
            if (supportsDialog(player)) {
                showLoginDialog(player, CachedMessages.IMP.player.dialog.notifications.mismatch);
            }
            return;
        }

        if (rejectPassword(player, password, PasswordCheck.EMPTY, PasswordCheck.LENGTH, PasswordCheck.PATTERN)) {
            return;
        }

        playerLock.execute(name, () -> database.getAuthUserRepository().getUser(name)
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
                    }));
    }

    public CompletableFuture<Boolean> registerUser(String username, String password, String ip) {
        return registerUserAsync(username, password, ip)
                .thenApply(result -> true)
                .exceptionally(throwable -> false);
    }

    public void unregisterPlayer(ProxiedPlayer player, String password) {
        String name = player.getName();

        if (rejectPassword(player, password, PasswordCheck.LENGTH)) {
            return;
        }

        playerLock.execute(name, () -> database.getAuthUserRepository().getUser(name)
                    .thenCompose(user -> {
                        if (!hash.verifyPassword(password, user.getPassword())) {
                            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.wrongPassword);
                            return CompletableFuture.completedFuture(null);
                        }

                        return database.getAuthUserRepository().deleteUser(name)
                                .thenAccept(result -> {
                                    SessionCache.removePlayer(name);
                                    player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.player.unregister.success));
                                });
                    })
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                        }
                    }));
    }

    public CompletableFuture<Boolean> unregisterUser(String username) {
        return database.getAuthUserRepository().deleteUser(username)
                .thenApply(result -> true)
                .exceptionally(throwable -> false);
    }

    public void loginPlayer(ProxiedPlayer player, String password) {
        String name = player.getName();

        if (AuthCache.isAuthenticated(name)) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.alreadyLogged);
            return;
        }

        if (AuthCache.isTotpPending(name)) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.prompt);
            return;
        }

        if (AuthCache.isPendingVerification(name)) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.alreadyLogged);
            return;
        }

        if (rejectPassword(player, password, PasswordCheck.EMPTY)) {
            return;
        }

        playerLock.execute(name, () -> database.getAuthUserRepository().getUser(name)
                    .thenCompose(user -> {
                        if (user == null) {
                            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.notRegistered);
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
                            player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                        }
                    }));
    }

    public CompletableFuture<Void> loginPlayer(ProxiedPlayer player, boolean forceLogin) {
        String name = player.getName();
        return authenticatePlayer(player, name, forceLogin);
    }

    public void changePasswordPlayer(ProxiedPlayer player, String oldPassword, String newPassword) {
        String name = player.getName();

        if (rejectPassword(player, oldPassword, PasswordCheck.EMPTY, PasswordCheck.LENGTH)) {
            return;
        }
        if (rejectPassword(player, newPassword, PasswordCheck.EMPTY, PasswordCheck.LENGTH, PasswordCheck.PATTERN)) {
            return;
        }

        playerLock.execute(name, () -> database.getAuthUserRepository().getUser(name)
                    .thenCompose(user -> {
                        if (!hash.verifyPassword(oldPassword, user.getPassword())) {
                            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.wrongPassword);
                            return CompletableFuture.completedFuture(null);
                        }

                        return updatePasswordAsync(name, newPassword)
                                .thenAccept(result -> BungeeUtils.sendMessage(player, CachedMessages.IMP.player.changePassword.success));
                    })
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                        }
                    }));
    }

    public CompletableFuture<Boolean> changePasswordUser(String username, String password) {
        return updatePasswordAsync(username, password)
                .thenApply(result -> true)
                .exceptionally(throwable -> false);
    }

    public void logoutPlayer(ProxiedPlayer player) {
        taskManager.cancelTasks(player);
        AuthCache.logout(player.getName());
        SessionCache.removePlayer(player.getName());
    }

    public void togglePremium(ProxiedPlayer player) {
        String name = player.getName();
        boolean isPremium = PremiumCache.isPremium(name);

        playerLock.execute(name, () -> database.getAuthUserRepository().setPremium(name, !isPremium)
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
                    }));
    }

    public void forceAuth(ProxiedPlayer player, PostLoginEvent event) {
        String name = player.getName();

        database.getAuthUserRepository().getUser(name)
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
                                (sessionIP != null && sessionIP.equals(BungeeUtils.getIp(player)))) {
                            AuthCache.setAuthenticated(name);

                            if (event != null) {
                                event.setTarget(plugin.getProxy().getServerInfo(MainConfig.IMP.servers.backend));
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

        database.getAuthUserRepository().getUser(player.getName())
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

    private CompletableFuture<Void> completeRegistrationAsync(ProxiedPlayer player, String name, String password) {
        String ip = BungeeUtils.getIp(player);

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

    private void processFailedLogin(ProxiedPlayer player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        int attempts = AuthCache.incrementLoginAttempts(lowerName);

        if (attempts >= MainConfig.IMP.auth.loginAttempts) {
            player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.tooManyAttempts));
            if (MainConfig.IMP.auth.banPlayer) {
                BanCache.addBan(BungeeUtils.getIp(player));
            }
            AuthCache.resetLoginAttempts(lowerName);
            return;
        }

        reject(
                player,
                CachedMessages.IMP.player.login.wrongPassword
                        .replace("{attempts}", String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts)),
                CachedMessages.IMP.player.dialog.notifications.wrongPassword
                        .replace("{attempts}", String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts))
        );
    }

    private CompletableFuture<Void> processSuccessfulLoginAsync(ProxiedPlayer player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);

        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.success);

        return authenticatePlayer(player, name, false)
                .thenRun(() -> {
                    if (MainConfig.IMP.title.enabledOnAuth) {
                        Title title = ProxyServer.getInstance().createTitle();
                        title.title(TextComponent.fromLegacy(CachedMessages.IMP.player.title.onAuthTitle));
                        title.subTitle(TextComponent.fromLegacy(CachedMessages.IMP.player.title.onAuthSubTitle));
                        title.fadeIn(0);
                        title.stay(21);
                        title.fadeOut(6);
                        player.sendTitle(title);
                    }

                    AuthCache.resetLoginAttempts(lowerName);
                });
    }

    private CompletableFuture<Void> authenticatePlayer(ProxiedPlayer player, String name, boolean forceLogin) {
        String ip = BungeeUtils.getIp(player);
        String lowerName = name.toLowerCase(Locale.ROOT);

        AuthCache.setAuthenticated(name);
        database.getAuthUserRepository().updateLastLogin(name);
        database.getAuthUserRepository().updateLastIp(name, ip);
        SessionCache.addPlayer(name, ip);
        AuthCache.resetLoginAttempts(lowerName);
        taskManager.cancelTasks(player);

        PlayerAuthEvent playerAuthEvent = new PlayerAuthEvent(player, forceLogin);
        plugin.getProxy().getPluginManager().callEvent(playerAuthEvent);

        if (playerAuthEvent.isMoveToBackendServer()) {
            connectToBackend(player);
        }

        return CompletableFuture.completedFuture(null);
    }

    private void connectToAuthServer(PostLoginEvent event) {
        ServerInfo authServer = plugin.getProxy().getServerInfo(MainConfig.IMP.servers.auth);
        event.setTarget(authServer);
    }

    private void connectToAuthServer(ProxiedPlayer player) {
        ServerInfo authServer = plugin.getProxy().getServerInfo(MainConfig.IMP.servers.auth);
        connect(player, authServer);
    }

    private void connectToBackend(ProxiedPlayer player) {
        getBackend(player).ifPresent(server -> connect(player, server));
    }

    private Optional<ServerInfo> getBackend(ProxiedPlayer player) {
        return getForcedBackend(player)
                .or(this::getDefaultBackend);
    }

    private Optional<ServerInfo> getForcedBackend(ProxiedPlayer player) {
        return Optional.ofNullable(player.getPendingConnection().getVirtualHost())
                .flatMap(this::getForcedHost)
                .map(plugin.getProxy()::getServerInfo);
    }

    private Optional<ServerInfo> getDefaultBackend() {
        return Optional.ofNullable(plugin.getProxy().getServerInfo(MainConfig.IMP.servers.backend));
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

    boolean reject(
            ProxiedPlayer player,
            String chat,
            String dialog
    ) {
        BungeeUtils.sendMessage(player, chat);

        if (supportsDialog(player)) {
            showLoginDialog(player, dialog);
        }

        return true;
    }

    private boolean rejectPassword(ProxiedPlayer player, String password, PasswordCheck... checks) {
        Set<PasswordCheck> checkSet = EnumSet.copyOf(Arrays.asList(checks));
        String errorMessage;
        String dialogMessage;

        if (checkSet.contains(PasswordCheck.EMPTY) && password.isEmpty()) {
            errorMessage = CachedMessages.IMP.player.checkPassword.passwordEmpty;
            dialogMessage = CachedMessages.IMP.player.dialog.notifications.passwordEmpty;
        } else if (checkSet.contains(PasswordCheck.LENGTH) && (password.length() < MainConfig.IMP.auth.minPasswordLength ||
                password.length() > MainConfig.IMP.auth.maxPasswordLength)) {
            errorMessage = CachedMessages.IMP.player.checkPassword.invalidLength
                    .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                    .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength));
            dialogMessage = CachedMessages.IMP.player.dialog.notifications.invalidLength
                    .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                    .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength));
        } else if (checkSet.contains(PasswordCheck.PATTERN) && !passwordPattern.matcher(password).matches()) {
            errorMessage = CachedMessages.IMP.player.checkPassword.invalidPattern;
            dialogMessage = CachedMessages.IMP.player.dialog.notifications.invalidPattern;
        } else {
            return false;
        }

        return reject(player, errorMessage, dialogMessage);
    }
}
