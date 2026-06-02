package ru.matveylegenda.tiauth.hash.impl;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.hash.Hash;

public class Argon2Hash implements Hash {
    private static final Argon2 ARGON2 = Argon2Factory.create();

    @Override
    public String hashPassword(String password) {
        char[] chars = password.toCharArray();

        try {
            return ARGON2.hash(
                    MainConfig.IMP.auth.argon2Iterations,
                    MainConfig.IMP.auth.argon2Memory,
                    MainConfig.IMP.auth.argon2Parallelism,
                    password.toCharArray()
            );
        } finally {
            ARGON2.wipeArray(chars);
        }
    }

    @Override
    public boolean verifyPassword(String password, String hashedPassword) {
        char[] chars = password.toCharArray();

        try {
            return ARGON2.verify(
                    hashedPassword,
                    chars
            );
        } finally {
            ARGON2.wipeArray(chars);
        }
    }
}
