package ru.matveylegenda.tiauth.velocity.dialog;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.List;

public interface VelocityDialogService extends AutoCloseable {

    boolean isAvailable();

    void show(
            Player player,
            Component title,
            List<TextInput> inputs,
            Component confirmButton,
            String actionId,
            Component notice
    );

    void clear(Player player);

    @Override
    default void close() {
    }

    record TextInput(String key, Component label) {
    }
}
