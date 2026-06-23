package ru.matveylegenda.tiauth.bungee.manager;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.cache.BanCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.hash.Hash;
import ru.matveylegenda.tiauth.hash.HashFactory;
import ru.matveylegenda.tiauth.hash.HashType;
import ru.matveylegenda.tiauth.util.EncryptionUtils;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
        totpEnableSecrets.put(playerName.toLowerCase(), secret);
    }

    public String getTotpEnableSecret(String playerName) {
        return totpEnableSecrets.get(playerName.toLowerCase());
    }

    public void removeTotpEnableSecret(String playerName) {
        totpEnableSecrets.remove(playerName.toLowerCase());
    }

    public void verifyTotpLogin(ProxiedPlayer player, String code) {
        String name = player.getName();
        String lowerName = name.toLowerCase();

        if (!totpPendingPlayers.contains(lowerName)) {
            return;
        }

        if (tryBeginProcess(name)) {
            return;
        }

        database.getAuthUserRepository().getUser(name, (user, success) -> {
            if (!success) {
                player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.queryError));
                endProcess(name);
                return;
            }

            if (user == null) {
                totpPendingPlayers.remove(lowerName);
                BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.notRegistered);
                endProcess(name);
                return;
            }

            if (user.getTotpToken() == null || user.getTotpToken().isEmpty()) {
                totpPendingPlayers.remove(lowerName);
                authManager.loginPlayer(player, () -> {
                    BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.success);
                    endProcess(name);
                });
                return;
            }

            String totpToken;
            try {
                totpToken = EncryptionUtils.decrypt(user.getTotpToken(), plugin.getSecretKey());
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error during secret decryption", e);
                endProcess(name);
                return;
            }

            if (RECOVERY_CODE_PATTERN.matcher(code).matches()) {
                verifyRecoveryCodeLogin(player, name, code);
            } else if (TOTP_CODE_VERIFIER.isValidCode(totpToken, code)) {
                completeTotpLogin(player, name);
            } else {
                handleWrongTotpAttempt(player, name);
            }
        });
    }

    public boolean isTotpLoginRequired(ProxiedPlayer player, AuthUser user) {
        String totpToken = user.getTotpToken();
        if (MainConfig.IMP.auth.totp.enabled && totpToken != null && !totpToken.isEmpty()) {
            String lowerName = player.getName().toLowerCase(Locale.ROOT);
            totpPendingPlayers.add(lowerName);
            plugin.getTaskManager().cancelTasks(player);
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.prompt);
            return true;
        }
        return false;
    }

    private void completeTotpLogin(ProxiedPlayer player, String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        totpPendingPlayers.remove(lowerName);
        totpAttempts.remove(lowerName);

        authManager.loginPlayer(player, () -> {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.success);
            authManager.clearLoginAttempts(lowerName);
            endProcess(name);
        });
    }

    private void handleWrongTotpAttempt(ProxiedPlayer player, String name) {
        String lowerName = name.toLowerCase();
        int attempts = totpAttempts.merge(lowerName, 1, Integer::sum);
        if (attempts >= MainConfig.IMP.auth.totp.maxAttempts) {
            totpPendingPlayers.remove(lowerName);
            totpAttempts.remove(lowerName);
            player.disconnect(TextComponent.fromLegacy(CachedMessages.IMP.player.kick.tooManyAttempts));
            if (MainConfig.IMP.auth.totp.banPlayer) {
                BanCache.addPlayer(((InetSocketAddress) player.getSocketAddress()).getAddress().getHostAddress());
            }
            endProcess(name);
            return;
        }
        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.wrong);
        endProcess(name);
    }

    private void verifyRecoveryCodeLogin(ProxiedPlayer player, String name, String code) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        String hashedCode = RECOVERY_HASH.hashPassword(code);

        database.getRecoveryCodeRepository().getRecoveryCode(hashedCode, (recoveryCode, success1) -> {
            if (!success1) {
                BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                endProcess(name);
                return;
            }

            if (recoveryCode != null && recoveryCode.getUsername().equalsIgnoreCase(lowerName)) {
                database.getRecoveryCodeRepository().removeCode(hashedCode, success2 -> {
                    if (!success2) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                        endProcess(name);
                        return;
                    }
                    completeTotpLogin(player, name);
                });
            } else {
                handleWrongTotpAttempt(player, name);
            }
        });
    }

    private boolean tryBeginProcess(String playerName) {
        return !inProcess.add(playerName);
    }

    private void endProcess(String playerName) {
        inProcess.remove(playerName);
    }
}
