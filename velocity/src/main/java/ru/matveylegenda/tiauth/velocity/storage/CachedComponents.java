package ru.matveylegenda.tiauth.velocity.storage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.matveylegenda.tiauth.config.MessagesConfig;

import static ru.matveylegenda.tiauth.util.Utils.COLORIZER;

public class CachedComponents {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public static CachedComponents IMP = new CachedComponents(MessagesConfig.IMP);

    public CachedComponents(MessagesConfig messagesConfig) {
        load(messagesConfig);
    }

    public Component prefix;
    public Component onlyPlayer;
    public Component queryError;
    public Component processing;
    public Component playerNotFound;
    public Component noPermission;
    public Admin admin;
    public Player player;

    public static class Admin {
        public Component usage;
        public Config config;
        public Unregister unregister;
        public ChangePassword changePassword;
        public ForceLogin forceLogin;
        public ForceRegister forceRegister;
        public Migrate migrate;

        public static class Config {
            public Component reload;
        }

        public static class Unregister {
            public Component usage;
            public Component success;
        }

        public static class ChangePassword {
            public Component usage;
            public Component success;
        }

        public static class ForceLogin {
            public Component usage;
            public Component isAuthenticated;
            public Component success;
        }

        public static class ForceRegister {
            public Component usage;
            public Component alreadyRegistered;
            public Component success;
        }

        public static class Migrate {
            public Component usage;
            public Component error;
            public Component success;
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
            public Component wrongPassword;
            public Component invalidLength;
            public Component invalidPattern;
            public Component passwordEmpty;
        }

        public static class Register {
            public Component usage;
            public Component mismatch;
            public Component alreadyRegistered;
            public Component success;
        }

        public static class Unregister {
            public Component usage;
            public Component success;
        }

        public static class Login {
            public Component usage;
            public Component notRegistered;
            public Component alreadyLogged;
            public Component wrongPassword;
            public Component success;
        }

        public static class ChangePassword {
            public Component usage;
            public Component success;
        }

        public static class Logout {
            public Component logoutByPremium;
        }

        public static class Premium {
            public Component enabled;
            public Component disabled;
        }

        public static class Kick {
            public Component notAuth;
            public Component timeout;
            public Component realname;
            public Component tooManyAttempts;
            public Component ban;
            public Component invalidNickPattern;
            public Component ipLimitOnlineReached;
            public Component ipLimitRegisteredReached;
        }

        public static class Reminder {
            public Component login;
            public Component register;
        }

        public static class Dialog {
            public Register register;
            public Login login;
            public Notifications notifications;

            public static class Register {
                public Component title;
                public Component passwordField;
                public Component repeatPasswordField;
                public Component confirmButton;
            }

            public static class Login {
                public Component title;
                public Component passwordField;
                public Component confirmButton;
            }

            public static class Notifications {
                public Component wrongPassword;
                public Component invalidLength;
                public Component invalidPattern;
                public Component mismatch;
                public Component passwordEmpty;
            }
        }

        public static class BossBar {
            public Component message;
        }

        public static class Title {
            public Component title;
            public Component subTitle;
        }

        public static class ActionBar {
            public Component message;
        }
    }

