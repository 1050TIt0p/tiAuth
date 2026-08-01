package ru.matveylegenda.tiauth.cache;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class AuthCache {
    private final Set<String> authenticated = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingVerifications = ConcurrentHashMap.newKeySet();
    private final Set<String> totpPending = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, Integer> totpAttempts = new ConcurrentHashMap<>();
    private final Map<String, String> totpEnableSecrets = new ConcurrentHashMap<>();

    public void setAuthenticated(String name) {
        authenticated.add(name.toLowerCase(Locale.ROOT));
    }

    public void logout(String name) {
        authenticated.remove(name.toLowerCase(Locale.ROOT));
    }

    public boolean isAuthenticated(String name) {
        return authenticated.contains(name.toLowerCase(Locale.ROOT));
    }

    public boolean isPendingVerification(String playerName) {
        return pendingVerifications.contains(playerName.toLowerCase(Locale.ROOT));
    }

    public void setPendingVerification(String playerName) {
        pendingVerifications.add(playerName.toLowerCase(Locale.ROOT));
    }

    public void clearPendingVerification(String playerName) {
        pendingVerifications.remove(playerName.toLowerCase(Locale.ROOT));
    }

    public boolean isTotpPending(String playerName) {
        return totpPending.contains(playerName.toLowerCase(Locale.ROOT));
    }

    public void setTotpPending(String playerName) {
        totpPending.add(playerName.toLowerCase(Locale.ROOT));
    }

    public void clearTotpPending(String playerName) {
        totpPending.remove(playerName.toLowerCase(Locale.ROOT));
    }

    public int incrementLoginAttempts(String lowerName) {
        return loginAttempts.merge(lowerName, 1, Integer::sum);
    }

    public void resetLoginAttempts(String lowerName) {
        loginAttempts.remove(lowerName);
    }

    public int incrementTotpAttempts(String lowerName) {
        return totpAttempts.merge(lowerName, 1, Integer::sum);
    }

    public void resetTotpAttempts(String lowerName) {
        totpAttempts.remove(lowerName);
    }

    public void setTotpEnableSecret(String playerName, String secret) {
        totpEnableSecrets.put(playerName.toLowerCase(Locale.ROOT), secret);
    }

    public String getTotpEnableSecret(String playerName) {
        return totpEnableSecrets.get(playerName.toLowerCase(Locale.ROOT));
    }

    public void removeTotpEnableSecret(String playerName) {
        totpEnableSecrets.remove(playerName.toLowerCase(Locale.ROOT));
    }
}
