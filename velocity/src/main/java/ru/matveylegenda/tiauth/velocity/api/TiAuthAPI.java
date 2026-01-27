package ru.matveylegenda.tiauth.velocity.api;

import lombok.Getter;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;

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
