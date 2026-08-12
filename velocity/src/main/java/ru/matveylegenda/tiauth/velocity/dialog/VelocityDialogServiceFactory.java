package ru.matveylegenda.tiauth.velocity.dialog;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;

import java.util.List;

public final class VelocityDialogServiceFactory {
    private VelocityDialogServiceFactory() {
    }

    public static VelocityDialogService create(TiAuth plugin, AuthManager authManager) {
        if (plugin.getServer().getPluginManager().getPlugin("packetevents").isEmpty()) {
            return NoopDialogService.INSTANCE;
        }

        try {
            Class<?> implementation = Class.forName(
                    "ru.matveylegenda.tiauth.velocity.dialog.PacketEventsDialogService",
                    true,
                    VelocityDialogServiceFactory.class.getClassLoader()
            );
            return (VelocityDialogService) implementation
                    .getConstructor(TiAuth.class, AuthManager.class)
                    .newInstance(plugin, authManager);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warn("PacketEvents dialog integration could not be initialized; dialogs are disabled", exception);
            return NoopDialogService.INSTANCE;
        }
    }

    private enum NoopDialogService implements VelocityDialogService {
        INSTANCE;

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void show(
                Player player,
                Component title,
                List<TextInput> inputs,
                Component confirmButton,
                String actionId,
                Component notice
        ) {
        }

        @Override
        public void clear(Player player) {
        }
    }
}
