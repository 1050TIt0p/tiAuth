package ru.matveylegenda.tiauth.velocity.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.util.regex.Pattern;

@UtilityClass
public class VelocityUtils {

    public final Pattern MIN = Pattern.compile("\\{min}", Pattern.LITERAL);
    public final Pattern MAX = Pattern.compile("\\{max}", Pattern.LITERAL);
    public final Pattern PLAYER = Pattern.compile("\\{player}", Pattern.LITERAL);
    public final Pattern NAME = Pattern.compile("\\{name}", Pattern.LITERAL);
    public final Pattern REAL_NAME = Pattern.compile("\\{realname}", Pattern.LITERAL);
    public final Pattern TIME = Pattern.compile("\\{time}", Pattern.LITERAL);
    public final Pattern ATTEMPTS = Pattern.compile("\\{attempts}", Pattern.LITERAL);

    public void sendMessage(Audience sender, Component message) {
        if (message.equals(Component.empty())) {
            return;
        }
        sender.sendMessage(message);
    }
}
