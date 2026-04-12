package ru.matveylegenda.tiauth.bukkit.api;

import lombok.Getter;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.bukkit.TiAuth;
import ru.matveylegenda.tiauth.bukkit.manager.AuthManager;

@Getter
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
