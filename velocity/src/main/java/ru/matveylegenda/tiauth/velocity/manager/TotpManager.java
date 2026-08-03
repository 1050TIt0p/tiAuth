package ru.matveylegenda.tiauth.velocity.manager;

import com.velocitypowered.api.proxy.Player;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;
import ru.matveylegenda.tiauth.hash.HashType;
import ru.matveylegenda.tiauth.util.EncryptionUtils;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.util.PlayerLock;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class TotpManager {
    public static final CodeVerifier TOTP_CODE_VERIFIER = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    public static final Pattern RECOVERY_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}$");
    public static final Hash RECOVERY_HASH = HashFactory.create(HashType.SHA256_DEFAULT);

    private final AuthManager authManager;
    private final TiAuth plugin;
    private final Database database;

    private final PlayerLock playerLock = new PlayerLock();

    public TotpManager(AuthManager authManager, TiAuth plugin) {
        this.authManager = authManager;
        this.plugin = plugin;
        this.database = plugin.getDatabase();
    }

    public void clearTotpState(String playerName) {
        String lowerName = playerName.toLowerCase(Locale.ROOT);
        AuthCache.clearTotpPending(playerName);
        AuthCache.endVerification(playerName);
        AuthCache.resetTotpAttempts(lowerName);
        playerLock.unlock(playerName);
    }

    public void processTotpChallenge(Player player, String code) {
        String name = player.getUsername();

        if (!AuthCache.isTotpPending(name)) {
            return;
        }

        playerLock.execute(name, () -> database.getAuthUserRepository().getUser(name)
                    .thenCompose(user -> {
                        if (user == null) {
                            AuthCache.clearTotpPending(name);
                            player.sendMessage(CachedComponents.IMP.player.login.notRegistered);
                            return CompletableFuture.completedFuture(null);
                        }

                        if (user.getTotpToken() == null || user.getTotpToken().isEmpty()) {
                            AuthCache.clearTotpPending(name);
                            return authManager.loginPlayer(player, false)
                                    .thenRun(() -> player.sendMessage(CachedComponents.IMP.player.login.success));
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
                            processFailedTotpAttempt(player, name);
                            return CompletableFuture.completedFuture(null);
                        }
                    })
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            player.disconnect(CachedComponents.IMP.queryError);
                        }
                    }));
    }

    public boolean requireTotpChallenge(Player player, AuthUser user) {
        String totpToken = user.getTotpToken();
        if (MainConfig.IMP.auth.totp.enabled && totpToken != null && !totpToken.isEmpty()) {
            AuthCache.setTotpPending(player.getUsername());
            plugin.getTaskManager().cancelTasks(player);
            plugin.getTaskManager().startTotpTimeoutTask(player);
            plugin.getTaskManager().startDisplayTimerTask(player, MainConfig.IMP.auth.totp.timeoutSeconds);
            player.sendMessage(CachedComponents.IMP.player.totp.prompt);
            return true;
        }
        return false;
    }

    private CompletableFuture<Void> completeTotpLoginAsync(Player player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        AuthCache.clearTotpPending(name);
        AuthCache.resetTotpAttempts(lowerName);

        return authManager.loginPlayer(player, false)
                .thenRun(() -> player.sendMessage(CachedComponents.IMP.player.login.success));
    }

    private void processFailedTotpAttempt(Player player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        int attempts = AuthCache.incrementTotpAttempts(lowerName);
        if (attempts >= MainConfig.IMP.auth.totp.maxAttempts) {
            AuthCache.clearTotpPending(name);
            AuthCache.resetTotpAttempts(lowerName);
            player.disconnect(CachedComponents.IMP.player.kick.totpTooManyAttempts);
            if (MainConfig.IMP.auth.totp.banPlayer) {
                BanCache.addTotpBan(VelocityUtils.getIp(player));
            }
            return;
        }
        player.sendMessage(CachedComponents.IMP.player.totp.wrong);
    }

    private CompletableFuture<Void> processRecoveryCodeAsync(Player player, String name, String code) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        String hashedCode = RECOVERY_HASH.hashPassword(code);

        return database.getRecoveryCodeRepository().getRecoveryCode(hashedCode)
                .thenCompose(recoveryCode -> {
                    if (recoveryCode != null && recoveryCode.getUsername().equalsIgnoreCase(lowerName)) {
                        return database.getRecoveryCodeRepository().removeCode(hashedCode)
                                .thenCompose(result -> completeTotpLoginAsync(player, name));
                    } else {
                        processFailedTotpAttempt(player, name);
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }
}