    public void load(MessagesConfig config) {
        String prefixRaw = COLORIZER.colorize(config.prefix);
        prefix = LEGACY.deserialize(prefixRaw);

        onlyPlayer = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.onlyPlayer, prefixRaw)));
        queryError = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.queryError, prefixRaw)));
        processing = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.processing, prefixRaw)));
        playerNotFound = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.playerNotFound, prefixRaw)));
        noPermission = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.noPermission, prefixRaw)));

        admin = new Admin();
        admin.usage = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.usage, prefixRaw)));

        admin.config = new Admin.Config();
        admin.config.reload = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.config.reload, prefixRaw)));

        admin.unregister = new Admin.Unregister();
        admin.unregister.usage = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.unregister.usage, prefixRaw)));
        admin.unregister.success = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.unregister.success, prefixRaw)));

        admin.changePassword = new Admin.ChangePassword();
        admin.changePassword.usage = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.changePassword.usage, prefixRaw)));
        admin.changePassword.success = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.changePassword.success, prefixRaw)));

        admin.forceLogin = new Admin.ForceLogin();
        admin.forceLogin.usage = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.forceLogin.usage, prefixRaw)));
        admin.forceLogin.isAuthenticated = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.forceLogin.isAuthenticated, prefixRaw)));
        admin.forceLogin.success = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.forceLogin.success, prefixRaw)));

        admin.forceRegister = new Admin.ForceRegister();
        admin.forceRegister.usage = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.forceRegister.usage, prefixRaw)));
        admin.forceRegister.alreadyRegistered = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.forceRegister.alreadyRegistered, prefixRaw)));
        admin.forceRegister.success = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.forceRegister.success, prefixRaw)));

        admin.migrate = new Admin.Migrate();
        admin.migrate.usage = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.migrate.usage, prefixRaw)));
        admin.migrate.error = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.migrate.error, prefixRaw)));
        admin.migrate.success = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.admin.migrate.success, prefixRaw)));

        player = new Player();

        player.checkPassword = new Player.CheckPassword();
        player.checkPassword.wrongPassword = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.checkPassword.wrongPassword, prefixRaw)));
        player.checkPassword.invalidLength = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.checkPassword.invalidLength, prefixRaw)));
        player.checkPassword.invalidPattern = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.checkPassword.invalidPattern, prefixRaw)));
        player.checkPassword.passwordEmpty = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.checkPassword.passwordEmpty, prefixRaw)));

        player.register = new Player.Register();
        player.register.usage = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.register.usage, prefixRaw)));
        player.register.mismatch = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.register.mismatch, prefixRaw)));
        player.register.alreadyRegistered = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.register.alreadyRegistered, prefixRaw)));
        player.register.success = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.register.success, prefixRaw)));

        player.unregister = new Player.Unregister();
        player.unregister.usage = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.unregister.usage, prefixRaw)));
        player.unregister.success = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.unregister.success, prefixRaw)));

        player.login = new Player.Login();
        player.login.usage = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.login.usage, prefixRaw)));
        player.login.notRegistered = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.login.notRegistered, prefixRaw)));
        player.login.alreadyLogged = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.login.alreadyLogged, prefixRaw)));
        player.login.wrongPassword = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.login.wrongPassword, prefixRaw)));
        player.login.success = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.login.success, prefixRaw)));

        player.changePassword = new Player.ChangePassword();
        player.changePassword.usage = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.changePassword.usage, prefixRaw)));
        player.changePassword.success = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.changePassword.success, prefixRaw)));

        player.logout = new Player.Logout();
        player.logout.logoutByPremium = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.logout.logoutByPremium, prefixRaw)));

        player.premium = new Player.Premium();
        player.premium.enabled = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.premium.enabled, prefixRaw)));
        player.premium.disabled = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.premium.disabled, prefixRaw)));

        player.kick = new Player.Kick();
        player.kick.timeout = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.kick.timeout, prefixRaw)));
        player.kick.realname = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.kick.realname, prefixRaw)));
        player.kick.tooManyAttempts = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.kick.tooManyAttempts, prefixRaw)));
        player.kick.ban = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.kick.ban, prefixRaw)));
        player.kick.invalidNickPattern = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.kick.invalidNickPattern, prefixRaw)));
        player.kick.ipLimitOnlineReached = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.kick.ipLimitOnlineReached, prefixRaw)));
        player.kick.ipLimitRegisteredReached = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.kick.ipLimitRegisteredReached, prefixRaw)));

        player.reminder = new Player.Reminder();
        player.reminder.login = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.reminder.login, prefixRaw)));
        player.reminder.register = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.reminder.register, prefixRaw)));

        player.dialog = new Player.Dialog();

        player.dialog.register = new Player.Dialog.Register();
        player.dialog.register.title = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.register.title, prefixRaw)));
        player.dialog.register.passwordField = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.register.passwordField, prefixRaw)));
        player.dialog.register.repeatPasswordField = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.register.repeatPasswordField, prefixRaw)));
        player.dialog.register.confirmButton = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.register.confirmButton, prefixRaw)));

        player.dialog.login = new Player.Dialog.Login();
        player.dialog.login.title = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.login.title, prefixRaw)));
        player.dialog.login.passwordField = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.login.passwordField, prefixRaw)));
        player.dialog.login.confirmButton = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.login.confirmButton, prefixRaw)));

        player.dialog.notifications = new Player.Dialog.Notifications();
        player.dialog.notifications.wrongPassword = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.wrongPassword, prefixRaw)));
        player.dialog.notifications.invalidLength = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.invalidLength, prefixRaw)));
        player.dialog.notifications.invalidPattern = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.invalidPattern, prefixRaw)));
        player.dialog.notifications.mismatch = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.mismatch, prefixRaw)));
        player.dialog.notifications.passwordEmpty = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.dialog.notifications.passwordEmpty, prefixRaw)));

        player.bossBar = new Player.BossBar();
        player.bossBar.message = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.bossBar.message, prefixRaw)));

        player.title = new Player.Title();
        player.title.title = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.title.title, prefixRaw)));
        player.title.subTitle = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.title.subTitle, prefixRaw)));

        player.actionBar = new Player.ActionBar();
        player.actionBar.message = LEGACY.deserialize(COLORIZER.colorize(getPrefixed(config.player.actionBar.message, prefixRaw)));
    }

    private String getPrefixed(String rawMessage, String prefix) {
        return rawMessage.replace("{prefix}", prefix);
    }
}
