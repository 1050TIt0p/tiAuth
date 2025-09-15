package ru.matveylegenda.tiauth.config;

import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public class MessagesConfig extends YamlSerializable {

    public String prefix = "&#8833ECᴀ&#7F65E7ᴜ&#7796E3ᴛ&#6EC8DEʜ &8»";
    public String onlyPlayer = "{prefix} &fКоманду может использовать только игрок";
    public String queryError = "{prefix} &fПроизошла ошибка при запросе к базе данных. Сообщите администрации!";
    public String playerNotFound = "{prefix} &fИгрок не найден";
    public String noPermission = "{prefix} &fНедостаточно прав";

    public Admin admin = new Admin();

    @NewLine
    public static class Admin {
        public String usage = "{prefix} &fИспользование: &#8833EC/tiauth <reload|unregister|changepassword|forcelogin>";

        public Config config = new Config();

        public static class Config {
            public String reload = "{prefix} &fКонфиги перезагружены";
        }

        public Unregister unregister = new Unregister();

        @NewLine
        public static class Unregister {
            public String usage = "{prefix} &fИспользование: &#8833EC/tiauth unregister";
            public String success = "{prefix} &fВы успешно удалили аккаунт игрока &#8833EC{player}";
        }

        public ChangePassword changePassword = new ChangePassword();

        @NewLine
        public static class ChangePassword {
            public String usage = "{prefix} &fИспользование: &#8833EC/tiauth changepassword <игрок> <пароль>";
            public String success = "{prefix} &fВы успешно изменили пароль игрока &#8833EC{player}";
        }

        public ForceLogin forceLogin = new ForceLogin();

        @NewLine
        public static class ForceLogin {
            public String usage = "{prefix} &fИспользование: &#8833EC/tiauth forcelogin <ник>";
            public String isAuthenticated = "{prefix} &fИгрок &#8833EC{player} &fуже авторизован";
            public String success = "{prefix} &fВы успешно авторизовали игрока &#8833EC{player}";
        }
    }

    public Player player = new Player();

    @NewLine
    public static class Player {
        public CheckPassword checkPassword = new CheckPassword();

        public static class CheckPassword {
            public String wrongPassword = "{prefix} &fНеверный пароль";
            public String invalidLength = "{prefix} &fДлина пароля должна быть от &#8833EC{min} &fдо &#8833EC{max} &fсимволов";
        }

        public Register register = new Register();

        @NewLine
        public static class Register {
            public String usage = "{prefix} &fИспользование: &#8833EC/register <пароль> <пароль>";
            public String mismatch = "{prefix} &fПароли не совпадают";
            public String alreadyRegistered = "{prefix} &fВы уже зарегистрированы";
            public String success = "{prefix} &fВы успешно зарегистрировались";
        }

        public Unregister unregister = new Unregister();

        @NewLine
        public static class Unregister {
            public String usage = "{prefix} &fИспользование: &#8833EC/unregister <пароль>";
            public String success = "{prefix} &fВы успешно удалили аккаунт";
        }


        public Login login = new Login();

        @NewLine
        public static class Login {
            public String usage = "{prefix} &fИспользование: &#8833EC/login <пароль>";
            public String notRegistered = "{prefix} &fВы еще не зарегистрированы";
            public String alreadyLogged = "{prefix} &fВы уже авторизованы";
            public String success = "{prefix} &fВы успешно авторизовались";
        }

        public ChangePassword changePassword = new ChangePassword();

        @NewLine
        public static class ChangePassword {
            public String usage = "{prefix} &fИспользование: &#8833EC/changepassword <старый пароль> <новый пароль>";
            public String success = "{prefix} &fВы успешно изменили пароль";
        }

        public Logout logout = new Logout();

        @NewLine
        public static class Logout {
            public String logoutByPremium = "{prefix} &fВы не можете разлогиниться из-за &#8833ECпремиум режима";
        }

        public Premium premium = new Premium();

        @NewLine
        public static class Premium {
            public String enabled = "{prefix} &fПремиум режим &#8833ECвключен\n" +
                    "&fЕсли у вас нет лицензии Minecraft, выключите режим прописав /premium, иначе вы не сможете войти на сервер";
            public String disabled = "{prefix} &fПремиум режим &#8833ECвыключен";
        }

        public Kick kick = new Kick();

        @NewLine
        public static class Kick {
            public String notAuth = "{prefix} &fВы не авторизованы";
            public String timeout = "{prefix} &fВы не успели авторизоваться";
            public String realname = "{prefix} &fПравильный ник: &#8833EC{realname}\n&fВаш ник: &#8833EC{name}";
        }

        public Reminder reminder = new Reminder();

        @NewLine
        public static class Reminder {
            public String login = "{prefix} &fАвторизируйтесь командой &#8833EC/login <пароль>";
            public String register = "{prefix} &fЗарегистрируйтесь командой &#8833EC/register <пароль> <пароль>";
        }

        public Dialog dialog = new Dialog();

        @NewLine
        public static class Dialog {
            public Register register = new Register();

            public static class Register {
                public String title = "Регистрация";
                public String passwordField = "Пароль";
                public String repeatPasswordField = "Повторите пароль";
                public String confirmButton = "Зарегистрироваться";
            }

            public Login login = new Login();

            public static class Login {
                public String title = "Авторизация";
                public String passwordField = "Пароль";
                public String confirmButton = "Авторизоваться";
            }

            public Notifications notifications = new Notifications();

            public static class Notifications {
                public String wrongPassword = "&cНеверный пароль";
                public String invalidLength = "&cДлина пароля должна быть от {min} до {max} символов";
                public String mismatch = "&cПароли не совпадают";
            }
        }

        public BossBar bossBar = new BossBar();

        @NewLine
        public static class BossBar {
            public String message = "{prefix} &fОсталось &#8833EC{time} &fсекунд";
        }

        public Title title = new Title();

        public static class Title {
            public String title = "{prefix}";
            public String subTitle = "&fОсталось &#8833EC{time} &fсекунд";
        }

        public ActionBar actionBar = new ActionBar();

        public static class ActionBar {
            public String message = "{prefix} &fОсталось &#8833EC{time} &fсекунд";
        }
    }
}
