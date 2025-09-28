package ru.matveylegenda.tiauth.util.colorizer;

import ru.matveylegenda.tiauth.config.MessagesConfig;

import static ru.matveylegenda.tiauth.util.Utils.COLORIZER;

public class ColorizedMessages {
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
            public String notAuth;
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
        onlyPlayer = COLORIZER.colorize(config.onlyPlayer);
        queryError = COLORIZER.colorize(config.queryError);
        processing = COLORIZER.colorize(config.processing);
        playerNotFound = COLORIZER.colorize(config.playerNotFound);
        noPermission = COLORIZER.colorize(config.noPermission);

        admin = new Admin();
        admin.usage = COLORIZER.colorize(config.admin.usage);

        admin.config = new Admin.Config();
        admin.config.reload = COLORIZER.colorize(config.admin.config.reload);

        admin.unregister = new Admin.Unregister();
        admin.unregister.usage = COLORIZER.colorize(config.admin.unregister.usage);
        admin.unregister.success = COLORIZER.colorize(config.admin.unregister.success);

        admin.changePassword = new Admin.ChangePassword();
        admin.changePassword.usage = COLORIZER.colorize(config.admin.changePassword.usage);
        admin.changePassword.success = COLORIZER.colorize(config.admin.changePassword.success);

        admin.forceLogin = new Admin.ForceLogin();
        admin.forceLogin.usage = COLORIZER.colorize(config.admin.forceLogin.usage);
        admin.forceLogin.isAuthenticated = COLORIZER.colorize(config.admin.forceLogin.isAuthenticated);
        admin.forceLogin.success = COLORIZER.colorize(config.admin.forceLogin.success);

        admin.forceRegister = new Admin.ForceRegister();
        admin.forceRegister.usage = COLORIZER.colorize(config.admin.forceRegister.usage);
        admin.forceRegister.alreadyRegistered = COLORIZER.colorize(config.admin.forceRegister.alreadyRegistered);
        admin.forceRegister.success = COLORIZER.colorize(config.admin.forceRegister.success);

        admin.migrate = new Admin.Migrate();
        admin.migrate.usage = COLORIZER.colorize(config.admin.migrate.usage);
        admin.migrate.error = COLORIZER.colorize(config.admin.migrate.error);
        admin.migrate.success = COLORIZER.colorize(config.admin.migrate.success);

        player = new Player();

        player.checkPassword = new Player.CheckPassword();
        player.checkPassword.wrongPassword = COLORIZER.colorize(config.player.checkPassword.wrongPassword);
        player.checkPassword.invalidLength = COLORIZER.colorize(config.player.checkPassword.invalidLength);
        player.checkPassword.invalidPattern = COLORIZER.colorize(config.player.checkPassword.invalidPattern);
        player.checkPassword.passwordEmpty = COLORIZER.colorize(config.player.checkPassword.passwordEmpty);

        player.register = new Player.Register();
        player.register.usage = COLORIZER.colorize(config.player.register.usage);
        player.register.mismatch = COLORIZER.colorize(config.player.register.mismatch);
        player.register.alreadyRegistered = COLORIZER.colorize(config.player.register.alreadyRegistered);
        player.register.success = COLORIZER.colorize(config.player.register.success);

        player.unregister = new Player.Unregister();
        player.unregister.usage = COLORIZER.colorize(config.player.unregister.usage);
        player.unregister.success = COLORIZER.colorize(config.player.unregister.success);

        player.login = new Player.Login();
        player.login.usage = COLORIZER.colorize(config.player.login.usage);
        player.login.notRegistered = COLORIZER.colorize(config.player.login.notRegistered);
        player.login.alreadyLogged = COLORIZER.colorize(config.player.login.alreadyLogged);
        player.login.wrongPassword = COLORIZER.colorize(config.player.login.wrongPassword);
        player.login.success = COLORIZER.colorize(config.player.login.success);

        player.changePassword = new Player.ChangePassword();
        player.changePassword.usage = COLORIZER.colorize(config.player.changePassword.usage);
        player.changePassword.success = COLORIZER.colorize(config.player.changePassword.success);

        player.logout = new Player.Logout();
        player.logout.logoutByPremium = COLORIZER.colorize(config.player.logout.logoutByPremium);

        player.premium = new Player.Premium();
        player.premium.enabled = COLORIZER.colorize(config.player.premium.enabled);
        player.premium.disabled = COLORIZER.colorize(config.player.premium.disabled);

        player.kick = new Player.Kick();
        player.kick.notAuth = COLORIZER.colorize(config.player.kick.notAuth);
        player.kick.timeout = COLORIZER.colorize(config.player.kick.timeout);
        player.kick.realname = COLORIZER.colorize(config.player.kick.realname);
        player.kick.tooManyAttempts = COLORIZER.colorize(config.player.kick.tooManyAttempts);
        player.kick.ban = COLORIZER.colorize(config.player.kick.ban);
        player.kick.invalidNickPattern = COLORIZER.colorize(config.player.kick.invalidNickPattern);
        player.kick.ipLimitOnlineReached = COLORIZER.colorize(config.player.kick.ipLimitOnlineReached);
        player.kick.ipLimitRegisteredReached = COLORIZER.colorize(config.player.kick.ipLimitRegisteredReached);

        player.reminder = new Player.Reminder();
        player.reminder.login = COLORIZER.colorize(config.player.reminder.login);
        player.reminder.register = COLORIZER.colorize(config.player.reminder.register);

        player.dialog = new Player.Dialog();

        player.dialog.register = new Player.Dialog.Register();
        player.dialog.register.title = COLORIZER.colorize(config.player.dialog.register.title);
        player.dialog.register.passwordField = COLORIZER.colorize(config.player.dialog.register.passwordField);
        player.dialog.register.repeatPasswordField = COLORIZER.colorize(config.player.dialog.register.repeatPasswordField);
        player.dialog.register.confirmButton = COLORIZER.colorize(config.player.dialog.register.confirmButton);

        player.dialog.login = new Player.Dialog.Login();
        player.dialog.login.title = COLORIZER.colorize(config.player.dialog.login.title);
        player.dialog.login.passwordField = COLORIZER.colorize(config.player.dialog.login.passwordField);
        player.dialog.login.confirmButton = COLORIZER.colorize(config.player.dialog.login.confirmButton);

        player.dialog.notifications = new Player.Dialog.Notifications();
        player.dialog.notifications.wrongPassword = COLORIZER.colorize(config.player.dialog.notifications.wrongPassword);
        player.dialog.notifications.invalidLength = COLORIZER.colorize(config.player.dialog.notifications.invalidLength);
        player.dialog.notifications.invalidPattern = COLORIZER.colorize(config.player.dialog.notifications.invalidPattern);
        player.dialog.notifications.mismatch = COLORIZER.colorize(config.player.dialog.notifications.mismatch);
        player.dialog.notifications.passwordEmpty = COLORIZER.colorize(config.player.dialog.notifications.passwordEmpty);

        player.bossBar = new Player.BossBar();
        player.bossBar.message = COLORIZER.colorize(config.player.bossBar.message);

        player.title = new Player.Title();
        player.title.title = COLORIZER.colorize(config.player.title.title);
        player.title.subTitle = COLORIZER.colorize(config.player.title.subTitle);

        player.actionBar = new Player.ActionBar();
        player.actionBar.message = COLORIZER.colorize(config.player.actionBar.message);
    }
}
