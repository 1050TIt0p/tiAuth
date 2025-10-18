package ru.matveylegenda.tiauth.cache;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class PremiumCache {
    private final Set<String> premiumUsers = ConcurrentHashMap.newKeySet();

    public boolean isPremium(String name) {
        return premiumUsers.contains(name.toLowerCase(Locale.ROOT));
    }

    public void addPremium(String name) {
        premiumUsers.add(name.toLowerCase(Locale.ROOT));
    }

    public void removePremium(String name) {
        premiumUsers.remove(name.toLowerCase(Locale.ROOT));
    }
}
