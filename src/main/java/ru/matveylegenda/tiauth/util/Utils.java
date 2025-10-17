package ru.matveylegenda.tiauth.util;

import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.CommandSender;
import ru.matveylegenda.tiauth.util.colorizer.Colorizer;
import ru.matveylegenda.tiauth.util.colorizer.Serializer;
import ru.matveylegenda.tiauth.util.colorizer.impl.LegacyColorizer;
import ru.matveylegenda.tiauth.util.colorizer.impl.MiniMessageColorizer;

@UtilityClass
public class Utils {
    public static Colorizer COLORIZER;

    public static void initializeColorizer(Serializer serializer) {
        COLORIZER = switch (serializer) {
            case LEGACY -> new LegacyColorizer();
            case MINIMESSAGE -> new MiniMessageColorizer();
        };
    }

    public void sendMessage(CommandSender sender, String message) {
        if (message.isBlank()) {
            return;
        }
        sender.sendMessage(message);
    }
}
