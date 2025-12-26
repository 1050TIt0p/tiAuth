package ru.matveylegenda.tiauth.bungee.api;

import lombok.Getter;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.manager.AuthManager;
import ru.matveylegenda.tiauth.database.Database;

public class TiAuthAPI {
    @Getter
    private static TiAuthAPI instance;
    private final TiAuth plugin;

    public TiAuthAPI(TiAuth plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public Database getDatabase() {
        return plugin.getDatabase();
    }

    public AuthManager getAuthManager() {
        return plugin.getAuthManager();
    }
}
