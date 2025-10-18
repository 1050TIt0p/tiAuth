package ru.matveylegenda.tiauth.velocity.util;

import com.velocitypowered.api.command.CommandSource;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;

@UtilityClass
public class VelocityUtils {

    public void sendMessage(CommandSource sender, Component message) {
        if (message.equals(Component.empty())) {
            return;
        }
        sender.sendMessage(message);
    }
}
