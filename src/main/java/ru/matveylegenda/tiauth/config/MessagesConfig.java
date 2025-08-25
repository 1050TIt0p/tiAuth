package ru.matveylegenda.tiauth.config;

import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public class MessagesConfig extends YamlSerializable {

    public String prefix = "&#8833ECᴀ&#7F65E7ᴜ&#7796E3ᴛ&#6EC8DEʜ &8»";
    public String onlyPlayer = "{prefix} &fКоманду может использовать только игрок";

    public Register register = new Register();

    @NewLine
    public static class Register {
        public String usage = "{prefix} &fИспользование: &#8833EC/register <пароль> <пароль>";
        public String mismatch = "{prefix} &fПароли не совпадают";
        public String alreadyRegistered = "{prefix} &fВы уже зарегистрированы";
        public String success = "{prefix} &fВы успешно зарегистрировались";
    }

    public Login login = new Login();

    @NewLine
    public static class Login {
        public String usage = "{prefix} &fИспользование: &#8833EC/login <пароль>";
        public String notRegistered = "{prefix} &fВы еще не зарегистрированы";
        public String alreadyLogged = "{prefix} &fВы уже авторизованы";
        public String wrongPassword = "{prefix} &fНеверный пароль";
        public String success = "{prefix} &fВы успешно авторизовались";
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

    public Database database = new Database();

    @NewLine
    public static class Database {
        public String queryError = "{prefix} &fПроизошла ошибка при запросе к базе данных. Сообщите администрации!";
    }
}
