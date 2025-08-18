package ru.matveylegenda.tiauth.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ru.matveylegenda.tiauth.config.MainConfig;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SessionCache {
    private final Cache<String, String> sessions;

    public SessionCache(MainConfig mainConfig) {
        this.sessions = Caffeine.newBuilder()
                .expireAfterWrite(mainConfig.auth.sessionLifetimeMinutes, TimeUnit.MINUTES)
                .build();
    }

    public void addPlayer(String name, String ip) {
        sessions.put(name.toLowerCase(Locale.ROOT), ip);
    }

    public String getIP(String name) {
        return sessions.getIfPresent(name.toLowerCase(Locale.ROOT));
    }
}
