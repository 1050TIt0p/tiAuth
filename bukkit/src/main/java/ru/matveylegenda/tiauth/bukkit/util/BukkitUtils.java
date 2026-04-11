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
        if (message == null) {
            return;
        }
        player.sendActionBar(colorize(message));
    }

    public String colorize(String message) {
        return Utils.COLORIZER.colorize(message);
    }

    public Component component(String message) {
        return LEGACY_SERIALIZER.deserialize(colorize(message));
    }
}
