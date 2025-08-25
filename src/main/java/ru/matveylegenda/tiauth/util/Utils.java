package ru.matveylegenda.tiauth.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.matveylegenda.tiauth.config.MessagesConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class Utils {
    private final MessagesConfig messagesConfig;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");

    public Utils(MessagesConfig messagesConfig) {
        this.messagesConfig = messagesConfig;
    }

    public static String colorize(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(builder,
                    COLOR_CHAR + "x" +
                            COLOR_CHAR + group.charAt(0) +
                            COLOR_CHAR + group.charAt(1) +
                            COLOR_CHAR + group.charAt(2) +
                            COLOR_CHAR + group.charAt(3) +
                            COLOR_CHAR + group.charAt(4) +
                            COLOR_CHAR + group.charAt(5));
        }
        message = matcher.appendTail(builder).toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void sendMessage(CommandSender sender, String message) {
        if (message.isEmpty()) {
            return;
        }

        sender.sendMessage(
                colorize(message.replace("{prefix}", messagesConfig.prefix))
        );
    }

    public void kickPlayer(ProxiedPlayer player, String message) {
        player.disconnect(
                colorize(message.replace("{prefix}", messagesConfig.prefix))
        );
    }
}
