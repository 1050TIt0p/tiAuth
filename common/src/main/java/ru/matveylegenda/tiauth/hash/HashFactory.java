package ru.matveylegenda.tiauth.hash;

import ru.matveylegenda.tiauth.hash.impl.Argon2Hash;
import ru.matveylegenda.tiauth.hash.impl.BCryptHash;
import ru.matveylegenda.tiauth.hash.impl.Sha256AuthMeHash;
import ru.matveylegenda.tiauth.hash.impl.Sha256Hash;

public class HashFactory {
    public static Hash create(HashType hashType) {
        return switch (hashType) {
            case BCRYPT -> new BCryptHash();
            case SHA256 -> new Sha256AuthMeHash();
            case ARGON2 -> new Argon2Hash();
            case SHA256_DEFAULT -> new Sha256Hash();
        };
    }
}
