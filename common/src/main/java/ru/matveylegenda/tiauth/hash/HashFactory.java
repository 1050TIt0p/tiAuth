package ru.matveylegenda.tiauth.hash;

import ru.matveylegenda.tiauth.hash.impl.BCryptHash;
import ru.matveylegenda.tiauth.hash.impl.Sha256Hash;

public class HashFactory {
    public static Hash create(HashType hashType) {
        return switch (hashType) {
            case BCRYPT -> new BCryptHash();
            case SHA256 -> new Sha256Hash();
        };
    }
}
