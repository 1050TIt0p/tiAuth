package ru.matveylegenda.tiauth.util.colorizer.impl;

import ru.matveylegenda.tiauth.util.Utils;
import ru.matveylegenda.tiauth.util.colorizer.Colorizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LegacyColorizer implements Colorizer {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");

    @Override
    public String colorize(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder(message.length() + 32);
        while (matcher.find()) {
            char[] group = matcher.group(1).toCharArray();
            matcher.appendReplacement(builder,
                    Utils.COLOR_CHAR + "x" +
                            Utils.COLOR_CHAR + group[0] +
                            Utils.COLOR_CHAR + group[1] +
                            Utils.COLOR_CHAR + group[2] +
                            Utils.COLOR_CHAR + group[3] +
                            Utils.COLOR_CHAR + group[4] +
                            Utils.COLOR_CHAR + group[5]);
        }

        message = matcher.appendTail(builder).toString();
        return Utils.translateAlternateColorCodes('&', message);
    }
}
