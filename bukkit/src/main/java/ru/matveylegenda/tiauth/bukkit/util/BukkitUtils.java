package ru.matveylegenda.tiauth.bukkit.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.matveylegenda.tiauth.util.Utils;

@UtilityClass
public class BukkitUtils {
    private final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        sender.sendMessage(colorize(message));
    }

    public void sendActionBar(Player player, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        
        String colored = colorize(message);
        
        // Try Spigot API (1.11+)
        try {
            java.lang.reflect.Method method = Player.class.getMethod("sendActionBar", String.class);
            method.invoke(player, colored);
            return;
        } catch (Exception ignored) {
        }
        
        // Fallback to BungeeCord spigot API (works on most versions)
        try {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(colored));
        } catch (Exception ignored) {
        }
    }

    public String colorize(String message) {
        return Utils.COLORIZER.colorize(message);
    }

    public Component component(String message) {
        return LEGACY_SERIALIZER.deserialize(colorize(message));
    }
}
