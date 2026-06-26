package ru.matveylegenda.tiauth.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.experimental.UtilityClass;
import ru.matveylegenda.tiauth.config.MainConfig;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class BanCache {
    private static final BanInstance AUTH = new BanInstance(MainConfig.IMP.auth.banTime);
    private static final BanInstance TOTP = new BanInstance(MainConfig.IMP.auth.totp.banTime);

    public void addPlayer(String ip) {
        AUTH.add(ip);
    }

    public boolean isBanned(String ip) {
        return AUTH.isBanned(ip);
    }

    public int getRemainingSeconds(String ip) {
        return AUTH.getRemainingSeconds(ip);
    }

    public void addTotpBan(String ip) {
        TOTP.add(ip);
    }

    public boolean isTotpBanned(String ip) {
        return TOTP.isBanned(ip);
    }

    public int getTotpRemainingSeconds(String ip) {
        return TOTP.getRemainingSeconds(ip);
    }

    private static class BanInstance {
        private final Cache<String, Long> bans;
        private final int banTime;

        BanInstance(int banTime) {
            this.banTime = banTime;
            this.bans = Caffeine.newBuilder()
                    .expireAfterWrite(banTime, TimeUnit.SECONDS)
                    .build();
        }

        void add(String ip) {
            bans.put(ip, System.currentTimeMillis());
        }

        boolean isBanned(String ip) {
            return bans.asMap().containsKey(ip);
        }

        int getRemainingSeconds(String ip) {
            Long startTime = bans.getIfPresent(ip);
            long currentTime = System.currentTimeMillis();

            if (startTime == null) return 0;

            return (int) (banTime - TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime));
        }
    }
}
