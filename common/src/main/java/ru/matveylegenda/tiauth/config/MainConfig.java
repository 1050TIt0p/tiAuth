package ru.matveylegenda.tiauth.config;

import net.elytrium.serializer.NameStyle;
import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.annotations.Transient;
import net.elytrium.serializer.language.object.YamlSerializable;
import ru.matveylegenda.tiauth.database.DatabaseType;
import ru.matveylegenda.tiauth.hash.HashType;
import ru.matveylegenda.tiauth.util.BarColor;
import ru.matveylegenda.tiauth.util.BarStyle;
import ru.matveylegenda.tiauth.util.colorizer.Serializer;

import java.nio.file.Paths;
import java.util.List;

public class MainConfig extends YamlSerializable {

    @Transient
    private static final SerializerConfig CONFIG = new SerializerConfig.Builder()
            .setCommentValueIndent(1)
            .setFieldNameStyle(NameStyle.MACRO_CASE)
            .setNodeNameStyle(NameStyle.KEBAB_CASE)
            .build();

    @Transient
    public static final MainConfig IMP = new MainConfig();

    public MainConfig() {
        super(Paths.get("plugins/tiAuth/config.yml"), CONFIG);
        this.servers = new Servers();
        this.database = new Database();
        this.auth = new Auth();
        this.bossBar = new BossBar();
        this.title = new Title();
        this.actionBar = new ActionBar();
    }

    @Comment({
            @CommentValue(" Доступные варианты:"),
            @CommentValue(" LEGACY - \"&fПример &#650dbdтекста\""),
            @CommentValue(" MINIMESSAGE - \"<white>Пример</white> <color:#650dbd>текста</color>\" (https://webui.advntr.dev/)")
    })
    public Serializer serializer = Serializer.LEGACY;

    @Comment({
            @CommentValue(" Доступные языки: RU, EN")
    })
    public MessagesConfig.Lang lang = MessagesConfig.Lang.RU;

    public Servers servers;

    @NewLine
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

    public Database database;

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
                value = @CommentValue(" Максимальное время ожидания соединения из пула"),
                at = Comment.At.SAME_LINE
        )
        public long connectionTimeoutMs = 30000;
        @Comment(
                value = @CommentValue(" Максимальное время простоя соединения в пуле. Применяется только если min-idle меньше max-pool-size"),
                at = Comment.At.SAME_LINE
        )
        public long idleTimeoutMs = 600000;
        @Comment(
                value = @CommentValue(" Максимальное время жизни соединения в пуле. После этого соединение будет закрыто и открыто новое, если требуется"),
                at = Comment.At.SAME_LINE
        )
        public long maxLifetimeMs = 1800000;
        @Comment(
                value = {
                        @CommentValue(" Максимальное количество соединений в пуле"),
                        @CommentValue(" Для H2 рекомендуется использовать небольшое количество соединений, например 2"),
                        @CommentValue(" Для MySQL и PostgreSQL можно выставить больше, например 10")
                },
                at = Comment.At.SAME_LINE
        )
        public int maxPoolSize = 2;
        @Comment(
                value = {
                        @CommentValue(" Минимальное количество простаивающих соединений в пуле. -1 = max-pool-size")
                },
                at = Comment.At.SAME_LINE
        )
        public int minIdle = -1;
    }

    public Auth auth;

    @NewLine
    public static class Auth {
        @Comment({
                @CommentValue(" Количество попыток ввода пароля")
        })
        public int loginAttempts = 3;

        @Comment({
                @CommentValue(" Банить ли игрока при исчерпании попыток авторизации")
        })
        public boolean banPlayer = true;

        @Comment({
                @CommentValue(" На сколько секунд банить игрока при исчерпании попыток авторизации")
        })
        public int banTime = 60;

        @Comment({
                @CommentValue(" Раз в сколько секунд игроку отправляется сообщение о требованием в регистрации/авторизации")
        })
        public int reminderInterval = 3;

        @Comment({
                @CommentValue(" Сколько секунд дается игроку на регистрацию/авторизацию")
        })
        public int timeoutSeconds = 60;

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
                @CommentValue(" Регулярное выражение для пароля")
        })
        public String passwordPattern = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]*$";

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

    public BossBar bossBar;

    @NewLine
    public static class BossBar {
        public boolean enabled = true;
        @Comment(
                value = @CommentValue(" PINK / BLUE / RED / GREEN / YELLOW / PURPLE / WHITE"),
                at = Comment.At.SAME_LINE
        )
        public BarColor color = BarColor.PURPLE;
        @Comment(
                value = @CommentValue(" SOLID / SEGMENTED_6 / SEGMENTED_10 / SEGMENTED_12 / SEGMENTED_20"),
                at = Comment.At.SAME_LINE
        )
        public BarStyle style = BarStyle.SEGMENTED_12;
    }

    public Title title;

    @NewLine
    public static class Title {
        public boolean enabled = false;
    }

    public ActionBar actionBar;

    @NewLine
    public static class ActionBar {
        public boolean enabled = false;
    }

    @NewLine
    @Comment({
            @CommentValue(" Регулярное выражение для ника")
    })
    public String nickPattern = "^[a-zA-Z0-9_]{3,16}$";
    @Comment({
            @CommentValue(" Максимальное количество одновременно играющих аккаунтов с одного IP")
    })
    public int maxOnlineAccountsPerIp = 10;
    @Comment({
            @CommentValue(" Максимальное количество зарегистрированных аккаунтов с одного IP")
    })
    public int maxRegisteredAccountsPerIp = 10;

    public Libraries libraries = new Libraries();

    @NewLine
    public static class Libraries {
        public SQLite sqlite = new SQLite();

        public static class SQLite {
            public String version = "3.50.3.0";
        }

        public H2 h2 = new H2();

        public static class H2 {
            public String version = "2.3.232";
        }

        public MySQL mysql = new MySQL();

        public static class MySQL {
            public String version = "9.4.0";
        }

        public PostgreSQL postgresql = new PostgreSQL();

        public static class PostgreSQL {
            public String version = "42.7.7";
        }
    }
}
