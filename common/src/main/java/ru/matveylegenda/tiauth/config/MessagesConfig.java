package ru.matveylegenda.tiauth.config;

import net.elytrium.serializer.NameStyle;
import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.annotations.Transient;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.nio.file.Path;

public class MessagesConfig extends YamlSerializable {

    @Transient
    private static final SerializerConfig CONFIG = new SerializerConfig.Builder()
            .setCommentValueIndent(1)
            .setFieldNameStyle(NameStyle.MACRO_CASE)
            .setNodeNameStyle(NameStyle.KEBAB_CASE)
            .build();

    @Transient
    public static final MessagesConfig IMP = new MessagesConfig(getMessageConfigPath());

    public MessagesConfig(Path path) {
        super(path, CONFIG);
        this.admin = new Admin();
        this.player = new Player();
        loadLang(MainConfig.IMP.lang);
    }

    private static Path getMessageConfigPath() {
        return switch (MainConfig.IMP.lang) {
            case RU -> Path.of("plugins/tiAuth", "lang", "messages_ru.yml");
            case EN -> Path.of("plugins/tiAuth", "lang", "messages_en.yml");
        };
    }

    public String prefix;
    public String onlyPlayer;
    public String queryError;
    public String processing;
    public String playerNotFound;
    public String noPermission;

    public Admin admin;
    public Player player;

    @NewLine
    public static class Admin {
        public String usage;
        public Config config = new Config();
        public Unregister unregister = new Unregister();
        public ChangePassword changePassword = new ChangePassword();
        public ForceLogin forceLogin = new ForceLogin();
        public ForceRegister forceRegister = new ForceRegister();
        public Migrate migrate = new Migrate();

        @NewLine
        public static class Config {
            public String reload;
        }

        @NewLine
        public static class Unregister {
            public String usage;
            public String success;
        }

        @NewLine
        public static class ChangePassword {
            public String usage;
            public String success;
        }

        @NewLine
        public static class ForceLogin {
            public String usage;
            public String isAuthenticated;
            public String success;
        }

        @NewLine
        public static class ForceRegister {
            public String usage;
            public String alreadyRegistered;
            public String success;
        }

        @NewLine
        public static class Migrate {
            public String usage;
            public String error;
            public String success;
        }
    }

    @NewLine
    public static class Player {
        public CheckPassword checkPassword = new CheckPassword();
        public Register register = new Register();
        public Unregister unregister = new Unregister();
        public Login login = new Login();
        public ChangePassword changePassword = new ChangePassword();
        public Logout logout = new Logout();
        public Premium premium = new Premium();
        public Kick kick = new Kick();
        public Reminder reminder = new Reminder();
        public Dialog dialog = new Dialog();
        public BossBar bossBar = new BossBar();
        public Title title = new Title();
        public ActionBar actionBar = new ActionBar();

        public static class CheckPassword {
            public String wrongPassword;
            public String invalidLength;
            public String invalidPattern;
            public String passwordEmpty;
        }

        @NewLine
        public static class Register {
            public String usage;
            public String mismatch;
            public String alreadyRegistered;
            public String success;
        }

        @NewLine
        public static class Unregister {
            public String usage;
            public String success;
        }

        @NewLine
        public static class Login {
            public String usage;
            public String notRegistered;
            public String alreadyLogged;
            public String wrongPassword;
            public String success;
        }

        @NewLine
        public static class ChangePassword {
            public String usage;
            public String success;
        }

        @NewLine
        public static class Logout {
            public String logoutByPremium;
        }

        @NewLine
        public static class Premium {
            public String enabled;
            public String disabled;
        }

        @NewLine
        public static class Kick {
            public String timeout;
            public String realname;
            public String tooManyAttempts;
            public String ban;
            public String invalidNickPattern;
            public String ipLimitOnlineReached;
            public String ipLimitRegisteredReached;
        }

        @NewLine
        public static class Reminder {
            public String login;
            public String register;
        }

        @NewLine
        public static class Dialog {
            public Register register = new Register();
            public Login login = new Login();
            public Notifications notifications = new Notifications();

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

        @NewLine
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

