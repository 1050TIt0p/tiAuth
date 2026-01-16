package ru.matveylegenda.tiauth.velocity.manager;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Setter;
import net.kyori.adventure.text.Component;
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

import java.util.Map;
import java.util.Set;
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
                });

                endProcess(name);
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

    public void unregisterPlayer(Player player, String password) {
        String name = player.getUsername();

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
        });

        callback.run();
    }

    public void changePasswordPlayer(Player player, String oldPassword, String newPassword) {
        String name = player.getUsername();

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            player.sendMessage(CachedComponents.IMP.player.checkPassword.passwordEmpty);

            return;
        }

        if (newPassword.length() < MainConfig.IMP.auth.minPasswordLength || newPassword.length() > MainConfig.IMP.auth.maxPasswordLength) {
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

    /**
     * Форсированный аутентификационный путь (для использования извне).
     */
    public void forceAuth(Player player) {
        String name = player.getUsername();

        database.getAuthUserRepository().getUser(name, (user, success) -> {
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
                connectToBackend(player);
                return;
            }

            // подключаем к auth-серверу
            connectToAuthServer(player);

            Component reminderMessage = (user != null)
                    ? CachedComponents.IMP.player.reminder.login
                    : CachedComponents.IMP.player.reminder.register;

            taskManager.startAuthTimeoutTask(player);
            taskManager.startAuthReminderTask(player, reminderMessage);
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

    private void connectToBackend(Player player) {
        java.util.Optional<RegisteredServer> serverOpt = plugin.getServer().getServer(MainConfig.IMP.servers.backend);
        if (serverOpt.isEmpty()) {
            return;
        }

        RegisteredServer backend = serverOpt.get();

        player.getCurrentServer().ifPresentOrElse(current -> {
            if (!current.getServer().equals(backend)) {
                player.createConnectionRequest(backend).connect();
            }
        }, () -> player.createConnectionRequest(backend).connect());
    }

    private boolean supportDialog(Player player) {
        return false;
    }

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
}
