package ru.matveylegenda.tiauth.config;

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
            .build();

    @Transient
    public static final MainConfig IMP = new MainConfig();

    public MainConfig() {
        super(Paths.get("plugins/tiAuth/config.yml"), CONFIG);
        this.servers = new Servers();
        this.database = new Database();
        this.auth = new Auth();
        this.bossBar = new BossBar();
        this.title = new TitleConfig();
        this.actionBar = new ActionBar();
    }

    @Comment({
            @CommentValue("Available options:"),
            @CommentValue("LEGACY - \"&fExample &#650dbdtext\""),
            @CommentValue("MINIMESSAGE - \"<white>Example</white> <color:#650dbd>text</color>\" (https://webui.advntr.dev/)")
    })
    public Serializer serializer = Serializer.LEGACY;

    @Comment({
            @CommentValue("Available languages: RU, EN (file messages_<lang>.yml in lang folder)")
    })
    public String lang = "EN";

    public Servers servers;

    @NewLine
    public static class Servers {
        @Comment({
                @CommentValue("Server selection mode after authentication"),
                @CommentValue("BACKEND - always send to the server from the backend setting"),
                @CommentValue("FORCED_HOST - send to the server from proxy forced_hosts if available")
        })
        public PostAuthServerMode postAuthServerMode = PostAuthServerMode.BACKEND;

        @NewLine
        @Comment({
                @CommentValue("Use NanoLimbo virtual server for the auth server"),
                @CommentValue("Virtual server settings in plugins/tiAuth/limbo/settings.yml"),
                @CommentValue("This feature has not been properly tested, bugs may occur")
        })
        public boolean useVirtualServer = false;

        @NewLine
        @Comment({
                @CommentValue("Auth server where players are sent for registration/login"),
                @CommentValue("When using a virtual server, make sure there is no server with the same name in your BungeeCord config")
        })
        public String auth = "auth";

        @Comment({
                @CommentValue("Backend server where players are sent after registration/login")
        })
        public String backend = "hub";

        @NewLine
        @Comment({
                @CommentValue("Forced hosts settings"),
                @CommentValue("List of servers considered as forced hosts"),
                @CommentValue("If empty - all servers except auth are considered"),
                @CommentValue("If not empty - only servers from the list are considered")
        })
        public ForcedHosts forcedHosts = new ForcedHosts();

        public static class ForcedHosts {
            public List<String> servers = List.of();
        }
    }

    public enum PostAuthServerMode {
        BACKEND,
        FORCED_HOST
    }

    public Database database;

    @NewLine
    public static class Database {
        @Comment({
                @CommentValue("Database type"),
                @CommentValue("Available options: SQLITE, H2, MYSQL, POSTGRESQL")
        })
        public DatabaseType type = DatabaseType.SQLITE;
        public String host;
        public int port;
        public String database;
        public String user;
        public String password;

        @NewLine
        @Comment({
                @CommentValue("Connection pool settings (H2, MySQL, PostgreSQL")
        })
        @Comment(
                value = @CommentValue("Maximum time to wait for a connection from the pool"),
                at = Comment.At.SAME_LINE
        )
        public long connectionTimeoutMs = 30000;
        @Comment(
                value = @CommentValue("Maximum idle time for a connection in the pool. Only applies if min-idle is less than max-pool-size"),
                at = Comment.At.SAME_LINE
        )
        public long idleTimeoutMs = 600000;
        @Comment(
                value = @CommentValue("Maximum lifetime of a connection in the pool. After this, the connection will be closed and a new one opened if needed"),
                at = Comment.At.SAME_LINE
        )
        public long maxLifetimeMs = 1800000;
        @Comment(
                value = {
                        @CommentValue("Maximum number of connections in the pool"),
                        @CommentValue("For H2 it is recommended to use a small number of connections, e.g. 2"),
                        @CommentValue("For MySQL and PostgreSQL you can set more, e.g. 10")
                },
                at = Comment.At.SAME_LINE
        )
        public int maxPoolSize = 2;
        @Comment(
                value = {
                        @CommentValue("Minimum number of idle connections in the pool. -1 = max-pool-size")
                },
                at = Comment.At.SAME_LINE
        )
        public int minIdle = -1;
    }

    public Auth auth;

    @NewLine
    public static class Auth {
        @Comment({
                @CommentValue("Number of login attempts")
        })
        public int loginAttempts = 3;

        @Comment({
                @CommentValue("Ban player when login attempts are exhausted")
        })
        public boolean banPlayer = true;

        @Comment({
                @CommentValue("How many seconds to ban the player when login attempts are exhausted")
        })
        public int banTime = 60;

        @Comment({
                @CommentValue("How often (in seconds) the player is reminded to register/login")
        })
        public int reminderInterval = 3;

        @Comment({
                @CommentValue("How many seconds the player has to register/login")
        })
        public int timeoutSeconds = 60;

        @Comment({
                @CommentValue("How long a player can rejoin without logging in if their IP hasn't changed")
        })
        public int sessionLifetimeMinutes = 60;

        @Comment({
                @CommentValue("Minimum password length")
        })
        public int minPasswordLength = 6;

        @Comment({
                @CommentValue("Maximum password length")
        })
        public int maxPasswordLength = 32;

        @Comment({
                @CommentValue("Password regex pattern")
        })
        public String passwordPattern = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]*$";

        @Comment({
                @CommentValue("Password hashing algorithm"),
                @CommentValue("Available options:"),
                @CommentValue("BCRYPT (recommended)"),
                @CommentValue("SHA256"),
                @CommentValue("ARGON2")
        })
        public HashType hashAlgorithm = HashType.BCRYPT;

        @Comment({
                @CommentValue("Bcrypt algorithm cost"),
                @CommentValue("Default value is optimal, do not change if you don't know how it works!")
        })
        public int bcryptCost = 12;

        @Comment({
                @CommentValue("Argon2 algorithm settings"),
                @CommentValue("Default values are optimal, do not change if you don't know how it works!")
        })
        public int argon2Iterations = 2;
        public int argon2Memory = 65536;
        public int argon2Parallelism = 1;

        @Comment({
                @CommentValue("Commands that can be used while not authenticated")
        })
        public List<String> allowedCommands = List.of(
                "/login",
                "/log",
                "/l",
                "/register",
                "/reg",
                "/2fa",
                "/totp"
        );

        @Comment({
                @CommentValue("Use dialog window for registration/login"),
                @CommentValue("Only works on clients 1.21.6+")
        })
        public boolean useDialogs = true;

        @Comment({
                @CommentValue("Require password confirmation in /register")
        })
        public boolean repeatPasswordWhenRegister = true;

        @NewLine
        @Comment({
                @CommentValue("Two-factor authentication settings (2FA/TOTP)")
        })
        public Totp totp = new Totp();

        public static class Totp {
            @Comment({
                    @CommentValue("Enable 2FA")
            })
            public boolean enabled = true;

            @Comment({
                    @CommentValue("Issuer name displayed in the authenticator app")
            })
            public String issuer = "tiAuth";

            @Comment({
                    @CommentValue("URL for QR code generation. {data} is replaced with otpauth:// URI")
            })
            public String qrGeneratorUrl = "https://api.qrserver.com/v1/create-qr-code/?data={data}&size=200x200&ecc=M&margin=30";

            @Comment({
                    @CommentValue("Require password when enabling 2FA")
            })
            public boolean needPassword = true;

            @Comment({
                    @CommentValue("Number of recovery codes")
            })
            public int recoveryCodesAmount = 16;

            @NewLine
            @Comment({
                    @CommentValue("Maximum invalid TOTP attempts before ban")
            })
            public int maxAttempts = 3;

            @Comment({
                    @CommentValue("Ban player when TOTP attempts are exhausted")
            })
            public boolean banPlayer = true;

            @Comment({
                    @CommentValue("How many seconds to ban the player when TOTP attempts are exhausted")
            })
            public int banTime = 60;
        }
    }

    public BossBar bossBar;

    @NewLine
    public static class BossBar {
        public boolean enabled = true;
        @Comment(
                value = @CommentValue("PINK / BLUE / RED / GREEN / YELLOW / PURPLE / WHITE"),
                at = Comment.At.SAME_LINE
        )
        public BarColor color = BarColor.PURPLE;
        @Comment(
                value = @CommentValue("SOLID / SEGMENTED_6 / SEGMENTED_10 / SEGMENTED_12 / SEGMENTED_20"),
                at = Comment.At.SAME_LINE
        )
        public BarStyle style = BarStyle.SEGMENTED_12;
    }

    @NewLine
    @Comment({
            @CommentValue("Title during authentication waiting")
    })
    public TitleConfig title;

    public static class TitleConfig {
        @Comment({
                @CommentValue("Title before login (countdown timer)")
        })
        public TitleSubSection beforeLogin = new TitleSubSection();
        @NewLine
        @Comment({
                @CommentValue("Title before registration")
        })
        public TitleSubSection beforeRegister = new TitleSubSection();
        @NewLine
        @Comment({
                @CommentValue("Title after login (when connecting to target server)")
        })
        public TitleSubSection afterLogin = new TitleSubSection();
        @NewLine
        @Comment({
                @CommentValue("Title after registration")
        })
        public TitleSubSection afterRegister = new TitleSubSection();

        @NewLine
        public static class TitleSubSection {
            public boolean enabled = false;
        }
    }

    public ActionBar actionBar;

    @NewLine
    public static class ActionBar {
        public boolean enabled = false;
    }

    @NewLine
    @Comment({
            @CommentValue("Nickname regex pattern")
    })
    public String nickPattern = "^[a-zA-Z0-9_]{3,16}$";
    @Comment({
            @CommentValue("Maximum number of accounts playing simultaneously from one IP")
    })
    public int maxOnlineAccountsPerIp = 10;
    @Comment({
            @CommentValue("Maximum number of registered accounts from one IP")
        })
    public int maxRegisteredAccountsPerIp = 10;

    public List<String> excludedIps = List.of("127.0.0.1");

    @NewLine
    @Comment({
            @CommentValue("Check for updates on GitHub")
    })
    public boolean checkUpdates = true;

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
