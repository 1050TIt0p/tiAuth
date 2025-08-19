package ru.matveylegenda.tiauth.config;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.List;

public class MainConfig extends YamlSerializable {

    public Servers servers = new Servers();
    public static class Servers {
        @Comment({
                @CommentValue(" Сервер авторизации на который будет перемещать игроков для регистрации/авторизации")
        })
        public String auth = "auth";

        @Comment({
                @CommentValue(" Бэкенд сервер на который будет перемещать игроков после регистрации/авторизации")
        })
        public String backend = "hub";
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
                @CommentValue(" Алгоритм хеширования пароля"),
                @CommentValue(" Доступные варианты:"),
                @CommentValue(" bcrypt"),
                @CommentValue(" sha256")
        })
        public String hashAlgorithm = "bcrypt";

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
    }
}
