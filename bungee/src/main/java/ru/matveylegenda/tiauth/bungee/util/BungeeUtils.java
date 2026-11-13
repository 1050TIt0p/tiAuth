package ru.matveylegenda.tiauth.bungee.util;

import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class BungeeUtils {

    private final Pattern HEX_COLOR = Pattern.compile("§x((?:§[0-9a-fA-F]){6})");
    private final char[] LEGACY_CODES = "0123456789abcdef".toCharArray();
    private final int[] LEGACY_RGB = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    public void sendMessage(CommandSender sender, String message) {
        if (message.isBlank()) {
            return;
        }
        if (!(sender instanceof ProxiedPlayer)) {
            message = downsampleHexColors(message);
        }
        sender.sendMessage(TextComponent.fromLegacy(message));
    }

    public String getIp(ProxiedPlayer player) {
        return ((InetSocketAddress) player.getSocketAddress()).getAddress().getHostAddress();
    }

    private String downsampleHexColors(String message) {
        Matcher matcher = HEX_COLOR.matcher(message);
        StringBuilder builder = new StringBuilder(message.length());
        while (matcher.find()) {
            int rgb = Integer.parseInt(matcher.group(1).replace("§", ""), 16);
            matcher.appendReplacement(builder, "§" + nearestLegacyCode(rgb));
        }
        return matcher.appendTail(builder).toString();
    }

    private char nearestLegacyCode(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        char nearest = 'f';
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < LEGACY_RGB.length; i++) {
            int dr = red - ((LEGACY_RGB[i] >> 16) & 0xFF);
            int dg = green - ((LEGACY_RGB[i] >> 8) & 0xFF);
            int db = blue - (LEGACY_RGB[i] & 0xFF);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = LEGACY_CODES[i];
            }
        }
        return nearest;
    }
}