    public void loadLang(Lang lang) {
        switch (lang) {
            case RU -> {
                prefix = "&#8833ECᴀ&#7F65E7ᴜ&#7796E3ᴛ&#6EC8DEʜ &8»";
                onlyPlayer = "{prefix} &fКоманду может использовать только игрок";
                queryError = "{prefix} &fПроизошла ошибка при запросе к базе данных. Сообщите администрации!";
                processing = "{prefix} &fПодождите, пока завершится обработка предыдущего запроса";
                playerNotFound = "{prefix} &fИгрок не найден";
                noPermission = "{prefix} &fНедостаточно прав";

                admin.usage = "{prefix} &fИспользование: &#8833EC/tiauth <reload|unregister|changepassword|forcelogin>";
                admin.config.reload = "{prefix} &fКонфиги перезагружены";
                admin.unregister.usage = "{prefix} &fИспользование: &#8833EC/tiauth unregister";
                admin.unregister.success = "{prefix} &fВы успешно удалили аккаунт игрока &#8833EC{player}";
                admin.changePassword.usage = "{prefix} &fИспользование: &#8833EC/tiauth changepassword <игрок> <пароль>";
                admin.changePassword.success = "{prefix} &fВы успешно изменили пароль игрока &#8833EC{player}";
                admin.forceLogin.usage = "{prefix} &fИспользование: &#8833EC/tiauth forcelogin <ник>";
                admin.forceLogin.isAuthenticated = "{prefix} &fИгрок &#8833EC{player} &fуже авторизован";
                admin.forceLogin.success = "{prefix} &fВы успешно авторизовали игрока &#8833EC{player}";
                admin.forceRegister.usage = "{prefix} &fИспользование: &#8833EC/tiauth forceregister <ник> <пароль>";
                admin.forceRegister.alreadyRegistered = "{prefix} &fИгрок &#8833EC{player} &fуже зарегистрирован";
                admin.forceRegister.success = "{prefix} &fВы успешно зарегистрировали аккаунт &#8833EC{player}";
                admin.migrate.usage = "{prefix} &fИспользование: &#8833EC/tiauth migrate <sourceplugin> <sourcedatabase> [file] [user] [password] [host] [port] [name]";
                admin.migrate.error = "{prefix} &fПроизошла ошибка при миграции базы данных";
                admin.migrate.success = "{prefix} &fБаза данных успешно мигрирована";

                player.checkPassword.wrongPassword = "{prefix} &fНеверный пароль";
                player.checkPassword.invalidLength = "{prefix} &fДлина пароля должна быть от &#8833EC{min} &fдо &#8833EC{max} &fсимволов";
                player.checkPassword.invalidPattern = "{prefix} &fПароль содержит недопустимые символы";
                player.checkPassword.passwordEmpty = "{prefix} &fПароль не может быть пустым";
                player.register.usage = "{prefix} &fИспользование: &#8833EC/register <пароль> <пароль>";
                player.register.mismatch = "{prefix} &fПароли не совпадают";
                player.register.alreadyRegistered = "{prefix} &fВы уже зарегистрированы";
                player.register.success = "{prefix} &fВы успешно зарегистрировались";
                player.unregister.usage = "{prefix} &fИспользование: &#8833EC/unregister <пароль>";
                player.unregister.success = "{prefix} &fВы успешно удалили аккаунт";
                player.login.usage = "{prefix} &fИспользование: &#8833EC/login <пароль>";
                player.login.notRegistered = "{prefix} &fВы еще не зарегистрированы";
                player.login.alreadyLogged = "{prefix} &fВы уже авторизованы";
                player.login.wrongPassword = "{prefix} &fНеверный пароль. Осталось &#8833EC{attempts} &fпопыток";
                player.login.success = "{prefix} &fВы успешно авторизовались";
                player.changePassword.usage = "{prefix} &fИспользование: &#8833EC/changepassword <старый пароль> <новый пароль>";
                player.changePassword.success = "{prefix} &fВы успешно изменили пароль";
                player.logout.logoutByPremium = "{prefix} &fВы не можете разлогиниться из-за &#8833ECпремиум режима";
                player.premium.enabled = "{prefix} &fПремиум режим &#8833ECвключен\n&fЕсли у вас нет лицензии Minecraft, выключите режим прописав /premium, иначе вы не сможете войти на сервер";
                player.premium.disabled = "{prefix} &fПремиум режим &#8833ECвыключен";
                player.kick.timeout = "{prefix} &fВы не успели авторизоваться";
                player.kick.realname = "{prefix} &fПравильный ник: &#8833EC{realname}\n&fВаш ник: &#8833EC{name}";
                player.kick.tooManyAttempts = "{prefix} &fВы превысили количество попыток для ввода пароля";
                player.kick.ban = "{prefix} &fВаш аккаунт заблокирован на &#8833EC{time} &fсекунд за превышение попыток ввода пароля";
                player.kick.invalidNickPattern = "{prefix} &fВаш ник содержит запрещенные символы";
                player.kick.ipLimitOnlineReached = "{prefix} &fС этого IP-адреса играет максимальное количество аккаунтов";
                player.kick.ipLimitRegisteredReached = "{prefix} &fС этого IP-адреса зарегистрировано слишком много аккаунтов";
                player.reminder.login = "{prefix} &fАвторизируйтесь командой &#8833EC/login <пароль>";
                player.reminder.register = "{prefix} &fЗарегистрируйтесь командой &#8833EC/register <пароль> <пароль>";
                player.dialog.register.title = "Регистрация";
                player.dialog.register.passwordField = "Пароль";
                player.dialog.register.repeatPasswordField = "Повторите пароль";
                player.dialog.register.confirmButton = "Зарегистрироваться";
                player.dialog.login.title = "Авторизация";
                player.dialog.login.passwordField = "Пароль";
                player.dialog.login.confirmButton = "Авторизоваться";
                player.dialog.notifications.wrongPassword = "&cНеверный пароль\nОсталось {attempts} попыток";
                player.dialog.notifications.invalidLength = "&cДлина пароля должна быть от {min} до {max} символов";
                player.dialog.notifications.invalidPattern = "&cПароль содержит недопустимые символы";
                player.dialog.notifications.mismatch = "&cПароли не совпадают";
                player.dialog.notifications.passwordEmpty = "&cПароль не может быть пустым";
                player.bossBar.message = "{prefix} &fОсталось &#8833EC{time} &fсекунд";
                player.title.title = "{prefix}";
                player.title.subTitle = "&fОсталось &#8833EC{time} &fсекунд";
                player.actionBar.message = "{prefix} &fОсталось &#8833EC{time} &fсекунд";
            }
            case EN -> {
                prefix = "&#8833ECᴀ&#7F65E7ᴜ&#7796E3ᴛ&#6EC8DEʜ &8»";
                onlyPlayer = "{prefix} &fThis command can only be used by a player";
                queryError = "{prefix} &fAn error occurred while querying the database. Please contact the administration!";
                processing = "{prefix} &fPlease wait until the previous request is processed";
                playerNotFound = "{prefix} &fPlayer not found";
                noPermission = "{prefix} &fInsufficient permissions";

                admin.usage = "{prefix} &fUsage: &#8833EC/tiauth <reload|unregister|changepassword|forcelogin>";
                admin.config.reload = "{prefix} &fConfigs reloaded";
                admin.unregister.usage = "{prefix} &fUsage: &#8833EC/tiauth unregister";
                admin.unregister.success = "{prefix} &fYou have successfully deleted the account of player &#8833EC{player}";
                admin.changePassword.usage = "{prefix} &fUsage: &#8833EC/tiauth changepassword <player> <password>";
                admin.changePassword.success = "{prefix} &fYou have successfully changed the password for player &#8833EC{player}";
                admin.forceLogin.usage = "{prefix} &fUsage: &#8833EC/tiauth forcelogin <nickname>";
                admin.forceLogin.isAuthenticated = "{prefix} &fPlayer &#8833EC{player} &fis already authenticated";
                admin.forceLogin.success = "{prefix} &fYou have successfully authenticated player &#8833EC{player}";
                admin.forceRegister.usage = "{prefix} &fUsage: &#8833EC/tiauth forceregister <username> <password>";
                admin.forceRegister.alreadyRegistered = "{prefix} &fPlayer &#8833EC{player} &fis already registered";
                admin.forceRegister.success = "{prefix} &fYou have successfully registered the account &#8833EC{player}";
                admin.migrate.usage = "{prefix} &Usage: &#8833EC/tiauth migrate <sourceplugin> <sourcedatabase> [file] [user] [password] [host] [port] [name]";
                admin.migrate.error = "{prefix} &fAn error occurred during database migration";
                admin.migrate.success = "{prefix} &fDatabase has been successfully migrated";

                player.checkPassword.wrongPassword = "{prefix} &fWrong password";
                player.checkPassword.invalidLength = "{prefix} &fPassword length must be between &#8833EC{min} &fand &#8833EC{max} &fcharacters";
                player.checkPassword.invalidPattern = "{prefix} &fPassword contains invalid characters";
                player.checkPassword.passwordEmpty = "{prefix} &fPassword cannot be empty";
                player.register.usage = "{prefix} &fUsage: &#8833EC/register <password> <password>";
                player.register.mismatch = "{prefix} &fPasswords do not match";
                player.register.alreadyRegistered = "{prefix} &fYou are already registered";
                player.register.success = "{prefix} &fYou have successfully registered";
                player.unregister.usage = "{prefix} &fUsage: &#8833EC/unregister <password>";
                player.unregister.success = "{prefix} &fYou have successfully deleted your account";
                player.login.usage = "{prefix} &fUsage: &#8833EC/login <password>";
                player.login.notRegistered = "{prefix} &fYou are not registered yet";
                player.login.alreadyLogged = "{prefix} &fYou are already logged in";
                player.login.wrongPassword = "{prefix} &fWrong password. &#8833EC{attempts} attempts left";
                player.login.success = "{prefix} &fYou have successfully logged in";
                player.changePassword.usage = "{prefix} &fUsage: &#8833EC/changepassword <old password> <new password>";
                player.changePassword.success = "{prefix} &fYou have successfully changed your password";
                player.logout.logoutByPremium = "{prefix} &fYou cannot log out due to &#8833ECpremium mode";
                player.premium.enabled = "{prefix} &fPremium mode &#8833ECenabled\n&fIf you don't have a Minecraft license, disable it using /premium, otherwise you won't be able to join the server";
                player.premium.disabled = "{prefix} &fPremium mode &#8833ECdisabled";
                player.kick.timeout = "{prefix} &fYou did not authenticate in time";
                player.kick.realname = "{prefix} &fCorrect nickname: &#8833EC{realname}\n&fYour nickname: &#8833EC{name}";
                player.kick.tooManyAttempts = "{prefix} &fYou exceeded the number of password attempts";
                player.kick.ban = "{prefix} &fYour account has been locked for &#8833EC{time} &fseconds due to exceeding password attempts";
                player.kick.invalidNickPattern = "{prefix} &fYour nickname contains invalid characters";
                player.kick.ipLimitOnlineReached = "{prefix} &fToo many accounts are currently playing from this IP address";
                player.kick.ipLimitRegisteredReached = "{prefix} &fToo many accounts registered from this IP address";
                player.reminder.login = "{prefix} &fAuthenticate using &#8833EC/login <password>";
                player.reminder.register = "{prefix} &fRegister using &#8833EC/register <password> <password>";
                player.dialog.register.title = "Registration";
                player.dialog.register.passwordField = "Password";
                player.dialog.register.repeatPasswordField = "Repeat Password";
                player.dialog.register.confirmButton = "Register";
                player.dialog.login.title = "Login";
                player.dialog.login.passwordField = "Password";
                player.dialog.login.confirmButton = "Login";
                player.dialog.notifications.wrongPassword = "&cWrong password\n{attempts} attempts left";
                player.dialog.notifications.invalidLength = "&cPassword length must be between {min} and {max} characters";
                player.dialog.notifications.invalidPattern = "&cPassword contains invalid characters";
                player.dialog.notifications.mismatch = "&cPasswords do not match";
                player.dialog.notifications.passwordEmpty = "&cPassword cannot be empty";
                player.bossBar.message = "{prefix} &fTime remaining: &#8833EC{time} &fseconds";
                player.title.title = "{prefix}";
                player.title.subTitle = "&fTime remaining: &#8833EC{time} &fseconds";
                player.actionBar.message = "{prefix} &fTime remaining: &#8833EC{time} &fseconds";
            }
        }
    }

    public enum Lang {
        RU,
        EN
    }
}
