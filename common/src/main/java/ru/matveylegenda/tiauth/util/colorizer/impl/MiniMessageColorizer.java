package ru.matveylegenda.tiauth.util.colorizer.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.matveylegenda.tiauth.util.Utils;
import ru.matveylegenda.tiauth.util.colorizer.Colorizer;

public class MiniMessageColorizer implements Colorizer {

    @Override
    public String colorize(String message) {
        Component component = MiniMessage.miniMessage().deserialize(message);
        return LegacyComponentSerializer.builder()
                .character(Utils.COLOR_CHAR)
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build()
                .serialize(component);
    }
}
