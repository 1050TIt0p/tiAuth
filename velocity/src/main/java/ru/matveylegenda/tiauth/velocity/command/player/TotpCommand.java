package ru.matveylegenda.tiauth.velocity.command.player;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

public class TotpCommand implements SimpleCommand {
    private final AuthManager authManager;
    private final TiAuth plugin;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final RecoveryCodeGenerator codesGenerator = new RecoveryCodeGenerator();

    public TotpCommand(TiAuth plugin) {
        this.plugin = plugin;
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (!(sender instanceof Player player)) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.onlyPlayer);
            return;
        }

        String name = player.getUsername();

        // TOTP verification during login - no permission needed
        if (args.length == 1 && !args[0].equalsIgnoreCase("enable") && !args[0].equalsIgnoreCase("disable")) {
            if (authManager.isTotpPending(name)) {
                authManager.verifyTotpLogin(player, args[0]);
                return;
            }
        }

        if (!player.hasPermission("tiauth.player.2fa")) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.noPermission);
            return;
        }

        // TOTP management commands
        if (args.length == 0) {
            player.sendMessage(CachedComponents.IMP.player.totp.usage);
            return;
        }

        if (args[0].equalsIgnoreCase("enable")) {
            handleEnable(player, args);
        } else if (args[0].equalsIgnoreCase("verify")) {
            handleVerify(player, args);
        } else if (args[0].equalsIgnoreCase("disable")) {
            handleDisable(player, args);
        } else {
            player.sendMessage(CachedComponents.IMP.player.totp.usage);
        }
    }

    private void handleEnable(Player player, String[] args) {
        String name = player.getUsername();

        if (MainConfig.IMP.auth.totp.needPassword) {
            if (args.length != 2) {
                player.sendMessage(CachedComponents.IMP.player.totp.enableUsage);
                return;
            }
        } else {
            if (args.length != 1) {
                player.sendMessage(CachedComponents.IMP.player.totp.enableUsage);
                return;
            }
        }

        plugin.getDatabase().getAuthUserRepository().getUser(name, (user, success) -> {
            if (!success) {
                player.sendMessage(CachedComponents.IMP.queryError);
                return;
            }

            if (user == null) {
                player.sendMessage(CachedComponents.IMP.player.login.notRegistered);
                return;
            }

            if (user.getTotpToken() != null && !user.getTotpToken().isEmpty()) {
                player.sendMessage(CachedComponents.IMP.player.totp.alreadyEnabled);
                return;
            }

            if (MainConfig.IMP.auth.totp.needPassword) {
                String password = args[1];
                if (!authManager.getHash().verifyPassword(password, user.getPassword())) {
                    player.sendMessage(CachedComponents.IMP.player.checkPassword.wrongPassword);
                    return;
                }
            }

            String secret = secretGenerator.generate();
            authManager.setTotpEnableSecret(name, secret);

            QrData qrData = new QrData.Builder()
                    .label(name)
                    .secret(secret)
                    .issuer(MainConfig.IMP.auth.totp.issuer)
                    .build();
            String qrUrl = MainConfig.IMP.auth.totp.qrGeneratorUrl.replace("{data}",
                    URLEncoder.encode(qrData.getUri(), StandardCharsets.UTF_8));
            player.sendMessage(CachedComponents.IMP.player.totp.qr.clickEvent(ClickEvent.openUrl(qrUrl)));

            Component tokenComponent = CachedComponents.IMP.player.totp.token
                    .replaceText(builder -> builder
                            .match(java.util.regex.Pattern.compile("\\{0}"))
                            .replacement(secret));
            player.sendMessage(tokenComponent.clickEvent(ClickEvent.copyToClipboard(secret)));

            String[] codes = codesGenerator.generateCodes(MainConfig.IMP.auth.totp.recoveryCodesAmount);
            String codesStr = String.join(", ", codes);
            Component recoveryComponent = CachedComponents.IMP.player.totp.recovery
                    .replaceText(builder -> builder
                            .match(java.util.regex.Pattern.compile("\\{0}"))
                            .replacement(codesStr));
            player.sendMessage(recoveryComponent.clickEvent(ClickEvent.copyToClipboard(codesStr)));

            player.sendMessage(CachedComponents.IMP.player.totp.verified);
        });
    }

    private void handleVerify(Player player, String[] args) {
        String name = player.getUsername();

        if (args.length != 2) {
            player.sendMessage(CachedComponents.IMP.player.totp.verifyUsage);
            return;
        }

        String secret = authManager.getTotpEnableSecret(name);
        if (secret == null) {
            player.sendMessage(CachedComponents.IMP.player.totp.enableUsage);
            return;
        }

        if (AuthManager.TOTP_CODE_VERIFIER.isValidCode(secret, args[1])) {
            plugin.getDatabase().getAuthUserRepository().updateTotpToken(name, secret, updateSuccess -> {
                if (!updateSuccess) {
                    player.sendMessage(CachedComponents.IMP.queryError);
                    return;
                }
                authManager.removeTotpEnableSecret(name);
                player.sendMessage(CachedComponents.IMP.player.totp.successful);
            });
        } else {
            player.sendMessage(CachedComponents.IMP.player.totp.wrong);
        }
    }

    private void handleDisable(Player player, String[] args) {
        String name = player.getUsername();

        if (args.length != 2) {
            player.sendMessage(CachedComponents.IMP.player.totp.disableUsage);
            return;
        }

        plugin.getDatabase().getAuthUserRepository().getUser(name, (user, success) -> {
            if (!success) {
                player.sendMessage(CachedComponents.IMP.queryError);
                return;
            }

            if (user == null) {
                player.sendMessage(CachedComponents.IMP.player.login.notRegistered);
                return;
            }

            String totpToken = user.getTotpToken();
            if (totpToken == null || totpToken.isEmpty()) {
                player.sendMessage(CachedComponents.IMP.player.totp.alreadyDisabled);
                return;
            }

            if (AuthManager.TOTP_CODE_VERIFIER.isValidCode(totpToken, args[1])) {
                plugin.getDatabase().getAuthUserRepository().updateTotpToken(name, "", updateSuccess -> {
                    if (!updateSuccess) {
                        player.sendMessage(CachedComponents.IMP.queryError);
                        return;
                    }
                    player.sendMessage(CachedComponents.IMP.player.totp.disabled);
                });
            } else {
                player.sendMessage(CachedComponents.IMP.player.totp.wrong);
            }
        });
    }
}
