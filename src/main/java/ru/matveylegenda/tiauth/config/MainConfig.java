package ru.matveylegenda.tiauth.config;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;
import ru.matveylegenda.tiauth.database.DatabaseType;
import ru.matveylegenda.tiauth.hash.HashType;
import ru.matveylegenda.tiauth.util.BarColor;
import ru.matveylegenda.tiauth.util.BarStyle;

import java.util.List;

public class MainConfig extends YamlSerializable {

    public Servers servers = new Servers();

    public static class Servers {
        @Comment({
                @CommentValue(" Использовать ли виртуальный сервер NanoLimbo для сервера авторизации"),
                @CommentValue(" Настройка виртуального сервера в plugins/tiAuth/limbo/settings.yml"),
                @CommentValue(" Функция не тестировалась должным образом, возможны баги")
        })
        public boolean useVirtualServer = false;

        @NewLine
        @Comment({
                @CommentValue(" Сервер авторизации на который будет перемещать игроков для регистрации/авторизации"),
                @CommentValue(" При использовании виртуального сервера убедитесь, что в конфигурации BungeeCord у вас нет сервера с таким же названием")
        })
        public String auth = "auth";

        @Comment({
                @CommentValue(" Бэкенд сервер на который будет перемещать игроков после регистрации/авторизации")
        })
        public String backend = "hub";
    }

    public Database database = new Database();

    @NewLine
    public static class Database {
        @Comment({
                @CommentValue(" Тип базы данных"),
                @CommentValue(" Доступные варианты: SQLITE, H2, MYSQL, POSTGRESQL")
        })
        public DatabaseType type = DatabaseType.H2;
        public String host;
        public int port;
        public String database;
        public String user;
        public String password;

        @NewLine
        @Comment({
                @CommentValue(" Параметры пула соединений (H2, MySQL, PostgreSQL")
        })
        @Comment(
                value = {
                        @CommentValue(" Максимальное время ожидания соединения из пула")
                },
                at = Comment.At.SAME_LINE
        )
        public long connectionTimeoutMs = 30000;
        @Comment(
                value = {
                        @CommentValue(" Максимальное время простоя соединения в пуле. Применяется только если min-idle меньше max-pool-size")
                },
                at = Comment.At.SAME_LINE
        )
        public long idleTimeoutMs = 600000;
        @Comment(
                value = {
                        @CommentValue(" Максимальное время жизни соединения в пуле. После этого соединение будет закрыто и открыто новое, если требуется")
                },
                at = Comment.At.SAME_LINE
        )
        public long maxLifetimeMs = 1800000;
        @Comment(
                value = {
                        @CommentValue(" Максимальное количество соединений в пуле. Для H2 рекомендуется использовать небольшое количество соединений, например 2")
                },
                at = Comment.At.SAME_LINE
        )
        public int maxPoolSize = 10;
        @Comment(
                value = {
                        @CommentValue(" Минимальное количество простаивающих соединений в пуле. -1 = max-pool-size")
                },
                at = Comment.At.SAME_LINE
        )
        public int minIdle = -1;
    }

    public Auth auth = new Auth();

    @NewLine
    public static class Auth {
        @Comment({
                @CommentValue(" Раз в сколько секунд игроку отправляется сообщение о требованием в регистрации/авторизации")
        })
        public int reminderInterval = 3;

        @Comment({
                @CommentValue(" Сколько секунд дается игроку на регистрацию/авторизацию")
        })
        public int timeoutSeconds = 30;

        @Comment({
                @CommentValue(" Сколько игрок может заходить без авторизации, если его IP не изменился")
        })
        public int sessionLifetimeMinutes = 60;

        @Comment({
                @CommentValue(" Минимальная длина пароля")
        })
        public int minPasswordLength = 6;

        @Comment({
                @CommentValue(" Максимальная длина пароля")
        })
        public int maxPasswordLength = 32;

        @Comment({
                @CommentValue(" Алгоритм хеширования пароля"),
                @CommentValue(" Доступные варианты:"),
                @CommentValue(" BCRYPT"),
                @CommentValue(" SHA256")
        })
        public HashType hashAlgorithm = HashType.BCRYPT;

        @Comment({
                @CommentValue(" Команды, которые можно использовать во время авторизации")
        })
        public List<String> allowedCommands = List.of(
                "/login",
                "/log",
                "/l",
                "/register",
                "/reg"
        );

        @Comment({
                @CommentValue(" Использовать ли диалоговое окно для регистрации/авторизации"),
                @CommentValue(" Работает только на клиентах 1.21.6+")
        })
        public boolean useDialogs = true;
    }

    public BossBar bossBar = new BossBar();

    @NewLine
    public static class BossBar {
        public boolean enabled = true;
        @Comment(
                value = {
                        @CommentValue(" PINK / BLUE / RED / GREEN / YELLOW / PURPLE / WHITE"),
                },
                at = Comment.At.APPEND
        )
        public BarColor color = BarColor.PURPLE;
        @Comment(
                value = {
                        @CommentValue(" SOLID / SEGMENTED_6 / SEGMENTED_10 / SEGMENTED_12 / SEGMENTED_20"),
                },
                at = Comment.At.APPEND
        )
        public BarStyle style = BarStyle.SEGMENTED_12;
    }

    public Title title = new Title();

    public static class Title {
        public boolean enabled = false;
    }

    public ActionBar actionBar = new ActionBar();

    public static class ActionBar {
        public boolean enabled = false;
    }
}
