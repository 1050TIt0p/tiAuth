package ru.matveylegenda.tiauth.cache;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuthCache {
    private final Set<String> authenticated = ConcurrentHashMap.newKeySet();

    public void setAuthenticated(String name) {
        authenticated.add(name.toLowerCase(Locale.ROOT));
    }

    public void logout(String name) {
        authenticated.remove(name.toLowerCase(Locale.ROOT));
    }

    public boolean isAuthenticated(String name) {
        return authenticated.contains(name.toLowerCase(Locale.ROOT));
    }
}
