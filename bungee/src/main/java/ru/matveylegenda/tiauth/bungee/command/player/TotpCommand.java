package ru.matveylegenda.tiauth.bungee.command.player;

import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Level;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.bungee.manager.TotpManager;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.util.EncryptionUtils;

public class TotpCommand extends Command {
    private final AuthManager authManager;
    private final TotpManager totpManager;
    private final Database database;
    private final TiAuth plugin;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final RecoveryCodeGenerator codesGenerator = new RecoveryCodeGenerator();

    public TotpCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.plugin = plugin;
        this.authManager = plugin.getAuthManager();
        this.totpManager = plugin.getTotpManager();
        this.database = plugin.getDatabase();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.onlyPlayer);
            return;
        }

        String name = player.getName();

        if (args.length == 1 && !args[0].equalsIgnoreCase("enable") && !args[0].equalsIgnoreCase("disable")) {
            if (totpManager.isTotpPending(name)) {
                totpManager.processTotpChallenge(player, args[0]);
                return;
            }
        }

        if (!player.hasPermission("tiauth.player.2fa")) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.noPermission);
            return;
        }

        if (args.length == 0) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.usage);
            return;
        }

        if (args[0].equalsIgnoreCase("enable")) {
            handleEnable(player, args);
        } else if (args[0].equalsIgnoreCase("verify")) {
            handleVerify(player, args);
        } else if (args[0].equalsIgnoreCase("disable")) {
            handleDisable(player, args);
        } else {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.usage);
        }
    }

    private void handleEnable(ProxiedPlayer player, String[] args) {
        String name = player.getName();

        if (MainConfig.IMP.auth.totp.needPassword) {
            if (args.length != 2) {
                BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.enableUsage);
                return;
            }
        } else {
            if (args.length != 1) {
                BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.enableUsage);
                return;
            }
        }

        plugin.getDatabase().getAuthUserRepository().getUser(name)
                .thenCompose(user -> {
                    if (user == null) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.notRegistered);
                        return null;
                    }

                    if (user.getTotpToken() != null && !user.getTotpToken().isEmpty()) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.alreadyEnabled);
                        return null;
                    }

                    if (MainConfig.IMP.auth.totp.needPassword) {
                        String password = args[1];
                        if (!authManager.getHash().verifyPassword(password, user.getPassword())) {
                            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.checkPassword.wrongPassword);
                            return null;
                        }
                    }

                    String secret = secretGenerator.generate();
                    totpManager.setTotpEnableSecret(name, secret);

                    QrData qrData = new QrData.Builder()
                            .label(name)
                            .secret(secret)
                            .issuer(MainConfig.IMP.auth.totp.issuer)
                            .build();
                    String qrUrl = MainConfig.IMP.auth.totp.qrGeneratorUrl.replace("{data}",
                            URLEncoder.encode(qrData.getUri(), StandardCharsets.UTF_8));

                    BaseComponent qrMessage = TextComponent.fromLegacy(CachedMessages.IMP.player.totp.qr);
                    qrMessage.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, qrUrl));
                    player.sendMessage(qrMessage);

                    String tokenMsg = CachedMessages.IMP.player.totp.token.replace("{0}", secret);
                    BaseComponent tokenMessage = TextComponent.fromLegacy(tokenMsg);
                    tokenMessage.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, secret));
                    player.sendMessage(tokenMessage);

                    String[] codes = codesGenerator.generateCodes(MainConfig.IMP.auth.totp.recoveryCodesAmount);
                    String[] hashedCodes = new String[codes.length];

                    for (int i = 0; i < codes.length; i++) {
                        hashedCodes[i] = TotpManager.RECOVERY_HASH.hashPassword(codes[i]);
                    }

                    return database.getRecoveryCodeRepository().addCodes(hashedCodes, player.getName())
                            .thenAccept(result -> {
                                String codesStr = String.join(", ", codes);
                                String recoveryMsg = CachedMessages.IMP.player.totp.recovery.replace("{0}", codesStr);
                                BaseComponent recoveryMessage = TextComponent.fromLegacy(recoveryMsg);
                                recoveryMessage.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, codesStr));
                                player.sendMessage(recoveryMessage);

                                BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.verified);
                            });
                })
                .exceptionally(throwable -> {
                    BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                    return null;
                });
    }

    private void handleVerify(ProxiedPlayer player, String[] args) {
        String name = player.getName();

        if (args.length != 2) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.verifyUsage);
            return;
        }

        String secret = totpManager.getTotpEnableSecret(name);
        if (secret == null) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.enableUsage);
            return;
        }

        String secretEncrypted;

        try {
            secretEncrypted = EncryptionUtils.encrypt(secret, plugin.getSecretKey());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during secret encryption", e);
            return;
        }

        if (TotpManager.TOTP_CODE_VERIFIER.isValidCode(secret, args[1])) {
            plugin.getDatabase().getAuthUserRepository().updateTotpToken(name, secretEncrypted)
                    .thenAccept(result -> {
                        totpManager.removeTotpEnableSecret(name);
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.successful);
                    })
                    .exceptionally(throwable -> {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                        return null;
                    });
        } else {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.wrong);
        }
    }

    private void handleDisable(ProxiedPlayer player, String[] args) {
        if (args.length != 2) {
            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.disableUsage);
            return;
        }

        plugin.getDatabase().getAuthUserRepository().getUser(player.getName())
                .thenCompose(user -> {
                    if (user == null) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.login.notRegistered);
                        return null;
                    }

                    if (user.getTotpToken() == null || user.getTotpToken().isEmpty()) {
                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.alreadyDisabled);
                        return null;
                    }

                    String totpToken;

                    try {
                        totpToken = EncryptionUtils.decrypt(user.getTotpToken(), plugin.getSecretKey());
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error during secret decryption", e);
                        return null;
                    }

                    if (TotpManager.RECOVERY_CODE_PATTERN.matcher(args[1]).matches()) {
                        String hashedCode = TotpManager.RECOVERY_HASH.hashPassword(args[1]);

                        return database.getRecoveryCodeRepository().getRecoveryCode(hashedCode)
                                .thenCompose(recoveryCode -> {
                                    if (recoveryCode != null && recoveryCode.getUsername().equals(player.getName().toLowerCase(Locale.ROOT))) {
                                        return database.getRecoveryCodeRepository().removeCodesByUsername(player.getName())
                                                .thenCompose(v -> plugin.getDatabase().getAuthUserRepository().updateTotpToken(player.getName(), ""))
                                                .thenAccept(v -> BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.disabled));
                                    } else {
                                        BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.wrong);
                                        return null;
                                    }
                                });
                    } else {
                        if (TotpManager.TOTP_CODE_VERIFIER.isValidCode(totpToken, args[1])) {
                            return database.getAuthUserRepository().updateTotpToken(player.getName(), "")
                                    .thenAccept(v -> BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.disabled));
                        } else {
                            BungeeUtils.sendMessage(player, CachedMessages.IMP.player.totp.wrong);
                            return null;
                        }
                    }
                })
                .exceptionally(throwable -> {
                    BungeeUtils.sendMessage(player, CachedMessages.IMP.queryError);
                    return null;
                });
    }
}
