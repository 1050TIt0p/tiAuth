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

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class AuthManager {
    public static final CodeVerifier TOTP_CODE_VERIFIER = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    private final Set<String> inProcess = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, String> forcedHostMap = new ConcurrentHashMap<>();
    private final Set<String> totpPendingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> totpAttempts = new ConcurrentHashMap<>();
    private final Map<String, String> totpEnableSecrets = new ConcurrentHashMap<>();

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

    //
    // Process management
    //

    private boolean beginProcess(String playerName) {
        if (!inProcess.add(playerName)) {
            plugin.getServer().getPlayer(playerName).ifPresent(p -> p.sendMessage(CachedComponents.IMP.processing));
            return false;
        }
        return true;
    }

    private void endProcess(String playerName) {
        inProcess.remove(playerName);
    }

    //
    // Dialog
    //

    private boolean supportDialog(Player player) {
        return false;
    }

    public void showLoginDialog(Player player) {
        // Don't know how to implement dialogs on Velocity
    }

    public void showLoginDialog(Player player, java.util.function.Supplier<?> notice) {
        // Don't know how to implement dialogs on Velocity
    }

    public void showLoginDialog(Player player, Object noticeComponent) {
        // Don't know how to implement dialogs on Velocity
    }

    //
    // Forced host management
    //

    public void setForcedHost(String playerName, String serverName) {
        forcedHostMap.put(playerName.toLowerCase(), serverName);
    }

    public void removeForcedHost(String playerName) {
        forcedHostMap.remove(playerName.toLowerCase());
    }

    private Optional<RegisteredServer> resolveBackendServer(String playerName) {
        if (MainConfig.IMP.servers.postAuthServerMode == MainConfig.PostAuthServerMode.FORCED_HOST) {
            String forcedHost = forcedHostMap.get(playerName.toLowerCase());
            if (forcedHost == null) {
                return Optional.empty();
            }
            return plugin.getServer().getServer(forcedHost);
        }
        return plugin.getServer().getServer(MainConfig.IMP.servers.backend);
    }

    //
    // Auth flow
    //

    public void forceAuth(Player player) {
        forceAuth(player, null, null);
    }

    /**
     * Force-authenticates a player (for external API calls).
     */
    public void forceAuth(Player player, PlayerChooseInitialServerEvent event, CompletableFuture<Void> future) {
        String name = player.getUsername();

        database.getAuthUserRepository().getUser(name, (user, success) -> {
            try {
                handleForceAuthUser(player, event, user, success);
            } finally {
                if (event != null && future != null) {
                    future.complete(null);
                }
            }
        });
    }

    private void handleForceAuthUser(Player player, PlayerChooseInitialServerEvent event, AuthUser user, boolean success) {
        if (!success) {
            player.disconnect(CachedComponents.IMP.queryError);
            return;
        }

        String name = player.getUsername();

        if (user != null && !name.equals(user.getRealName())) {
            player.disconnect(CachedComponents.IMP.player.kick.realname
                    .replaceText(builder -> builder
                            .match(VelocityUtils.REAL_NAME)
                            .replacement(user.getRealName()))
                    .replaceText(builder -> builder
                            .match(VelocityUtils.NAME)
                            .replacement(name)));
            return;
        }

        String sessionIP = SessionCache.getIP(name);
        String remoteIp = player.getRemoteAddress().getAddress().getHostAddress();

        boolean isExternalCall = event == null;

        if (PremiumCache.isPremium(name) || (sessionIP != null && sessionIP.equals(remoteIp))) {
            AuthCache.setAuthenticated(name);
            if (isExternalCall) {
                connectToBackend(player);
            } else {
                Optional<RegisteredServer> backend = resolveBackendServer(name);
                if (backend.isPresent()) {
                    event.setInitialServer(backend.get());
                } else if (MainConfig.IMP.servers.postAuthServerMode == MainConfig.PostAuthServerMode.FORCED_HOST) {
                    player.disconnect(CachedComponents.IMP.player.kick.forcedHostNotFound);
                    return;
                }
            }
            return;
        }

        if (isExternalCall) {
            connectToAuthServer(player);
        } else {
            Optional<RegisteredServer> authOpt = plugin.getServer().getServer(MainConfig.IMP.servers.auth);
            if (authOpt.isEmpty()) {
                player.disconnect(CachedComponents.IMP.player.kick.authServerNotFound);
                return;
            }
            event.setInitialServer(authOpt.get());
        }

        Component reminderMessage = (user != null)
                ? CachedComponents.IMP.player.reminder.login
                : CachedComponents.IMP.player.reminder.register;

        taskManager.startAuthTimeoutTask(player);
        taskManager.startAuthReminderTask(player, reminderMessage);
    }

    //
    // Connection helpers
    //

    private void connectToAuthServer(Player player) {
        java.util.Optional<RegisteredServer> serverOpt = plugin.getServer().getServer(MainConfig.IMP.servers.auth);
        if (serverOpt.isEmpty()) {
            player.disconnect(CachedComponents.IMP.player.kick.authServerNotFound);
            return;
        }

        RegisteredServer authServer = serverOpt.get();

        player.getCurrentServer().ifPresentOrElse(current -> {
            if (!current.getServer().equals(authServer)) {
                player.createConnectionRequest(authServer).connect();
            }
        }, () -> player.createConnectionRequest(authServer).connect());
    }

    private void connectToBackend(Player player) {
        Optional<RegisteredServer> backend = resolveBackendServer(player.getUsername());
        if (backend.isPresent()) {
            RegisteredServer target = backend.get();
            player.getCurrentServer().ifPresentOrElse(current -> {
                if (!current.getServer().equals(target)) {
                    player.createConnectionRequest(target).connect();
                }
            }, () -> player.createConnectionRequest(target).connect());
        } else if (MainConfig.IMP.servers.postAuthServerMode == MainConfig.PostAuthServerMode.FORCED_HOST) {
            player.disconnect(CachedComponents.IMP.player.kick.forcedHostNotFound);
        }
    }

    //
    // Player actions
    //

    public void registerPlayer(Player player, String password, String repeatPassword) {
        String name = player.getUsername();

        if (!password.equals(repeatPassword)) {
            player.sendMessage(CachedComponents.IMP.player.register.mismatch);

            if (supportDialog(player)) {
                showLoginDialog(player, CachedComponents.IMP.player.dialog.notifications.mismatch);
            }

            return;
        }

        if (password.isEmpty()) {
            player.sendMessage(CachedComponents.IMP.player.checkPassword.passwordEmpty);

            if (supportDialog(player)) {
                showLoginDialog(player, CachedComponents.IMP.player.dialog.notifications.passwordEmpty);
            }

            return;
        }

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

            return;
        }

        if (!passwordPattern.matcher(password).matches()) {
            player.sendMessage(CachedComponents.IMP.player.checkPassword.invalidPattern);

            if (supportDialog(player)) {
                showLoginDialog(player, CachedComponents.IMP.player.dialog.notifications.invalidPattern);
            }

            return;
        }

        if (!beginProcess(name)) {
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

                    endProcess(name);
                });
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

    public void loginPlayer(Player player, String password) {
        String name = player.getUsername();

        if (AuthCache.isAuthenticated(name)) {
            player.sendMessage(CachedComponents.IMP.player.login.alreadyLogged);
            return;
        }

        if (password.isEmpty()) {
            player.sendMessage(CachedComponents.IMP.player.checkPassword.passwordEmpty);

            if (supportDialog(player)) {
                showLoginDialog(player, CachedComponents.IMP.player.dialog.notifications.passwordEmpty);
            }

            return;
        }

        if (!beginProcess(name)) {
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


            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(password, hashedPassword)) {
                int attempts = loginAttempts.merge(name, 1, Integer::sum);

                if (attempts >= MainConfig.IMP.auth.loginAttempts) {
                    player.disconnect(CachedComponents.IMP.player.kick.tooManyAttempts);

                    if (MainConfig.IMP.auth.banPlayer) {
                        String ip = player.getRemoteAddress().getAddress().getHostAddress();
                        BanCache.addPlayer(ip);
                    }

                    loginAttempts.remove(name);
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
                return;
            }

            String totpToken = user.getTotpToken();
            if (MainConfig.IMP.auth.totp.enabled && totpToken != null && !totpToken.isEmpty()) {
                endProcess(name);
                totpPendingPlayers.add(name.toLowerCase());
                taskManager.cancelTasks(player);
                player.sendMessage(CachedComponents.IMP.player.totp.prompt);
                return;
            }

            loginPlayer(player, () -> {
                player.sendMessage(CachedComponents.IMP.player.login.success);

                loginAttempts.remove(name);
                endProcess(name);
            });
        });
    }

    public void loginPlayer(Player player, Runnable callback) {
        String name = player.getUsername();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        AuthCache.setAuthenticated(name);
        database.getAuthUserRepository().updateLastLogin(name);
        database.getAuthUserRepository().updateLastIp(name, ip);
        SessionCache.addPlayer(name, ip);
        taskManager.cancelTasks(player);

        PlayerAuthEvent playerAuthEvent = new PlayerAuthEvent(player);
        plugin.getServer().getEventManager().fire(playerAuthEvent).thenAccept(firedEvent -> {
            if (firedEvent.isMoveToBackendServer()) {
                connectToBackend(player);
            }

            callback.run();
        });
    }

    public void sendAuthTitle(Player player) {
        var afterLogin = MainConfig.IMP.title.afterLogin;
        if (afterLogin.enabled) {
            var msg = CachedComponents.IMP.player.title.afterLogin;
            player.showTitle(Title.title(
                    msg.title,
                    msg.subtitle,
                    msg.fadeIn,
                    msg.stay,
                    msg.fadeOut));
        }
    }

    public void changePasswordPlayer(Player player, String oldPassword, String newPassword) {
        String name = player.getUsername();

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            player.sendMessage(CachedComponents.IMP.player.checkPassword.passwordEmpty);

            return;
        }

        if ((oldPassword.length() < MainConfig.IMP.auth.minPasswordLength || oldPassword.length() > MainConfig.IMP.auth.maxPasswordLength) ||
                (newPassword.length() < MainConfig.IMP.auth.minPasswordLength || newPassword.length() > MainConfig.IMP.auth.maxPasswordLength)) {
            player.sendMessage(
                    CachedComponents.IMP.player.checkPassword.invalidLength
                            .replaceText(builder -> builder
                                    .match(VelocityUtils.MIN)
                                    .replacement(String.valueOf(MainConfig.IMP.auth.minPasswordLength)))
                            .replaceText(builder -> builder
                                    .match(VelocityUtils.MAX)
                                    .replacement(String.valueOf(MainConfig.IMP.auth.maxPasswordLength)))
            );

            return;
        }

        if (!passwordPattern.matcher(newPassword).matches()) {
            player.sendMessage(CachedComponents.IMP.player.checkPassword.invalidPattern);

            return;
        }

        if (!beginProcess(name)) {
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> {
            if (!success) {
                player.sendMessage(CachedComponents.IMP.queryError);
                endProcess(name);
                return;
            }


            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(oldPassword, hashedPassword)) {
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
    }

    public void unregisterPlayer(Player player, String password) {
        String name = player.getUsername();

        if (!beginProcess(name)) {
            return;
        }

        if (password.length() < MainConfig.IMP.auth.minPasswordLength || password.length() > MainConfig.IMP.auth.maxPasswordLength) {
            player.sendMessage(
                    CachedComponents.IMP.player.checkPassword.invalidLength
                            .replaceText(builder -> builder
                                    .match(VelocityUtils.MIN)
                                    .replacement(String.valueOf(MainConfig.IMP.auth.minPasswordLength)))
                            .replaceText(builder -> builder
                                    .match(VelocityUtils.MAX)
                                    .replacement(String.valueOf(MainConfig.IMP.auth.maxPasswordLength)))
            );

            endProcess(name);
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> {
            if (!success) {
                player.sendMessage(CachedComponents.IMP.queryError);
                endProcess(name);
                return;
            }


            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(password, hashedPassword)) {
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

    //
    // TOTP
    //

    public boolean isTotpPending(String playerName) {
        return totpPendingPlayers.contains(playerName.toLowerCase());
    }

    public void setTotpEnableSecret(String playerName, String secret) {
        totpEnableSecrets.put(playerName.toLowerCase(), secret);
    }

    public String getTotpEnableSecret(String playerName) {
        return totpEnableSecrets.get(playerName.toLowerCase());
    }

    public void removeTotpEnableSecret(String playerName) {
        totpEnableSecrets.remove(playerName.toLowerCase());
    }

    public void verifyTotpLogin(Player player, String code) {
        String name = player.getUsername();

        if (!totpPendingPlayers.contains(name.toLowerCase())) {
            return;
        }

        if (!beginProcess(name)) {
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> {
            if (!success) {
                player.disconnect(CachedComponents.IMP.queryError);
                endProcess(name);
                return;
            }

            if (user == null) {
                totpPendingPlayers.remove(name.toLowerCase());
                player.sendMessage(CachedComponents.IMP.player.login.notRegistered);
                endProcess(name);
                return;
            }

            String totpToken = user.getTotpToken();
            if (totpToken == null || totpToken.isEmpty()) {
                totpPendingPlayers.remove(name.toLowerCase());
                loginPlayer(player, () -> {
                    player.sendMessage(CachedComponents.IMP.player.login.success);
                    endProcess(name);
                });
                return;
            }

            if (TOTP_CODE_VERIFIER.isValidCode(totpToken, code)) {
                totpPendingPlayers.remove(name.toLowerCase());
                totpAttempts.remove(name.toLowerCase());
                loginPlayer(player, () -> {
                    player.sendMessage(CachedComponents.IMP.player.login.success);
                    loginAttempts.remove(name);
                    endProcess(name);
                });
            } else {
                int attempts = totpAttempts.merge(name.toLowerCase(), 1, Integer::sum);
                if (attempts >= MainConfig.IMP.auth.totp.maxAttempts) {
                    totpPendingPlayers.remove(name.toLowerCase());
                    totpAttempts.remove(name.toLowerCase());
                    player.disconnect(CachedComponents.IMP.player.kick.tooManyAttempts);
                    if (MainConfig.IMP.auth.totp.banPlayer) {
                        String ip = player.getRemoteAddress().getAddress().getHostAddress();
                        BanCache.addPlayer(ip);
                    }
                    endProcess(name);
                    return;
                }
                player.sendMessage(CachedComponents.IMP.player.totp.wrong);
                endProcess(name);
            }
        });
    }

    //
    // Premium
    //

    public void togglePremium(Player player) {
        String name = player.getUsername();

        if (!beginProcess(name)) {
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
}
