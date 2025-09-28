package ru.matveylegenda.tiauth.util.colorizer;

import ru.matveylegenda.tiauth.config.MessagesConfig;

import static ru.matveylegenda.tiauth.util.Utils.COLORIZER;

public record ColorizedMessages(
        String prefix,
        String onlyPlayer,
        String queryError,
        String processing,
        String playerNotFound,
        String noPermission,
        Admin admin,
        Player player
) {
    public record Admin(
            String usage,
            Config config,
            Unregister unregister,
            ChangePassword changePassword,
            ForceLogin forceLogin,
            Migrate migrate
    ) {
        public record Config(
                String reload
        ) {}

        public record Unregister(
                String usage,
                String success
        ) {}

        public record ChangePassword(
                String usage,
                String success
        ) {}

        public record ForceLogin(
                String usage,
                String isAuthenticated,
                String success
        ) {}

        public record Migrate(
                String usage,
                String error,
                String success
        ) {}
    }

    public record Player(
            CheckPassword checkPassword,
            Register register,
            Unregister unregister,
            Login login,
            ChangePassword changePassword,
            Logout logout,
            Premium premium,
            Kick kick,
            Reminder reminder,
            Dialog dialog,
            BossBar bossBar,
            Title title,
            ActionBar actionBar
    ) {
        public record CheckPassword(
                String wrongPassword,
                String invalidLength,
                String invalidPattern,
                String passwordEmpty
        ) {}

        public record Register(
                String usage,
                String mismatch,
                String alreadyRegistered,
                String success
        ) {}

        public record Unregister(
                String usage,
                String success
        ) {}

        public record Login(
                String usage,
                String notRegistered,
                String alreadyLogged,
                String wrongPassword,
                String success
        ) {}

        public record ChangePassword(
                String usage,
                String success
        ) {}

        public record Logout(
                String logoutByPremium
        ) {}

        public record Premium(
                String enabled,
                String disabled
        ) {}

        public record Kick(
                String notAuth,
                String timeout,
                String realname,
                String tooManyAttempts,
                String ban,
                String invalidNickPattern,
                String ipLimitOnlineReached,
                String ipLimitRegisteredReached
        ) {}

        public record Reminder(
                String login,
                String register
        ) {}

        public record Dialog(
                Register register,
                Login login,
                Notifications notifications
        ) {
            public record Register(
                    String title,
                    String passwordField,
                    String repeatPasswordField,
                    String confirmButton
            ) {}

            public record Login(
                    String title,
                    String passwordField,
                    String confirmButton
            ) {}

            public record Notifications(
                    String wrongPassword,
                    String invalidLength,
                    String invalidPattern,
                    String mismatch,
                    String passwordEmpty
            ) {}
        }

        public record BossBar(
                String message
        ) {}

        public record Title(
                String title,
                String subTitle
        ) {}

        public record ActionBar(
                String message
        ) {}
    }

    public static ColorizedMessages load(MessagesConfig config) {
        return new ColorizedMessages(
                COLORIZER.colorize(config.prefix),
                COLORIZER.colorize(config.onlyPlayer),
                COLORIZER.colorize(config.queryError),
                COLORIZER.colorize(config.processing),
                COLORIZER.colorize(config.playerNotFound),
                COLORIZER.colorize(config.noPermission),
                new Admin(
                        COLORIZER.colorize(config.admin.usage),
                        new Admin.Config(
                                COLORIZER.colorize(config.admin.config.reload)
                        ),
                        new Admin.Unregister(
                                COLORIZER.colorize(config.admin.unregister.usage),
                                COLORIZER.colorize(config.admin.unregister.success)
                        ),
                        new Admin.ChangePassword(
                                COLORIZER.colorize(config.admin.changePassword.usage),
                                COLORIZER.colorize(config.admin.changePassword.success)
                        ),
                        new Admin.ForceLogin(
                                COLORIZER.colorize(config.admin.forceLogin.usage),
                                COLORIZER.colorize(config.admin.forceLogin.isAuthenticated),
                                COLORIZER.colorize(config.admin.forceLogin.success)
                        ),
                        new Admin.Migrate(
                                COLORIZER.colorize(config.admin.migrate.usage),
                                COLORIZER.colorize(config.admin.migrate.error),
                                COLORIZER.colorize(config.admin.migrate.success)
                        )
                ),
                new Player(
                        new Player.CheckPassword(
                                COLORIZER.colorize(config.player.checkPassword.wrongPassword),
                                COLORIZER.colorize(config.player.checkPassword.invalidLength),
                                COLORIZER.colorize(config.player.checkPassword.invalidPattern),
                                COLORIZER.colorize(config.player.checkPassword.passwordEmpty)
                        ),
                        new Player.Register(
                                COLORIZER.colorize(config.player.register.usage),
                                COLORIZER.colorize(config.player.register.mismatch),
                                COLORIZER.colorize(config.player.register.alreadyRegistered),
                                COLORIZER.colorize(config.player.register.success)
                        ),
                        new Player.Unregister(
                                COLORIZER.colorize(config.player.unregister.usage),
                                COLORIZER.colorize(config.player.unregister.success)
                        ),
                        new Player.Login(
                                COLORIZER.colorize(config.player.login.usage),
                                COLORIZER.colorize(config.player.login.notRegistered),
                                COLORIZER.colorize(config.player.login.alreadyLogged),
                                COLORIZER.colorize(config.player.login.wrongPassword),
                                COLORIZER.colorize(config.player.login.success)
                        ),
                        new Player.ChangePassword(
                                COLORIZER.colorize(config.player.changePassword.usage),
                                COLORIZER.colorize(config.player.changePassword.success)
                        ),
                        new Player.Logout(
                                COLORIZER.colorize(config.player.logout.logoutByPremium)
                        ),
                        new Player.Premium(
                                COLORIZER.colorize(config.player.premium.enabled),
                                COLORIZER.colorize(config.player.premium.disabled)
                        ),
                        new Player.Kick(
                                COLORIZER.colorize(config.player.kick.notAuth),
                                COLORIZER.colorize(config.player.kick.timeout),
                                COLORIZER.colorize(config.player.kick.realname),
                                COLORIZER.colorize(config.player.kick.tooManyAttempts),
                                COLORIZER.colorize(config.player.kick.ban),
                                COLORIZER.colorize(config.player.kick.invalidNickPattern),
                                COLORIZER.colorize(config.player.kick.ipLimitOnlineReached),
                                COLORIZER.colorize(config.player.kick.ipLimitRegisteredReached)
                        ),
                        new Player.Reminder(
                                COLORIZER.colorize(config.player.reminder.login),
                                COLORIZER.colorize(config.player.reminder.register)
                        ),
                        new Player.Dialog(
                                new Player.Dialog.Register(
                                        COLORIZER.colorize(config.player.dialog.register.title),
                                        COLORIZER.colorize(config.player.dialog.register.passwordField),
                                        COLORIZER.colorize(config.player.dialog.register.repeatPasswordField),
                                        COLORIZER.colorize(config.player.dialog.register.confirmButton)
                                ),
                                new Player.Dialog.Login(
                                        COLORIZER.colorize(config.player.dialog.login.title),
                                        COLORIZER.colorize(config.player.dialog.login.passwordField),
                                        COLORIZER.colorize(config.player.dialog.login.confirmButton)
                                ),
                                new Player.Dialog.Notifications(
                                        COLORIZER.colorize(config.player.dialog.notifications.wrongPassword),
                                        COLORIZER.colorize(config.player.dialog.notifications.invalidLength),
                                        COLORIZER.colorize(config.player.dialog.notifications.invalidPattern),
                                        COLORIZER.colorize(config.player.dialog.notifications.mismatch),
                                        COLORIZER.colorize(config.player.dialog.notifications.passwordEmpty)
                                )
                        ),
                        new Player.BossBar(
                                COLORIZER.colorize(config.player.bossBar.message)
                        ),
                        new Player.Title(
                                COLORIZER.colorize(config.player.title.title),
                                COLORIZER.colorize(config.player.title.subTitle)
                        ),
                        new Player.ActionBar(
                                COLORIZER.colorize(config.player.actionBar.message)
                        )
                )
        );
    }
}
