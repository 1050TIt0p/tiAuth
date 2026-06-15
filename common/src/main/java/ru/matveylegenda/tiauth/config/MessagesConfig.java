package ru.matveylegenda.tiauth.config;

import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.annotations.Transient;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Objects;

public class MessagesConfig extends YamlSerializable {

    @Transient
    private static final SerializerConfig CONFIG = new SerializerConfig.Builder()
            .setCommentValueIndent(1)
            .build();

    @Transient
    public static MessagesConfig IMP = new MessagesConfig(getMessagesPath(MainConfig.IMP.lang));

    public MessagesConfig(Path path) {
        super(path, CONFIG);
        this.admin = new Admin();
        this.player = new Player();
        if (!path.toFile().exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("lang/" + path.getFileName()))))) {
                load(reader);
                save();
            } catch (Exception ignored) {
            }
        }
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
        public ForcePremium forcePremium = new ForcePremium();
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
        public static class ForcePremium {
            public String usage;
            public String enabled;
            public String disabled;
        }

        @NewLine
        public static class Migrate {
            public String usage;
            public String error;
            public String invalidFileName;
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
        public Totp totp = new Totp();
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
        public static class Totp {
            public String usage;
            public String enableUsage;
            public String verifyUsage;
            public String disableUsage;
            public String successful;
            public String verified;
            public String disabled;
            public String wrong;
            public String alreadyEnabled;
            public String alreadyDisabled;
            public String qr;
            public String token;
            public String recovery;
            public String needPassword;
            public String prompt;
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
            public String authServerNotFound;
            public String forcedHostNotFound;
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
            public TitleMessage beforeLogin = new TitleMessage();
            public TitleMessage beforeRegister = new TitleMessage();
            public TitleMessageDelayed afterLogin = new TitleMessageDelayed();
            public TitleMessageDelayed afterRegister = new TitleMessageDelayed();

            @NewLine
            public static class TitleMessage {
                public String title;
                public String subtitle;
            }

            @NewLine
            public static class TitleMessageDelayed {
                public String title;
                public String subtitle;
                public Delays delays = new Delays();

                @NewLine
                public static class Delays {
                    public int start = 0;
                    public int duration = 60;
                    public int end = 6;
                }
            }
        }

        public static class ActionBar {
            public String message;
        }
    }

    public static Path getMessagesPath(String lang) {
        return Path.of("plugins", "tiAuth", "lang", "messages_" + lang.toLowerCase() + ".yml");
    }
}