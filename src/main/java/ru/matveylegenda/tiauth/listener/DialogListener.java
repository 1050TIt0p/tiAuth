package ru.matveylegenda.tiauth.listener;

import com.google.gson.JsonObject;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.CustomClickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.manager.AuthManager;

public class DialogListener implements Listener {
    private final AuthManager authManager;

    public DialogListener(TiAuth plugin) {
        this.authManager = plugin.getAuthManager();
    }

    @EventHandler
    public void onCustomClick(CustomClickEvent event) {
        ProxiedPlayer player = event.getPlayer();
        JsonObject jsonObject = event.getData().getAsJsonObject();

        if (event.getId().equals("minecraft:tiauth_login")) {
            String password = jsonObject.get("password").getAsString().trim();
            authManager.loginPlayer(player, password);
        }

        if (event.getId().equals("minecraft:tiauth_register")) {
            String password = jsonObject.get("password").getAsString().trim();
            String repeatPassword = jsonObject.get("repeatPassword").getAsString().trim();
            authManager.registerPlayer(player, password, repeatPassword);
        }
    }
}
