package ru.matveylegenda.tiauth.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BanCache {
    private int banTime;
    private final Cache<String, Long> bans;

    public BanCache(int banTime) {
        this.banTime = banTime;
        this.bans = Caffeine.newBuilder()
                .expireAfterWrite(banTime, TimeUnit.SECONDS)
                .build();
    }

    public void addPlayer(String name) {
        bans.put(name.toLowerCase(Locale.ROOT), System.currentTimeMillis());
    }

    public boolean isBanned(String name) {
        return bans.asMap().containsKey(name.toLowerCase(Locale.ROOT));
    }

    public int getRemainingSeconds(String name) {
        Long startTime = bans.getIfPresent(name.toLowerCase(Locale.ROOT));
        long currentTime = System.currentTimeMillis();

        if (startTime == null) return 0;

        return (int) (banTime - TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime));
    }
}
