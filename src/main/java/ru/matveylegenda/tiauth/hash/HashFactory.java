package ru.matveylegenda.tiauth.hash;

import ru.matveylegenda.tiauth.hash.impl.BCryptHash;
import ru.matveylegenda.tiauth.hash.impl.Sha256Hash;

public class HashFactory {
    public static Hash create(String type) {
        return switch (type.toLowerCase()) {
            case "bcrypt" -> new BCryptHash();
            case "sha256" -> new Sha256Hash();
            default -> throw new IllegalArgumentException("Unknown hash type: " + type);
        };
    }
}
