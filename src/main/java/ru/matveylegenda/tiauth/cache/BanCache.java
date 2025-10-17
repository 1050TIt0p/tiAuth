package ru.matveylegenda.tiauth.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.experimental.UtilityClass;
import ru.matveylegenda.tiauth.config.MainConfig;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class BanCache {
    private final Cache<String, Long> bans = Caffeine.newBuilder()
            .expireAfterWrite(MainConfig.IMP.auth.banTime, TimeUnit.SECONDS)
            .build();
    ;

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

        return (int) (MainConfig.IMP.auth.banTime - TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime));
    }
}
