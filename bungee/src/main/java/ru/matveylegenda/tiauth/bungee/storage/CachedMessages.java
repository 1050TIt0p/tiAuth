package ru.matveylegenda.tiauth.bungee.storage;

import ru.matveylegenda.tiauth.config.MessagesConfig;

import static ru.matveylegenda.tiauth.util.Utils.COLORIZER;

public class CachedMessages {

    public static CachedMessages IMP = new CachedMessages(MessagesConfig.IMP);

    public CachedMessages(MessagesConfig messagesConfig) {
        load(messagesConfig);
    }

    public String prefix;
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
            public String success;
        }
    }

    public static class Player {
        public CheckPassword checkPassword;
        public Register register;
        public Unregister unregister;
        public Login login;
        public ChangePassword changePassword;
        public Logout logout;
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
            public String title;
            public String subTitle;
        }

        public static class ActionBar {
            public String message;
        }
    }

    public void load(MessagesConfig config) {
        prefix = COLORIZER.colorize(config.prefix);
        onlyPlayer = COLORIZER.colorize(getPrefixed(config.onlyPlayer, prefix));
        queryError = COLORIZER.colorize(getPrefixed(config.queryError, prefix));
        processing = COLORIZER.colorize(getPrefixed(config.processing, prefix));
        playerNotFound = COLORIZER.colorize(getPrefixed(config.playerNotFound, prefix));
        noPermission = COLORIZER.colorize(getPrefixed(config.noPermission, prefix));

        admin = new Admin();
        admin.usage = COLORIZER.colorize(getPrefixed(config.admin.usage, prefix));

        admin.config = new Admin.Config();
        admin.config.reload = COLORIZER.colorize(getPrefixed(config.admin.config.reload, prefix));

        admin.unregister = new Admin.Unregister();
        admin.unregister.usage = COLORIZER.colorize(getPrefixed(config.admin.unregister.usage, prefix));
        admin.unregister.success = COLORIZER.colorize(getPrefixed(config.admin.unregister.success, prefix));

        admin.changePassword = new Admin.ChangePassword();
        admin.changePassword.usage = COLORIZER.colorize(getPrefixed(config.admin.changePassword.usage, prefix));
        admin.changePassword.success = COLORIZER.colorize(getPrefixed(config.admin.changePassword.success, prefix));

        admin.forceLogin = new Admin.ForceLogin();
        admin.forceLogin.usage = COLORIZER.colorize(getPrefixed(config.admin.forceLogin.usage, prefix));
        admin.forceLogin.isAuthenticated = COLORIZER.colorize(getPrefixed(config.admin.forceLogin.isAuthenticated, prefix));
        admin.forceLogin.success = COLORIZER.colorize(getPrefixed(config.admin.forceLogin.success, prefix));

        admin.forceRegister = new Admin.ForceRegister();
        admin.forceRegister.usage = COLORIZER.colorize(getPrefixed(config.admin.forceRegister.usage, prefix));
        admin.forceRegister.alreadyRegistered = COLORIZER.colorize(getPrefixed(config.admin.forceRegister.alreadyRegistered, prefix));
        admin.forceRegister.success = COLORIZER.colorize(getPrefixed(config.admin.forceRegister.success, prefix));

        admin.forcePremium = new Admin.ForcePremium();
        admin.forcePremium.usage = COLORIZER.colorize(getPrefixed(config.admin.forcePremium.usage, prefix));
        admin.forcePremium.enabled = COLORIZER.colorize(getPrefixed(config.admin.forcePremium.enabled, prefix));
        admin.forcePremium.disabled = COLORIZER.colorize(getPrefixed(config.admin.forcePremium.disabled, prefix));

        admin.migrate = new Admin.Migrate();
        admin.migrate.usage = COLORIZER.colorize(getPrefixed(config.admin.migrate.usage, prefix));
        admin.migrate.error = COLORIZER.colorize(getPrefixed(config.admin.migrate.error, prefix));
        admin.migrate.success = COLORIZER.colorize(getPrefixed(config.admin.migrate.success, prefix));

        player = new Player();

        player.checkPassword = new Player.CheckPassword();
        player.checkPassword.wrongPassword = COLORIZER.colorize(getPrefixed(config.player.checkPassword.wrongPassword, prefix));
        player.checkPassword.invalidLength = COLORIZER.colorize(getPrefixed(config.player.checkPassword.invalidLength, prefix));
        player.checkPassword.invalidPattern = COLORIZER.colorize(getPrefixed(config.player.checkPassword.invalidPattern, prefix));
        player.checkPassword.passwordEmpty = COLORIZER.colorize(getPrefixed(config.player.checkPassword.passwordEmpty, prefix));

        player.register = new Player.Register();
        player.register.usage = COLORIZER.colorize(getPrefixed(config.player.register.usage, prefix));
        player.register.mismatch = COLORIZER.colorize(getPrefixed(config.player.register.mismatch, prefix));
        player.register.alreadyRegistered = COLORIZER.colorize(getPrefixed(config.player.register.alreadyRegistered, prefix));
        player.register.success = COLORIZER.colorize(getPrefixed(config.player.register.success, prefix));

        player.unregister = new Player.Unregister();
        player.unregister.usage = COLORIZER.colorize(getPrefixed(config.player.unregister.usage, prefix));
        player.unregister.success = COLORIZER.colorize(getPrefixed(config.player.unregister.success, prefix));

        player.login = new Player.Login();
        player.login.usage = COLORIZER.colorize(getPrefixed(config.player.login.usage, prefix));
        player.login.notRegistered = COLORIZER.colorize(getPrefixed(config.player.login.notRegistered, prefix));
        player.login.alreadyLogged = COLORIZER.colorize(getPrefixed(config.player.login.alreadyLogged, prefix));
        player.login.wrongPassword = COLORIZER.colorize(getPrefixed(config.player.login.wrongPassword, prefix));
        player.login.success = COLORIZER.colorize(getPrefixed(config.player.login.success, prefix));

        player.changePassword = new Player.ChangePassword();
        player.changePassword.usage = COLORIZER.colorize(getPrefixed(config.player.changePassword.usage, prefix));
        player.changePassword.success = COLORIZER.colorize(getPrefixed(config.player.changePassword.success, prefix));

        player.logout = new Player.Logout();
        player.logout.logoutByPremium = COLORIZER.colorize(getPrefixed(config.player.logout.logoutByPremium, prefix));

        player.premium = new Player.Premium();
        player.premium.enabled = COLORIZER.colorize(getPrefixed(config.player.premium.enabled, prefix));
        player.premium.disabled = COLORIZER.colorize(getPrefixed(config.player.premium.disabled, prefix));

        player.kick = new Player.Kick();
        player.kick.timeout = COLORIZER.colorize(getPrefixed(config.player.kick.timeout, prefix));
        player.kick.realname = COLORIZER.colorize(getPrefixed(config.player.kick.realname, prefix));
        player.kick.tooManyAttempts = COLORIZER.colorize(getPrefixed(config.player.kick.tooManyAttempts, prefix));
        player.kick.ban = COLORIZER.colorize(getPrefixed(config.player.kick.ban, prefix));
        player.kick.invalidNickPattern = COLORIZER.colorize(getPrefixed(config.player.kick.invalidNickPattern, prefix));
        player.kick.ipLimitOnlineReached = COLORIZER.colorize(getPrefixed(config.player.kick.ipLimitOnlineReached, prefix));
        player.kick.ipLimitRegisteredReached = COLORIZER.colorize(getPrefixed(config.player.kick.ipLimitRegisteredReached, prefix));

        player.reminder = new Player.Reminder();
        player.reminder.login = COLORIZER.colorize(getPrefixed(config.player.reminder.login, prefix));
        player.reminder.register = COLORIZER.colorize(getPrefixed(config.player.reminder.register, prefix));

        player.dialog = new Player.Dialog();

        player.dialog.register = new Player.Dialog.Register();
        player.dialog.register.title = COLORIZER.colorize(getPrefixed(config.player.dialog.register.title, prefix));
        player.dialog.register.passwordField = COLORIZER.colorize(getPrefixed(config.player.dialog.register.passwordField, prefix));
        player.dialog.register.repeatPasswordField = COLORIZER.colorize(getPrefixed(config.player.dialog.register.repeatPasswordField, prefix));
        player.dialog.register.confirmButton = COLORIZER.colorize(getPrefixed(config.player.dialog.register.confirmButton, prefix));

        player.dialog.login = new Player.Dialog.Login();
        player.dialog.login.title = COLORIZER.colorize(getPrefixed(config.player.dialog.login.title, prefix));
        player.dialog.login.passwordField = COLORIZER.colorize(getPrefixed(config.player.dialog.login.passwordField, prefix));
        player.dialog.login.confirmButton = COLORIZER.colorize(getPrefixed(config.player.dialog.login.confirmButton, prefix));

        player.dialog.notifications = new Player.Dialog.Notifications();
        player.dialog.notifications.wrongPassword = COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.wrongPassword, prefix));
        player.dialog.notifications.invalidLength = COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.invalidLength, prefix));
        player.dialog.notifications.invalidPattern = COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.invalidPattern, prefix));
        player.dialog.notifications.mismatch = COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.mismatch, prefix));
        player.dialog.notifications.passwordEmpty = COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.passwordEmpty, prefix));

        player.bossBar = new Player.BossBar();
        player.bossBar.message = COLORIZER.colorize(getPrefixed(config.player.bossBar.message, prefix));

        player.title = new Player.Title();
        player.title.title = COLORIZER.colorize(getPrefixed(config.player.title.title, prefix));
        player.title.subTitle = COLORIZER.colorize(getPrefixed(config.player.title.subTitle, prefix));

        player.actionBar = new Player.ActionBar();
        player.actionBar.message = COLORIZER.colorize(getPrefixed(config.player.actionBar.message, prefix));
    }

    private String getPrefixed(String rawMessage, String prefix) {
        return rawMessage.replace("{prefix}", prefix);
    }
}
