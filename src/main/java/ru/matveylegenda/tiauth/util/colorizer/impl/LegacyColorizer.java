package ru.matveylegenda.tiauth.util.colorizer.impl;

import net.md_5.bungee.api.ChatColor;
import ru.matveylegenda.tiauth.util.colorizer.Colorizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class LegacyColorizer implements Colorizer {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");

    @Override
    public String colorize(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder(message.length() + 32);
        while (matcher.find()) {
            char[] group = matcher.group(1).toCharArray();
            matcher.appendReplacement(builder,
                    COLOR_CHAR + "x" +
                            COLOR_CHAR + group[0] +
                            COLOR_CHAR + group[1] +
                            COLOR_CHAR + group[2] +
                            COLOR_CHAR + group[3] +
                            COLOR_CHAR + group[4] +
                            COLOR_CHAR + group[5]);
        }

        message = matcher.appendTail(builder).toString();
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
