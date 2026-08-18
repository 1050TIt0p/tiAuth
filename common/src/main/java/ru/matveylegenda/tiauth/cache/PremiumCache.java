package ru.matveylegenda.tiauth.cache;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class PremiumCache {
    private final Set<String> premiumPlayers = ConcurrentHashMap.newKeySet();

    public boolean isPremium(String name) {
        return premiumPlayers.contains(name.toLowerCase(Locale.ROOT));
    }

    public void addPremium(String name) {
        premiumPlayers.add(name.toLowerCase(Locale.ROOT));
    }

    public void removePremium(String name) {
        premiumPlayers.remove(name.toLowerCase(Locale.ROOT));
    }

    public void clear() {
        premiumPlayers.clear();
    }
}
