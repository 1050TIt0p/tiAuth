package ru.matveylegenda.tiauth.velocity.manager;

import com.velocitypowered.api.proxy.Player;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.database.model.RecoveryCode;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;
import ru.matveylegenda.tiauth.hash.HashType;
import ru.matveylegenda.tiauth.util.EncryptionUtils;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class TotpManager {
    public static final CodeVerifier TOTP_CODE_VERIFIER = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    public static final Pattern RECOVERY_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}$");
    public static final Hash RECOVERY_HASH = HashFactory.create(HashType.SHA256_DEFAULT);

    private final AuthManager authManager;
    private final TiAuth plugin;
    private final Database database;

    private final Set<String> inProcess = ConcurrentHashMap.newKeySet();
    private final Set<String> totpPendingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> totpAttempts = new ConcurrentHashMap<>();
    private final Map<String, String> totpEnableSecrets = new ConcurrentHashMap<>();

    public TotpManager(AuthManager authManager, TiAuth plugin) {
        this.authManager = authManager;
        this.plugin = plugin;
        this.database = plugin.getDatabase();
    }

    public boolean isTotpPending(String playerName) {
        String lowerName = playerName.toLowerCase(Locale.ROOT);
        return totpPendingPlayers.contains(lowerName);
    }

    public void setTotpEnableSecret(String playerName, String secret) {
        totpEnableSecrets.put(playerName.toLowerCase(Locale.ROOT), secret);
    }

    public String getTotpEnableSecret(String playerName) {
        return totpEnableSecrets.get(playerName.toLowerCase(Locale.ROOT));
    }

    public void removeTotpEnableSecret(String playerName) {
        totpEnableSecrets.remove(playerName.toLowerCase(Locale.ROOT));
    }

    public void clearTotpState(String playerName) {
        String lowerName = playerName.toLowerCase(Locale.ROOT);
        totpPendingPlayers.remove(lowerName);
        totpAttempts.remove(lowerName);
        inProcess.remove(playerName);
    }

    public void processTotpChallenge(Player player, String code) {
        String name = player.getUsername();
        String lowerName = name.toLowerCase();

        if (!totpPendingPlayers.contains(lowerName)) {
            return;
        }

        if (!inProcess.add(name)) {
            return;
        }

        getUserAsync(name)
                .thenCompose(user -> {
                    if (user == null) {
                        totpPendingPlayers.remove(lowerName);
                        player.sendMessage(CachedComponents.IMP.player.login.notRegistered);
                        return CompletableFuture.completedFuture(null);
                    }

                    if (user.getTotpToken() == null || user.getTotpToken().isEmpty()) {
                        totpPendingPlayers.remove(lowerName);
                        authManager.loginPlayer(player, () ->
                                player.sendMessage(CachedComponents.IMP.player.login.success)
                        );
                        return CompletableFuture.completedFuture(null);
                    }

                    String totpToken;
                    try {
                        totpToken = EncryptionUtils.decrypt(user.getTotpToken(), plugin.getSecretKey());
                    } catch (Exception e) {
                        plugin.getLogger().error("Error during secret decryption", e);
                        return CompletableFuture.completedFuture(null);
                    }

                    if (RECOVERY_CODE_PATTERN.matcher(code).matches()) {
                        return processRecoveryCodeAsync(player, name, code);
                    } else if (TOTP_CODE_VERIFIER.isValidCode(totpToken, code)) {
                        return completeTotpLoginAsync(player, name);
                    } else {
                        handleWrongTotpAttempt(player, name);
                        return CompletableFuture.completedFuture(null);
                    }
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        player.disconnect(CachedComponents.IMP.queryError);
                    }
                    inProcess.remove(name);
                });
    }

    public boolean requireTotpChallenge(Player player, AuthUser user) {
        String totpToken = user.getTotpToken();
        if (MainConfig.IMP.auth.totp.enabled && totpToken != null && !totpToken.isEmpty()) {
            String lowerName = player.getUsername().toLowerCase(Locale.ROOT);
            totpPendingPlayers.add(lowerName);
            plugin.getTaskManager().cancelTasks(player);
            plugin.getTaskManager().startTotpTimeoutTask(player);
            plugin.getTaskManager().startDisplayTimerTask(player, MainConfig.IMP.auth.totp.timeoutSeconds);
            player.sendMessage(CachedComponents.IMP.player.totp.prompt);
            return true;
        }
        return false;
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

    private CompletableFuture<RecoveryCode> getRecoveryCodeAsync(String code) {
        CompletableFuture<RecoveryCode> future = new CompletableFuture<>();
        database.getRecoveryCodeRepository().getRecoveryCode(code, (recoveryCode, success) -> {
            if (success) {
                future.complete(recoveryCode);
            } else {
                future.completeExceptionally(new RuntimeException("Database query error"));
            }
        });
        return future;
    }

    private CompletableFuture<Void> removeRecoveryCodeAsync(String code) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        database.getRecoveryCodeRepository().removeCode(code, success -> {
            if (success) {
                future.complete(null);
            } else {
                future.completeExceptionally(new RuntimeException("Database remove error"));
            }
        });
        return future;
    }

    private CompletableFuture<Void> completeTotpLoginAsync(Player player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        totpPendingPlayers.remove(lowerName);
        totpAttempts.remove(lowerName);

        authManager.loginPlayer(player, () -> {
            player.sendMessage(CachedComponents.IMP.player.login.success);
            authManager.resetLoginAttempts(lowerName);
        });

        return CompletableFuture.completedFuture(null);
    }

    private void handleWrongTotpAttempt(Player player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        int attempts = totpAttempts.merge(lowerName, 1, Integer::sum);
        if (attempts >= MainConfig.IMP.auth.totp.maxAttempts) {
            totpPendingPlayers.remove(lowerName);
            totpAttempts.remove(lowerName);
            player.disconnect(CachedComponents.IMP.player.kick.totpTooManyAttempts);
            if (MainConfig.IMP.auth.totp.banPlayer) {
                BanCache.addTotpBan(player.getRemoteAddress().getAddress().getHostAddress());
            }
            return;
        }
        player.sendMessage(CachedComponents.IMP.player.totp.wrong);
    }

    private CompletableFuture<Void> processRecoveryCodeAsync(Player player, String name, String code) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        String hashedCode = RECOVERY_HASH.hashPassword(code);

        return getRecoveryCodeAsync(hashedCode)
                .thenCompose(recoveryCode -> {
                    if (recoveryCode != null && recoveryCode.getUsername().equalsIgnoreCase(lowerName)) {
                        return removeRecoveryCodeAsync(hashedCode)
                                .thenCompose(result -> completeTotpLoginAsync(player, name));
                    } else {
                        handleWrongTotpAttempt(player, name);
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }
}
