package ru.matveylegenda.tiauth.bukkit.manager;

import lombok.Setter;
import org.bukkit.entity.Player;
import ru.matveylegenda.tiauth.bukkit.TiAuth;
import ru.matveylegenda.tiauth.bukkit.api.event.PlayerAuthEvent;
import ru.matveylegenda.tiauth.bukkit.api.event.PlayerRegisterEvent;
import ru.matveylegenda.tiauth.bukkit.storage.CachedMessages;
import ru.matveylegenda.tiauth.bukkit.util.BukkitUtils;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;

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
        String name = player.getName();

        if (!password.equals(repeatPassword)) {
            BukkitUtils.sendMessage(player, CachedMessages.IMP.player.register.mismatch);
            return;
        }

        if (password.isEmpty()) {
            BukkitUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.passwordEmpty);
            return;
        }

        if (password.length() < MainConfig.IMP.auth.minPasswordLength ||
                password.length() > MainConfig.IMP.auth.maxPasswordLength) {
            BukkitUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.invalidLength
                            .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                            .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength))
            );
            return;
        }

        if (!passwordPattern.matcher(password).matches()) {
            BukkitUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.invalidPattern);
            return;
        }

        if (!beginProcess(name)) {
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> plugin.runSync(() -> {
            if (!success) {
                player.kickPlayer(BukkitUtils.colorize(CachedMessages.IMP.queryError));
                endProcess(name);
                return;
            }

            if (user != null) {
                BukkitUtils.sendMessage(player, CachedMessages.IMP.player.register.alreadyRegistered);
                endProcess(name);
                return;
            }

            String ip = player.getAddress().getAddress().getHostAddress();

            registerPlayer(name, password, ip, success1 -> plugin.runSync(() -> {
                if (!success1) {
                    player.kickPlayer(BukkitUtils.colorize(CachedMessages.IMP.queryError));
                    endProcess(name);
                    return;
                }

                BukkitUtils.sendMessage(player, CachedMessages.IMP.player.register.success);
                AuthCache.setAuthenticated(name);

                SessionCache.addPlayer(name, ip);
                taskManager.cancelTasks(player);

                PlayerRegisterEvent playerRegisterEvent = new PlayerRegisterEvent(player);
                plugin.getServer().getPluginManager().callEvent(playerRegisterEvent);

                endProcess(name);
            }));
        }));
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
        String name = player.getName();

        if (!beginProcess(name)) {
            return;
        }

        if (password.length() < MainConfig.IMP.auth.minPasswordLength || password.length() > MainConfig.IMP.auth.maxPasswordLength) {
            BukkitUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.invalidLength
                            .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                            .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength))
            );

            endProcess(name);
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> plugin.runSync(() -> {
            if (!success) {
                BukkitUtils.sendMessage(player, CachedMessages.IMP.queryError);
                endProcess(name);
                return;
            }

            if (user == null) {
                BukkitUtils.sendMessage(player, CachedMessages.IMP.playerNotFound);
                endProcess(name);
                return;
            }

            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(password, hashedPassword)) {
                BukkitUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.wrongPassword);
                endProcess(name);
                return;
            }

            unregisterPlayer(name, success1 -> plugin.runSync(() -> {
                if (!success1) {
                    BukkitUtils.sendMessage(player, CachedMessages.IMP.queryError);
                    endProcess(name);
                    return;
                }

                SessionCache.removePlayer(name);
                player.kickPlayer(BukkitUtils.colorize(CachedMessages.IMP.player.unregister.success));
                endProcess(name);
            }));
        }));
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
        String name = player.getName();

        if (AuthCache.isAuthenticated(name)) {
            BukkitUtils.sendMessage(player, CachedMessages.IMP.player.login.alreadyLogged);
            return;
        }

        if (password.isEmpty()) {
            BukkitUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.passwordEmpty);
            return;
        }

        if (!beginProcess(name)) {
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> plugin.runSync(() -> {
            if (!success) {
                player.kickPlayer(BukkitUtils.colorize(CachedMessages.IMP.queryError));
                endProcess(name);
                return;
            }

            if (user == null) {
                BukkitUtils.sendMessage(player, CachedMessages.IMP.player.login.notRegistered);
                endProcess(name);
                return;
            }

            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(password, hashedPassword)) {
                int attempts = loginAttempts.merge(name, 1, Integer::sum);

                if (attempts >= MainConfig.IMP.auth.loginAttempts) {
                    player.kickPlayer(BukkitUtils.colorize(CachedMessages.IMP.player.kick.tooManyAttempts));

                    if (MainConfig.IMP.auth.banPlayer) {
                        BanCache.addPlayer(player.getAddress().getAddress().getHostAddress());
                    }

                    loginAttempts.remove(name);
                    endProcess(name);
                    return;
                }

                BukkitUtils.sendMessage(
                        player,
                        CachedMessages.IMP.player.login.wrongPassword
                                .replace("{attempts}", String.valueOf(MainConfig.IMP.auth.loginAttempts - attempts))
                );

                endProcess(name);
                return;
            }

            loginPlayer(player, () -> {
                BukkitUtils.sendMessage(player, CachedMessages.IMP.player.login.success);

                if (MainConfig.IMP.title.enabledOnAuth) {
                    player.sendTitle(
                            BukkitUtils.colorize(CachedMessages.IMP.player.title.onAuthTitle),
                            BukkitUtils.colorize(CachedMessages.IMP.player.title.onAuthSubTitle),
                            0, 21, 0
                    );
                }

                loginAttempts.remove(name);
                endProcess(name);
            });
        }));
    }

    public void loginPlayer(Player player, Runnable callback) {
        String name = player.getName();
        String ip = player.getAddress().getAddress().getHostAddress();

        AuthCache.setAuthenticated(name);
        database.getAuthUserRepository().updateLastLogin(name);
        database.getAuthUserRepository().updateLastIp(name, ip);
        SessionCache.addPlayer(name, ip);
        taskManager.cancelTasks(player);

        PlayerAuthEvent playerAuthEvent = new PlayerAuthEvent(player);
        plugin.getServer().getPluginManager().callEvent(playerAuthEvent);

        callback.run();
    }

    public void changePasswordPlayer(Player player, String oldPassword, String newPassword) {
        String name = player.getName();

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            BukkitUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.passwordEmpty);

            return;
        }

        if ((oldPassword.length() < MainConfig.IMP.auth.minPasswordLength || oldPassword.length() > MainConfig.IMP.auth.maxPasswordLength) ||
                (newPassword.length() < MainConfig.IMP.auth.minPasswordLength || newPassword.length() > MainConfig.IMP.auth.maxPasswordLength)) {
            BukkitUtils.sendMessage(
                    player,
                    CachedMessages.IMP.player.checkPassword.invalidLength
                            .replace("{min}", String.valueOf(MainConfig.IMP.auth.minPasswordLength))
                            .replace("{max}", String.valueOf(MainConfig.IMP.auth.maxPasswordLength))
            );

            return;
        }

        if (!passwordPattern.matcher(newPassword).matches()) {
            BukkitUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.invalidPattern);

            return;
        }

        if (!beginProcess(name)) {
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> plugin.runSync(() -> {
            if (!success) {
                BukkitUtils.sendMessage(player, CachedMessages.IMP.queryError);
                endProcess(name);
                return;
            }

            if (user == null) {
                BukkitUtils.sendMessage(player, CachedMessages.IMP.playerNotFound);
                endProcess(name);
                return;
            }

            String hashedPassword = user.getPassword();

            if (!hash.verifyPassword(oldPassword, hashedPassword)) {
                BukkitUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.wrongPassword);
                endProcess(name);
                return;
            }

            changePasswordPlayer(name, newPassword, success1 -> plugin.runSync(() -> {
                if (!success1) {
                    BukkitUtils.sendMessage(player, CachedMessages.IMP.queryError);
                    endProcess(name);
                    return;
                }

                BukkitUtils.sendMessage(player, CachedMessages.IMP.player.changePassword.success);

                endProcess(name);
            }));
        }));
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
        AuthCache.logout(player.getName());
        SessionCache.removePlayer(player.getName());
    }

    public void forceAuth(Player player) {
        String ip = player.getAddress().getAddress().getHostAddress();

        database.getAuthUserRepository().getUser(player.getName(), (user, success) -> plugin.runSync(() -> {
            if (!success) {
                player.kickPlayer(BukkitUtils.colorize(CachedMessages.IMP.queryError));
                return;
            }

            if (user != null && !player.getName().equals(user.getRealName())) {
                player.kickPlayer(BukkitUtils.colorize(CachedMessages.IMP.player.kick.realname
                        .replace("{realname}", user.getRealName())
                        .replace("{name}", player.getName())));
                return;
            }

            if (user == null && !MainConfig.IMP.excludedIps.contains(ip)) {
                database.getAuthUserRepository().getUserCountByIp(ip, count -> plugin.runSync(() -> {
                    if (count >= MainConfig.IMP.maxRegisteredAccountsPerIp) {
                        player.kickPlayer(BukkitUtils.colorize(CachedMessages.IMP.player.kick.ipLimitRegisteredReached));
                    } else {
                        startAuthProcess(player, null);
                    }
                }));
                return;
            }

            startAuthProcess(player, user);
        }));
    }

    private void startAuthProcess(Player player, AuthUser user) {
        String name = player.getName();
        String sessionIP = SessionCache.getIP(name);
        String remoteIp = player.getAddress().getAddress().getHostAddress();

        if (sessionIP != null && sessionIP.equals(remoteIp)) {
            AuthCache.setAuthenticated(name);
            return;
        }

        String reminderMessage = (user != null)
                ? CachedMessages.IMP.player.reminder.login
                : CachedMessages.IMP.player.reminder.register;

        taskManager.startAuthTimeoutTask(player);
        taskManager.startAuthReminderTask(player, reminderMessage);
        taskManager.startDisplayTimerTask(player);
    }

    private boolean beginProcess(String playerName) {
        if (!inProcess.add(playerName)) {
            Player p = plugin.getServer().getPlayer(playerName);
            if (p != null) {
                BukkitUtils.sendMessage(p, CachedMessages.IMP.processing);
            }
            return false;
        }

        return true;
    }

    private void endProcess(String playerName) {
        inProcess.remove(playerName);
    }
}
