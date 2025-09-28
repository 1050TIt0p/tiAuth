package ru.matveylegenda.tiauth.util;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PreLoginEvent;
import ru.matveylegenda.tiauth.util.colorizer.ColorizedMessages;
import ru.matveylegenda.tiauth.util.colorizer.Colorizer;
import ru.matveylegenda.tiauth.util.colorizer.Serializer;
import ru.matveylegenda.tiauth.util.colorizer.impl.LegacyColorizer;
import ru.matveylegenda.tiauth.util.colorizer.impl.MiniMessageColorizer;

public class Utils {
    private final ColorizedMessages colorizedMessages;
    public static Colorizer COLORIZER;

    public Utils(ColorizedMessages colorizedMessages) {
        this.colorizedMessages = colorizedMessages;
    }

    public static void initializeColorizer(Serializer serializer) {
        COLORIZER = switch (serializer) {
            case LEGACY -> new LegacyColorizer();
            case MINIMESSAGE -> new MiniMessageColorizer();
        };
    }

    public void sendMessage(CommandSender sender, String message) {
        if (message.isEmpty()) {
            return;
        }

        sender.sendMessage(message.replace("{prefix}", colorizedMessages.prefix));
    }

    public void kickPlayer(ProxiedPlayer player, String message) {
        player.disconnect(message.replace("{prefix}", colorizedMessages.prefix));
    }

    public void kickPlayer(PreLoginEvent event, String message) {
        event.setCancelReason(message.replace("{prefix}", colorizedMessages.prefix));
        event.setCancelled(true);
    }
}
