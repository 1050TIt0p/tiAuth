package ru.matveylegenda.tiauth.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

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

    public void addPlayer(String ip) {
        bans.put(ip, System.currentTimeMillis());
    }

    public boolean isBanned(String ip) {
        return bans.asMap().containsKey(ip);
    }

    public int getRemainingSeconds(String ip) {
        Long startTime = bans.getIfPresent(ip);
        long currentTime = System.currentTimeMillis();

        if (startTime == null) return 0;

        return (int) (banTime - TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime));
    }
}
