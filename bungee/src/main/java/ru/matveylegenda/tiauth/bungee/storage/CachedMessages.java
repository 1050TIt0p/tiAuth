package ru.matveylegenda.tiauth.bungee.storage;

import ru.matveylegenda.tiauth.config.MessagesConfig;

import static ru.matveylegenda.tiauth.util.Utils.COLORIZER;

public class CachedMessages {

    public static CachedMessages IMP = new CachedMessages(MessagesConfig.IMP);

    public CachedMessages(MessagesConfig messagesConfig) {
        load(messagesConfig);
    }

    public String onlyPlayer;
    public String queryError;
    public String processing;
    public String playerNotFound;
    public String noPermission;
    public Admin admin;
    public Player player;

    public static class Admin {
        public String usage;
        public Config config;
        public Unregister unregister;
        public ChangePassword changePassword;
        public ForceLogin forceLogin;
        public ForceRegister forceRegister;
        public ForcePremium forcePremium;
        public Migrate migrate;
        public Backup backup;

        public static class Config {
            public String reload;
        }

        public static class Unregister {
            public String usage;
            public String success;
        }

        public static class ChangePassword {
            public String usage;
            public String success;
        }

        public static class ForceLogin {
            public String usage;
            public String isAuthenticated;
            public String success;
        }

        public static class ForceRegister {
            public String usage;
            public String alreadyRegistered;
            public String success;
        }

        public static class ForcePremium {
            public String usage;
            public String enabled;
            public String disabled;
        }

        public static class Migrate {
            public String usage;
            public String error;
            public String invalidFileName;
            public String success;
        }

        public static class Backup {
            public String usage;
            public String invalidFileName;
            public String invalidCompression;
            public String alreadyExists;
            public String notFound;
            public String creating;
            public String createSuccess;
            public String createError;
            public String restoring;
            public String restoreSuccess;
            public String restoreError;
        }
    }

    public static class Player {
        public CheckPassword checkPassword;
        public Register register;
        public Unregister unregister;
        public Login login;
        public ChangePassword changePassword;
        public Logout logout;
        public Totp totp;
        public Premium premium;
        public Kick kick;
        public Reminder reminder;
        public Dialog dialog;
        public BossBar bossBar;
        public Title title;
        public ActionBar actionBar;

        public static class CheckPassword {
            public String wrongPassword;
            public String invalidLength;
            public String invalidPattern;
            public String passwordEmpty;
        }

        public static class Register {
            public String usage;
            public String mismatch;
            public String alreadyRegistered;
            public String success;
        }

        public static class Unregister {
            public String usage;
            public String success;
        }

        public static class Login {
            public String usage;
            public String notRegistered;
            public String alreadyLogged;
            public String wrongPassword;
            public String success;
        }

        public static class ChangePassword {
            public String usage;
            public String success;
        }

        public static class Logout {
            public String logoutByPremium;
            public String success;
        }

        public static class Totp {
            public String usage;
            public String enableUsage;
            public String verifyUsage;
            public String disableUsage;
            public String successful;
            public String verified;
            public String disabled;
            public String wrong;
            public String alreadyEnabled;
            public String alreadyDisabled;
            public String qr;
            public String token;
            public String recovery;
            public String needPassword;
            public String prompt;
        }

        public static class Premium {
            public String enabled;
            public String disabled;
        }

        public static class Kick {
            public String timeout;
            public String realname;
            public String tooManyAttempts;
            public String ban;
            public String invalidNickPattern;
            public String ipLimitOnlineReached;
            public String ipLimitRegisteredReached;
            public String totpTimeout;
            public String totpTooManyAttempts;
            public String totpBan;
            public String authServerUnavailable;
            public String backendServerUnavailable;
        }

        public static class Reminder {
            public String login;
            public String register;
        }

        public static class Dialog {
            public Register register;
            public Login login;
            public Notifications notifications;

            public static class Register {
                public String title;
                public String passwordField;
                public String repeatPasswordField;
                public String confirmButton;
            }

            public static class Login {
                public String title;
                public String passwordField;
                public String confirmButton;
            }

            public static class Notifications {
                public String wrongPassword;
                public String invalidLength;
                public String invalidPattern;
                public String mismatch;
                public String passwordEmpty;
            }
        }

        public static class BossBar {
            public String message;
        }

        public static class Title {
            public Stage login;
            public Stage register;
            public String onAuthTitle;
            public String onAuthSubTitle;

            public static class Stage {
                public String title;
                public String subTitle;
            }
        }

        public static class ActionBar {
            public String message;
        }
    }

