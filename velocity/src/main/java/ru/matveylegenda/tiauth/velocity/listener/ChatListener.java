package ru.matveylegenda.tiauth.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.config.MainConfig;

import java.util.Locale;

public class ChatListener {

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }

        if (AuthCache.isAuthenticated(player.getUsername())) {
            return;
        }

        // Уровень содержания костылей в крови сука зашкаливает!!!
        String command = '/' + cutCommand(event.getCommand()).toLowerCase(Locale.ROOT);

        if (!MainConfig.IMP.auth.allowedCommands.contains(command)) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
        }
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!AuthCache.isAuthenticated(player.getUsername())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
        }
    }

    private String cutCommand(String str) {
        int index = str.indexOf(' ');
        return index == -1 ? str : str.substring(0, index);
    }
}
