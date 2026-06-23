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
    private final Set<String> authenticatedPlayers = ConcurrentHashMap.newKeySet();

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
            if (supportDialog(player)) {
                showLoginDialog(player, CachedComponents.IMP.player.dialog.notifications.mismatch);
            }
            return;
        }

        if (checkPasswordEmpty(player, password) ||
                checkPasswordLength(player, password) ||
                checkPasswordPattern(player, password)) {
            return;
        }

        if (beginProcess(name)) {
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> {
            if (!success) {
                player.disconnect(CachedComponents.IMP.queryError);
                endProcess(name);
                return;
            }

            if (user != null) {
                player.sendMessage(CachedComponents.IMP.player.register.alreadyRegistered);
                endProcess(name);
                return;
            }

            completeRegistration(player, name, password);
        });
    }

    private void completeRegistration(Player player, String name, String password) {
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        registerPlayer(name, password, ip, success1 -> {
            if (!success1) {
                player.disconnect(CachedComponents.IMP.queryError);
                endProcess(name);
                return;
            }
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

            endProcess(name);
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

    public void unregisterPlayer(Player player, String password) {
        String name = player.getUsername();

        if (beginProcess(name)) {
            return;
        }

        if (checkPasswordLength(player, password)) {
            endProcess(name);
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> {
            if (!success) {
                player.sendMessage(CachedComponents.IMP.queryError);
                endProcess(name);
                return;
            }

            if (!hash.verifyPassword(password, user.getPassword())) {
                player.sendMessage(CachedComponents.IMP.player.checkPassword.wrongPassword);
                endProcess(name);
                return;
            }

            unregisterPlayer(name, success1 -> {
                if (!success1) {
                    player.sendMessage(CachedComponents.IMP.queryError);
                    endProcess(name);
                    return;
                }

                SessionCache.removePlayer(name);

                player.disconnect(CachedComponents.IMP.player.unregister.success);

                endProcess(name);
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

    public void loginPlayer(Player player, String password) {
        String name = player.getUsername();
        String lowerName = name.toLowerCase(Locale.ROOT);

        if (AuthCache.isAuthenticated(name)) {
            player.sendMessage(CachedComponents.IMP.player.login.alreadyLogged);
            return;
        }

        if (plugin.getTotpManager().isTotpPending(name)) {
            player.sendMessage(CachedComponents.IMP.player.totp.prompt);
            return;
        }

        if (authenticatedPlayers.contains(lowerName)) {
            player.sendMessage(CachedComponents.IMP.player.login.alreadyLogged);
            return;
        }

        if (checkPasswordEmpty(player, password)) {
            return;
        }

        if (beginProcess(name)) {
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> {
            if (!success) {
                player.disconnect(CachedComponents.IMP.queryError);
                endProcess(name);
                return;
            }

            if (user == null) {
                player.sendMessage(CachedComponents.IMP.player.login.notRegistered);
                endProcess(name);
                return;
            }

            if (!hash.verifyPassword(password, user.getPassword())) {
                handleWrongPasswordAttempt(player, name);
                return;
            }

            if (plugin.getTotpManager().isTotpLoginRequired(player, user)) {
                endProcess(name);
                return;
            }

            processSuccessfulLogin(player, name);
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
            endProcess(name);
            return;
        }

        player.sendMessage(
                CachedComponents.IMP.player.login.wrongPassword.replaceText(builder -> builder
                        .match(VelocityUtils.ATTEMPTS)
                        .replacement(String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts)))
        );

        if (supportDialog(player)) {
            showLoginDialog(player,
                    CachedComponents.IMP.player.dialog.notifications.wrongPassword.replaceText(builder -> builder
                            .match(VelocityUtils.ATTEMPTS)
                            .replacement(String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts)))
            );
        }
        endProcess(name);
    }

    private void processSuccessfulLogin(Player player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        loginPlayer(player, () -> {
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
            endProcess(name);
        });
    }

    public void loginPlayer(Player player, Runnable callback) {
        loginPlayer(player, callback, false);
    }

    public void loginPlayer(Player player, Runnable callback, boolean forceLogin) {
        String name = player.getUsername();
        String lowerName = name.toLowerCase(Locale.ROOT);
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        authenticatedPlayers.add(lowerName);
        AuthCache.setAuthenticated(name);
        database.getAuthUserRepository().updateLastLogin(name);
        database.getAuthUserRepository().updateLastIp(name, ip);
        SessionCache.addPlayer(name, ip);
        taskManager.cancelTasks(player);

        PlayerAuthEvent playerAuthEvent = new PlayerAuthEvent(player, forceLogin);
        plugin.getServer().getEventManager().fire(playerAuthEvent).thenAccept(firedEvent -> {
            if (firedEvent.isMoveToBackendServer()) {
                connectToBackend(player);
            }
        });

        callback.run();
    }

    public void changePasswordPlayer(Player player, String oldPassword, String newPassword) {
        String name = player.getUsername();

        if (checkPasswordEmpty(player, oldPassword) || checkPasswordEmpty(player, newPassword)) {
            return;
        }

        if (checkPasswordLength(player, oldPassword) || checkPasswordLength(player, newPassword)) {
            return;
        }

        if (checkPasswordPattern(player, newPassword)) {
            return;
        }

        if (beginProcess(name)) {
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> {
            if (!success) {
                player.sendMessage(CachedComponents.IMP.queryError);
                endProcess(name);
                return;
            }

            if (!hash.verifyPassword(oldPassword, user.getPassword())) {
                player.sendMessage(CachedComponents.IMP.player.checkPassword.wrongPassword);
                endProcess(name);
                return;
            }

            changePasswordPlayer(name, newPassword, success1 -> {
                if (!success1) {
                    player.sendMessage(CachedComponents.IMP.queryError);
                    endProcess(name);
                    return;
                }

                player.sendMessage(CachedComponents.IMP.player.changePassword.success);
                endProcess(name);
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

    public void logoutPlayer(Player player) {
        taskManager.cancelTasks(player);
        AuthCache.logout(player.getUsername());
        SessionCache.removePlayer(player.getUsername());
        authenticatedPlayers.remove(player.getUsername().toLowerCase(Locale.ROOT));
    }

    public void clearLoginAttempts(String lowerName) {
        loginAttempts.remove(lowerName);
    }

    public void togglePremium(Player player) {
        String name = player.getUsername();

        if (beginProcess(name)) {
            return;
        }

        boolean isPremium = PremiumCache.isPremium(name);

        database.getAuthUserRepository().setPremium(name, !isPremium, success -> {
            if (!success) {
                player.sendMessage(CachedComponents.IMP.queryError);
                endProcess(name);
                return;
            }

            if (isPremium) {
                PremiumCache.removePremium(name);
                player.sendMessage(CachedComponents.IMP.player.premium.disabled);
                endProcess(name);
                return;
            }

            PremiumCache.addPremium(name);
            player.sendMessage(CachedComponents.IMP.player.premium.enabled);
            endProcess(name);
        });
    }

    /**
     * Force-authenticates the player, bypassing the normal login flow.
     */
    public void forceAuth(Player player, PlayerChooseInitialServerEvent event, CompletableFuture<Void> future) {
        String name = player.getUsername();
        authenticatedPlayers.remove(name.toLowerCase(Locale.ROOT));

        database.getAuthUserRepository().getUser(name, (user, success) -> {
            try {
                if (!success) {
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
        // Руки в очке, не знаем как реализовать диалоги на велосити
    }

    public void showLoginDialog(Player player, java.util.function.Supplier<?> notice) {
        // Руки в очке, не знаем как реализовать диалоги на велосити
    }

    public void showLoginDialog(Player player, Object noticeComponent) {
        // Руки в очке, не знаем как реализовать диалоги на велосити
    }

    private void connectToAuthServer(Player player) {
        java.util.Optional<RegisteredServer> serverOpt = plugin.getServer().getServer(MainConfig.IMP.servers.auth);
        if (serverOpt.isEmpty()) {
            return;
        }

        RegisteredServer authServer = serverOpt.get();

        player.getCurrentServer().ifPresentOrElse(current -> {
            if (!current.getServer().equals(authServer)) {
                player.createConnectionRequest(authServer).connect();
            }
        }, () -> player.createConnectionRequest(authServer).connect());
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

        RegisteredServer targetServer = serverOpt.get();

        player.getCurrentServer().ifPresentOrElse(current -> {
            if (!current.getServer().equals(targetServer)) {
                player.createConnectionRequest(targetServer).connect();
            }
        }, () -> player.createConnectionRequest(targetServer).connect());
    }

    private Optional<String> getForcedHost(InetSocketAddress virtualHost) {
        return Optional.ofNullable(MainConfig.IMP.servers.forcedHosts.get(virtualHost.getHostString().toLowerCase()));
    }

    private boolean supportDialog(Player player) {
        return false;
    }

    private boolean beginProcess(String playerName) {
        return !inProcess.add(playerName);
    }

    private void endProcess(String playerName) {
        inProcess.remove(playerName);
    }

    private boolean checkPasswordEmpty(Player player, String password) {
        if (password.isEmpty()) {
            player.sendMessage(CachedComponents.IMP.player.checkPassword.passwordEmpty);
            if (supportDialog(player)) {
                showLoginDialog(player, CachedComponents.IMP.player.dialog.notifications.passwordEmpty);
            }
            return true;
        }
        return false;
    }

    private boolean checkPasswordLength(Player player, String password) {
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
            if (supportDialog(player)) {
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

    private boolean checkPasswordPattern(Player player, String password) {
        if (!passwordPattern.matcher(password).matches()) {
            player.sendMessage(CachedComponents.IMP.player.checkPassword.invalidPattern);
            if (supportDialog(player)) {
                showLoginDialog(player, CachedComponents.IMP.player.dialog.notifications.invalidPattern);
            }
            return true;
        }
        return false;
    }
}