    public void load(MessagesConfig config) {
        String prefixRaw = config.prefix;

        onlyPlayer = COLORIZER.colorize(getPrefixed(config.onlyPlayer, prefixRaw));
        queryError = COLORIZER.colorize(getPrefixed(config.queryError, prefixRaw));
        processing = COLORIZER.colorize(getPrefixed(config.processing, prefixRaw));
        playerNotFound = COLORIZER.colorize(getPrefixed(config.playerNotFound, prefixRaw));
        noPermission = COLORIZER.colorize(getPrefixed(config.noPermission, prefixRaw));

        admin = new Admin();
        admin.usage = COLORIZER.colorize(getPrefixed(config.admin.usage, prefixRaw));

        admin.config = new Admin.Config();
        admin.config.reload = COLORIZER.colorize(getPrefixed(config.admin.config.reload, prefixRaw));

        admin.unregister = new Admin.Unregister();
        admin.unregister.usage = COLORIZER.colorize(getPrefixed(config.admin.unregister.usage, prefixRaw));
        admin.unregister.success = COLORIZER.colorize(getPrefixed(config.admin.unregister.success, prefixRaw));

        admin.changePassword = new Admin.ChangePassword();
        admin.changePassword.usage = COLORIZER.colorize(getPrefixed(config.admin.changePassword.usage, prefixRaw));
        admin.changePassword.success = COLORIZER.colorize(getPrefixed(config.admin.changePassword.success, prefixRaw));

        admin.forceLogin = new Admin.ForceLogin();
        admin.forceLogin.usage = COLORIZER.colorize(getPrefixed(config.admin.forceLogin.usage, prefixRaw));
        admin.forceLogin.isAuthenticated = COLORIZER.colorize(getPrefixed(config.admin.forceLogin.isAuthenticated, prefixRaw));
        admin.forceLogin.success = COLORIZER.colorize(getPrefixed(config.admin.forceLogin.success, prefixRaw));

        admin.forceRegister = new Admin.ForceRegister();
        admin.forceRegister.usage = COLORIZER.colorize(getPrefixed(config.admin.forceRegister.usage, prefixRaw));
        admin.forceRegister.alreadyRegistered = COLORIZER.colorize(getPrefixed(config.admin.forceRegister.alreadyRegistered, prefixRaw));
        admin.forceRegister.success = COLORIZER.colorize(getPrefixed(config.admin.forceRegister.success, prefixRaw));

        admin.forcePremium = new Admin.ForcePremium();
        admin.forcePremium.usage = COLORIZER.colorize(getPrefixed(config.admin.forcePremium.usage, prefixRaw));
        admin.forcePremium.enabled = COLORIZER.colorize(getPrefixed(config.admin.forcePremium.enabled, prefixRaw));
        admin.forcePremium.disabled = COLORIZER.colorize(getPrefixed(config.admin.forcePremium.disabled, prefixRaw));

        admin.migrate = new Admin.Migrate();
        admin.migrate.usage = COLORIZER.colorize(getPrefixed(config.admin.migrate.usage, prefixRaw));
        admin.migrate.error = COLORIZER.colorize(getPrefixed(config.admin.migrate.error, prefixRaw));
        admin.migrate.invalidFileName = COLORIZER.colorize(getPrefixed(config.admin.migrate.invalidFileName, prefixRaw));
        admin.migrate.success = COLORIZER.colorize(getPrefixed(config.admin.migrate.success, prefixRaw));

        admin.backup = new Admin.Backup();
        admin.backup.usage = COLORIZER.colorize(getPrefixed(config.admin.backup.usage, prefixRaw));
        admin.backup.invalidFileName = COLORIZER.colorize(getPrefixed(config.admin.backup.invalidFileName, prefixRaw));
        admin.backup.invalidCompression = COLORIZER.colorize(getPrefixed(config.admin.backup.invalidCompression, prefixRaw));
        admin.backup.alreadyExists = COLORIZER.colorize(getPrefixed(config.admin.backup.alreadyExists, prefixRaw));
        admin.backup.notFound = COLORIZER.colorize(getPrefixed(config.admin.backup.notFound, prefixRaw));
        admin.backup.creating = COLORIZER.colorize(getPrefixed(config.admin.backup.creating, prefixRaw));
        admin.backup.createSuccess = COLORIZER.colorize(getPrefixed(config.admin.backup.createSuccess, prefixRaw));
        admin.backup.createError = COLORIZER.colorize(getPrefixed(config.admin.backup.createError, prefixRaw));
        admin.backup.restoring = COLORIZER.colorize(getPrefixed(config.admin.backup.restoring, prefixRaw));
        admin.backup.restoreSuccess = COLORIZER.colorize(getPrefixed(config.admin.backup.restoreSuccess, prefixRaw));
        admin.backup.restoreError = COLORIZER.colorize(getPrefixed(config.admin.backup.restoreError, prefixRaw));

        player = new Player();

        player.checkPassword = new Player.CheckPassword();
        player.checkPassword.wrongPassword = COLORIZER.colorize(getPrefixed(config.player.checkPassword.wrongPassword, prefixRaw));
        player.checkPassword.invalidLength = COLORIZER.colorize(getPrefixed(config.player.checkPassword.invalidLength, prefixRaw));
        player.checkPassword.invalidPattern = COLORIZER.colorize(getPrefixed(config.player.checkPassword.invalidPattern, prefixRaw));
        player.checkPassword.passwordEmpty = COLORIZER.colorize(getPrefixed(config.player.checkPassword.passwordEmpty, prefixRaw));

        player.register = new Player.Register();
        player.register.usage = COLORIZER.colorize(getPrefixed(config.player.register.usage, prefixRaw));
        player.register.mismatch = COLORIZER.colorize(getPrefixed(config.player.register.mismatch, prefixRaw));
        player.register.alreadyRegistered = COLORIZER.colorize(getPrefixed(config.player.register.alreadyRegistered, prefixRaw));
        player.register.success = COLORIZER.colorize(getPrefixed(config.player.register.success, prefixRaw));

        player.unregister = new Player.Unregister();
        player.unregister.usage = COLORIZER.colorize(getPrefixed(config.player.unregister.usage, prefixRaw));
        player.unregister.success = COLORIZER.colorize(getPrefixed(config.player.unregister.success, prefixRaw));

        player.login = new Player.Login();
        player.login.usage = COLORIZER.colorize(getPrefixed(config.player.login.usage, prefixRaw));
        player.login.notRegistered = COLORIZER.colorize(getPrefixed(config.player.login.notRegistered, prefixRaw));
        player.login.alreadyLogged = COLORIZER.colorize(getPrefixed(config.player.login.alreadyLogged, prefixRaw));
        player.login.wrongPassword = COLORIZER.colorize(getPrefixed(config.player.login.wrongPassword, prefixRaw));
        player.login.success = COLORIZER.colorize(getPrefixed(config.player.login.success, prefixRaw));

        player.changePassword = new Player.ChangePassword();
        player.changePassword.usage = COLORIZER.colorize(getPrefixed(config.player.changePassword.usage, prefixRaw));
        player.changePassword.success = COLORIZER.colorize(getPrefixed(config.player.changePassword.success, prefixRaw));

        player.logout = new Player.Logout();
        player.logout.logoutByPremium = COLORIZER.colorize(getPrefixed(config.player.logout.logoutByPremium, prefixRaw));
        player.logout.success = COLORIZER.colorize(getPrefixed(config.player.logout.success, prefixRaw));

        player.premium = new Player.Premium();
        player.premium.enabled = COLORIZER.colorize(getPrefixed(config.player.premium.enabled, prefixRaw));
        player.premium.disabled = COLORIZER.colorize(getPrefixed(config.player.premium.disabled, prefixRaw));

        player.totp = new Player.Totp();
        player.totp.usage = COLORIZER.colorize(getPrefixed(config.player.totp.usage, prefixRaw));
        player.totp.enableUsage = COLORIZER.colorize(getPrefixed(config.player.totp.enableUsage, prefixRaw));
        player.totp.verifyUsage = COLORIZER.colorize(getPrefixed(config.player.totp.verifyUsage, prefixRaw));
        player.totp.disableUsage = COLORIZER.colorize(getPrefixed(config.player.totp.disableUsage, prefixRaw));
        player.totp.successful = COLORIZER.colorize(getPrefixed(config.player.totp.successful, prefixRaw));
        player.totp.verified = COLORIZER.colorize(getPrefixed(config.player.totp.verified, prefixRaw));
        player.totp.disabled = COLORIZER.colorize(getPrefixed(config.player.totp.disabled, prefixRaw));
        player.totp.wrong = COLORIZER.colorize(getPrefixed(config.player.totp.wrong, prefixRaw));
        player.totp.alreadyEnabled = COLORIZER.colorize(getPrefixed(config.player.totp.alreadyEnabled, prefixRaw));
        player.totp.alreadyDisabled = COLORIZER.colorize(getPrefixed(config.player.totp.alreadyDisabled, prefixRaw));
        player.totp.qr = COLORIZER.colorize(getPrefixed(config.player.totp.qr, prefixRaw));
        player.totp.token = COLORIZER.colorize(getPrefixed(config.player.totp.token, prefixRaw));
        player.totp.recovery = COLORIZER.colorize(getPrefixed(config.player.totp.recovery, prefixRaw));
        player.totp.needPassword = COLORIZER.colorize(getPrefixed(config.player.totp.needPassword, prefixRaw));
        player.totp.prompt = COLORIZER.colorize(getPrefixed(config.player.totp.prompt, prefixRaw));

        player.kick = new Player.Kick();
        player.kick.timeout = COLORIZER.colorize(getPrefixed(config.player.kick.timeout, prefixRaw));
        player.kick.realname = COLORIZER.colorize(getPrefixed(config.player.kick.realname, prefixRaw));
        player.kick.tooManyAttempts = COLORIZER.colorize(getPrefixed(config.player.kick.tooManyAttempts, prefixRaw));
        player.kick.ban = COLORIZER.colorize(getPrefixed(config.player.kick.ban, prefixRaw));
        player.kick.invalidNickPattern = COLORIZER.colorize(getPrefixed(config.player.kick.invalidNickPattern, prefixRaw));
        player.kick.ipLimitOnlineReached = COLORIZER.colorize(getPrefixed(config.player.kick.ipLimitOnlineReached, prefixRaw));
        player.kick.ipLimitRegisteredReached = COLORIZER.colorize(getPrefixed(config.player.kick.ipLimitRegisteredReached, prefixRaw));
        player.kick.totpTimeout = COLORIZER.colorize(getPrefixed(config.player.kick.totpTimeout, prefixRaw));
        player.kick.totpTooManyAttempts = COLORIZER.colorize(getPrefixed(config.player.kick.totpTooManyAttempts, prefixRaw));
        player.kick.totpBan = COLORIZER.colorize(getPrefixed(config.player.kick.totpBan, prefixRaw));
        player.kick.authServerUnavailable = COLORIZER.colorize(getPrefixed(config.player.kick.authServerUnavailable, prefixRaw));
        player.kick.backendServerUnavailable = COLORIZER.colorize(getPrefixed(config.player.kick.backendServerUnavailable, prefixRaw));

        player.reminder = new Player.Reminder();
        player.reminder.login = COLORIZER.colorize(getPrefixed(config.player.reminder.login, prefixRaw));
        player.reminder.register = COLORIZER.colorize(getPrefixed(config.player.reminder.register, prefixRaw));

        player.dialog = new Player.Dialog();

        player.dialog.register = new Player.Dialog.Register();
        player.dialog.register.title = COLORIZER.colorize(getPrefixed(config.player.dialog.register.title, prefixRaw));
        player.dialog.register.passwordField = COLORIZER.colorize(getPrefixed(config.player.dialog.register.passwordField, prefixRaw));
        player.dialog.register.repeatPasswordField = COLORIZER.colorize(getPrefixed(config.player.dialog.register.repeatPasswordField, prefixRaw));
        player.dialog.register.confirmButton = COLORIZER.colorize(getPrefixed(config.player.dialog.register.confirmButton, prefixRaw));

        player.dialog.login = new Player.Dialog.Login();
        player.dialog.login.title = COLORIZER.colorize(getPrefixed(config.player.dialog.login.title, prefixRaw));
        player.dialog.login.passwordField = COLORIZER.colorize(getPrefixed(config.player.dialog.login.passwordField, prefixRaw));
        player.dialog.login.confirmButton = COLORIZER.colorize(getPrefixed(config.player.dialog.login.confirmButton, prefixRaw));

        player.dialog.notifications = new Player.Dialog.Notifications();
        player.dialog.notifications.wrongPassword = COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.wrongPassword, prefixRaw));
        player.dialog.notifications.invalidLength = COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.invalidLength, prefixRaw));
        player.dialog.notifications.invalidPattern = COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.invalidPattern, prefixRaw));
        player.dialog.notifications.mismatch = COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.mismatch, prefixRaw));
        player.dialog.notifications.passwordEmpty = COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.passwordEmpty, prefixRaw));

        player.bossBar = new Player.BossBar();
        player.bossBar.message = COLORIZER.colorize(getPrefixed(config.player.bossBar.message, prefixRaw));

        player.title = new Player.Title();
        player.title.login = new Player.Title.Stage();
        player.title.login.title = COLORIZER.colorize(getPrefixed(config.player.title.login.title, prefixRaw));
        player.title.login.subTitle = COLORIZER.colorize(getPrefixed(config.player.title.login.subTitle, prefixRaw));
        player.title.register = new Player.Title.Stage();
        player.title.register.title = COLORIZER.colorize(getPrefixed(config.player.title.register.title, prefixRaw));
        player.title.register.subTitle = COLORIZER.colorize(getPrefixed(config.player.title.register.subTitle, prefixRaw));
        player.title.onAuthTitle = COLORIZER.colorize(getPrefixed(config.player.title.onAuthTitle, prefixRaw));
        player.title.onAuthSubTitle = COLORIZER.colorize(getPrefixed(config.player.title.onAuthSubTitle, prefixRaw));

        player.actionBar = new Player.ActionBar();
        player.actionBar.message = COLORIZER.colorize(getPrefixed(config.player.actionBar.message, prefixRaw));
    }

    private String getPrefixed(String rawMessage, String prefix) {
        return rawMessage.replace("{prefix}", prefix);
    }
}
